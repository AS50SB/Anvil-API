package eab.anvilapi.mixin;

import eab.anvilapi.AnvilApiMod;
import eab.anvilapi.recipe.AnvilRepairRecipe;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin extends ForgingScreenHandler {

    @Shadow
    private Property levelCost;

    protected AnvilScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(type, syncId, playerInventory, context);
    }

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void onUpdateResult(CallbackInfo ci) {
        ItemStack leftStack = this.input.getStack(0);
        ItemStack rightStack = this.input.getStack(1);

        if (leftStack.isEmpty()) {
            this.output.setStack(0, ItemStack.EMPTY);
            this.levelCost.set(0);
            ci.cancel();
            return;
        }
        
        if (rightStack.isEmpty()) {
            this.output.setStack(0, ItemStack.EMPTY);
            this.levelCost.set(0);
            ci.cancel();
            return;
        }

        var optionalRecipe = AnvilApiMod.getRecipeLoader()
            .getRecipeFor(leftStack.getItem(), rightStack.getItem());
            
        if (optionalRecipe.isPresent()) {
            AnvilRepairRecipe recipe = optionalRecipe.get();
            
            if (rightStack.getCount() < recipe.getMaterialCost()) {
                this.output.setStack(0, ItemStack.EMPTY);
                this.levelCost.set(0);
            } else if (leftStack.isDamaged()) {
                int repairAmount = recipe.calculateRepairAmount(leftStack);
                ItemStack result = leftStack.copy();
                int newDamage = Math.max(0, result.getDamage() - repairAmount);
                result.setDamage(newDamage);
                this.output.setStack(0, result);
                this.levelCost.set(recipe.getExperienceCost());
            } else {
                this.output.setStack(0, ItemStack.EMPTY);
                this.levelCost.set(0);
            }
            ci.cancel();
        }
    }

    @Inject(method = "getLevelCost", at = @At("RETURN"), cancellable = true)
    private void onGetLevelCost(CallbackInfoReturnable<Integer> cir) {
        ItemStack left = this.input.getStack(0);
        ItemStack right = this.input.getStack(1);
        
        if (!left.isEmpty() && !right.isEmpty()) {
            var optionalRecipe = AnvilApiMod.getRecipeLoader()
                .getRecipeFor(left.getItem(), right.getItem());
                
            if (optionalRecipe.isPresent()) {
                AnvilRepairRecipe recipe = optionalRecipe.get();
                if (right.getCount() >= recipe.getMaterialCost() && left.isDamaged()) {
                    cir.setReturnValue(recipe.getExperienceCost());
                }
            }
        }
    }

    @Inject(method = "canTakeOutput", at = @At("RETURN"), cancellable = true)
    private void onCanTakeOutput(PlayerEntity player, boolean something, CallbackInfoReturnable<Boolean> cir) {
        ItemStack left = this.input.getStack(0);
        ItemStack right = this.input.getStack(1);
        
        if (!left.isEmpty() && !right.isEmpty()) {
            var optionalRecipe = AnvilApiMod.getRecipeLoader()
                .getRecipeFor(left.getItem(), right.getItem());
                
            if (optionalRecipe.isPresent()) {
                AnvilRepairRecipe recipe = optionalRecipe.get();
                if (player.isCreative()) {
                    cir.setReturnValue(true);
                } else {
                    cir.setReturnValue(player.experienceLevel >= recipe.getExperienceCost());
                }
            }
        }
    }

    @Override
    public void onTakeOutput(PlayerEntity player, ItemStack stack) {
        ItemStack left = this.input.getStack(0);
        ItemStack right = this.input.getStack(1);

        var optionalRecipe = AnvilApiMod.getRecipeLoader()
            .getRecipeFor(left.getItem(), right.getItem());

        if (optionalRecipe.isPresent()) {
            AnvilRepairRecipe recipe = optionalRecipe.get();
            int materialCost = recipe.getMaterialCost();
            int expCost = recipe.getExperienceCost();

            if (right.getCount() < materialCost) {
                return;
            }
            
            if (!player.isCreative() && player.experienceLevel < expCost) {
                return;
            }

            // 清空左边物品
            this.input.setStack(0, ItemStack.EMPTY);
            
            // 消耗右边材料
            if (right.getCount() <= materialCost) {
                this.input.setStack(1, ItemStack.EMPTY);
            } else {
                right.decrement(materialCost);
                this.input.setStack(1, right);
            }

            // 扣除经验
            if (!player.isCreative()) {
                player.addExperienceLevels(-expCost);
            }

            // 播放音效
            this.context.run((world, pos) -> {
                world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 1.0f, world.random.nextFloat() * 0.1f + 0.9f);
            });
        }
    }
}
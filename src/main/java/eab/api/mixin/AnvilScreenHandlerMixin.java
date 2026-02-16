package eab.api.mixin;

import eab.api.data.AnvilRepairRecipe;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin extends ForgingScreenHandler {
    
    // 存储当前配方的经验和材料消耗
    private AnvilRepairRecipe currentRecipe = null;
    private int currentMaterialCost = 0;
    private int currentExperienceCost = 0;
    
    public AnvilScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(type, syncId, playerInventory, context);
    }
    
    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void onUpdateResult(CallbackInfo ci) {
        ItemStack leftStack = this.input.getStack(0);
        ItemStack rightStack = this.input.getStack(1);
        
        // 重置当前配方
        currentRecipe = null;
        currentMaterialCost = 0;
        currentExperienceCost = 0;
        
        // 如果没有物品在左边，清空输出
        if (leftStack.isEmpty()) {
            this.output.setStack(0, ItemStack.EMPTY);
            return;
        }
        
        // 如果没有材料在右边，清空输出
        if (rightStack.isEmpty()) {
            this.output.setStack(0, ItemStack.EMPTY);
            return;
        }
        
        // 检查自定义配方
        AnvilRepairRecipe recipe = AnvilRepairRecipe.getRecipe(
            leftStack.getItem(), 
            rightStack.getItem()
        );
        
        if (recipe != null) {
            // 检查材料数量
            if (rightStack.getCount() < recipe.getMaterialCost()) {
                this.output.setStack(0, ItemStack.EMPTY);
                ci.cancel();
                return;
            }
            
            // 创建修复后的物品
            ItemStack resultStack = leftStack.copy();
            
            // 修复物品
            int repairAmount = recipe.getRepairAmount();
            if (repairAmount <= 0) {
                resultStack.setDamage(0); // 完全修复
            } else {
                int newDamage = Math.max(0, resultStack.getDamage() - repairAmount);
                resultStack.setDamage(newDamage);
            }
            
            // 设置输出
            this.output.setStack(0, resultStack);
            
            // 保存当前配方信息供后续使用
            currentRecipe = recipe;
            currentMaterialCost = recipe.getMaterialCost();
            currentExperienceCost = recipe.getExperienceCost();
            
            ci.cancel();
        }
    }
    
    @Inject(method = "getLevelCost", at = @At("RETURN"), cancellable = true)
    private void onGetLevelCost(CallbackInfoReturnable<Integer> cir) {
        if (currentRecipe != null) {
            cir.setReturnValue(currentExperienceCost);
        }
    }
    
    // 修复：正确的参数签名是 (PlayerEntity, boolean)
    @Inject(method = "canTakeOutput", at = @At("HEAD"), cancellable = true)
    private void onCanTakeOutput(PlayerEntity player, boolean something, CallbackInfoReturnable<Boolean> cir) {
        if (currentRecipe != null) {
            // 检查玩家是否有足够的经验（创造模式总是可以）
            if (player.isCreative()) {
                cir.setReturnValue(true);
                return;
            }
            
            // 检查经验等级
            if (player.experienceLevel >= currentExperienceCost) {
                cir.setReturnValue(true);
            } else {
                cir.setReturnValue(false);
            }
        }
    }
    
    @Inject(method = "onTakeOutput", at = @At("HEAD"), cancellable = true)
    private void onTakeOutput(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (currentRecipe != null) {
            ItemStack leftStack = this.input.getStack(0);
            ItemStack rightStack = this.input.getStack(1);
            
            // 再次检查材料数量
            if (rightStack.getCount() < currentMaterialCost) {
                ci.cancel();
                return;
            }
            
            // 再次检查经验等级（非创造模式）
            if (!player.isCreative() && player.experienceLevel < currentExperienceCost) {
                ci.cancel();
                return;
            }
            
            // 消耗左边的物品
            this.input.setStack(0, ItemStack.EMPTY);
            
            // 消耗右边的材料
            if (rightStack.getCount() <= currentMaterialCost) {
                this.input.setStack(1, ItemStack.EMPTY);
            } else {
                rightStack.decrement(currentMaterialCost);
                this.input.setStack(1, rightStack);
            }
            
            // 扣除经验（非创造模式）
            if (!player.isCreative()) {
                player.experienceLevel -= currentExperienceCost;
                // 更新经验条
                player.addExperienceLevels(0);
            }
            
            // 播放音效
            this.context.run((world, pos) -> {
                world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 1.0f, world.random.nextFloat() * 0.1f + 0.9f);
            });
            
            // 重置当前配方
            currentRecipe = null;
            currentMaterialCost = 0;
            currentExperienceCost = 0;
            
            ci.cancel();
        }
    }
}
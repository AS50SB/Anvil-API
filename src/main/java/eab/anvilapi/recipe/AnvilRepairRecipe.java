package eab.anvilapi.recipe;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class AnvilRepairRecipe {
    private final Identifier id;
    private final Item item;
    private final Item repairMaterial;
    private final String repairAmountRaw;
    private final int repairAmountFixed;
    private final boolean isPercentage;
    private final int materialCost;
    private final int experienceCost;
    private boolean enabled;
    
    public AnvilRepairRecipe(Identifier id, Item item, Item repairMaterial, 
                             String repairAmount, int materialCost, int experienceCost) {
        this.id = id;
        this.item = item;
        this.repairMaterial = repairMaterial;
        this.repairAmountRaw = repairAmount;
        this.isPercentage = repairAmount.endsWith("%");
        int fixedAmount = 0;
        if (isPercentage) {
            try {
                double percent = Double.parseDouble(repairAmount.substring(0, repairAmount.length() - 1));
                fixedAmount = (int) (100 * percent);
            } catch (NumberFormatException e) {
                fixedAmount = 0;
            }
        } else {
            try {
                fixedAmount = Integer.parseInt(repairAmount);
            } catch (NumberFormatException e) {
                fixedAmount = 0;
            }
        }
        this.repairAmountFixed = fixedAmount;
        this.materialCost = materialCost;
        this.experienceCost = experienceCost;
        this.enabled = true;
    }
    
    public Identifier getId() { return id; }
    public Item getItem() { return item; }
    public Item getRepairMaterial() { return repairMaterial; }
    public String getRepairAmountRaw() { return repairAmountRaw; }
    public int getRepairAmountFixed() { return repairAmountFixed; }
    public boolean isPercentage() { return isPercentage; }
    public int getMaterialCost() { return materialCost; }
    public int getExperienceCost() { return experienceCost; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public int calculateRepairAmount(ItemStack stack) {
        return RepairAmountCalculator.calculate(stack, repairAmountRaw);
    }
    
    public int getDisplayRepairAmount(ItemStack stack) {
        return RepairAmountCalculator.getDisplayAmount(stack, repairAmountRaw);
    }
    
    public boolean matches(Item item, Item material) {
        return enabled && this.item == item && this.repairMaterial == material;
    }
    
    public String getItemName() {
        return item.getName().getString();
    }
    
    public String getMaterialName() {
        return repairMaterial.getName().getString();
    }
    
    @Override
    public String toString() {
        return String.format("AnvilRepairRecipe{id=%s, item=%s, material=%s, repairAmount=%s, cost=%d, xp=%d, enabled=%b}",
            id, Registries.ITEM.getId(item), Registries.ITEM.getId(repairMaterial), 
            repairAmountRaw, materialCost, experienceCost, enabled);
    }
}
package eab.anvilapi.recipe;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class AnvilRepairRecipe {
    private final Identifier id;
    private final Item item;
    private final Item repairMaterial;
    private final int repairAmount;
    private final int materialCost;
    private final int experienceCost;
    private boolean enabled;
    
    public AnvilRepairRecipe(Identifier id, Item item, Item repairMaterial, 
                             int repairAmount, int materialCost, int experienceCost) {
        this.id = id;
        this.item = item;
        this.repairMaterial = repairMaterial;
        this.repairAmount = repairAmount;
        this.materialCost = materialCost;
        this.experienceCost = experienceCost;
        this.enabled = true;
    }
    
    public Identifier getId() { return id; }
    public Item getItem() { return item; }
    public Item getRepairMaterial() { return repairMaterial; }
    public int getRepairAmount() { return repairAmount; }
    public int getMaterialCost() { return materialCost; }
    public int getExperienceCost() { return experienceCost; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
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
        return String.format("AnvilRepairRecipe{id=%s, item=%s, material=%s, repairAmount=%d, cost=%d, xp=%d, enabled=%b}",
            id, Registries.ITEM.getId(item), Registries.ITEM.getId(repairMaterial), 
            repairAmount, materialCost, experienceCost, enabled);
    }
}
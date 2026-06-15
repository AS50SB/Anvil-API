package eab.anvilapi.recipe;

import net.minecraft.item.ItemStack;

public class RepairAmountCalculator {
    
    public static int calculate(ItemStack stack, String repairAmountStr) {
        int maxDamage = stack.getMaxDamage();
        int currentDamage = stack.getDamage();
        
        if (repairAmountStr.endsWith("%")) {
            try {
                double percent = Double.parseDouble(repairAmountStr.substring(0, repairAmountStr.length() - 1));
                int repairAmount = (int) (maxDamage * percent / 100.0);
                return Math.min(repairAmount, currentDamage);
            } catch (NumberFormatException e) {
                return 0;
            }
        } else {
            try {
                int repairAmount = Integer.parseInt(repairAmountStr);
                return Math.min(repairAmount, currentDamage);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }
    
    public static int getDisplayAmount(ItemStack stack, String repairAmountStr) {
        if (repairAmountStr.endsWith("%")) {
            try {
                double percent = Double.parseDouble(repairAmountStr.substring(0, repairAmountStr.length() - 1));
                return (int) (stack.getMaxDamage() * percent / 100.0);
            } catch (NumberFormatException e) {
                return 0;
            }
        } else {
            try {
                return Integer.parseInt(repairAmountStr);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }
}
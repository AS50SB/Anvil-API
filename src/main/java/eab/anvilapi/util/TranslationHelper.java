package eab.anvilapi.util;

import eab.anvilapi.AnvilApiMod;
import net.minecraft.client.resource.language.I18n;

public class TranslationHelper {
    
    public static String translate(String key, Object... args) {
        return String.format(I18n.translate(key), args);
    }
    
    public static String translateOrKey(String key, Object... args) {
        String translated = I18n.translate(key);
        if (translated.equals(key)) {
            return key;
        }
        return String.format(translated, args);
    }
}
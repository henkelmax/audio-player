package de.maxhenkel.audioplayer.lang;

import de.maxhenkel.audioplayer.AudioPlayerMod;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Lang {

    protected final Map<String, String> languageMap;

    protected Lang(Map<String, String> languageMap) {
        this.languageMap = languageMap;
    }

    protected static Lang load() throws IOException {
        Map<String, String> map = new HashMap<>();
        try (InputStream is = Lang.class.getResourceAsStream("/assets/audioplayer/lang/en_us.json")) {
            if (is == null) {
                throw new IOException("Failed to find language file");
            }
            Language.loadFromJson(is, map::put);
        }
        return new Lang(map);
    }

    public static MutableComponent translatable(String key, Object... args) {
        return Component.translatableWithFallback(key, instance.languageMap.getOrDefault(key, key), args);
    }

    private static Lang instance;

    public static void onInitialize() {
        try {
            instance = Lang.load();
        } catch (IOException e) {
            AudioPlayerMod.LOGGER.error("Failed to load language file");
            throw new RuntimeException(e);
        }
    }

}

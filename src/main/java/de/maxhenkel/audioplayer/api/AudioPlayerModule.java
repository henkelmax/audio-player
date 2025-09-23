package de.maxhenkel.audioplayer.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.maxhenkel.audioplayer.AudioPlayerMod;
import de.maxhenkel.audioplayer.api.data.AudioDataModule;
import de.maxhenkel.audioplayer.api.data.ModuleKey;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.UUID;

public class AudioPlayerModule implements AudioDataModule {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AudioPlayerMod.MODID, "audio");
    public static ModuleKey<AudioPlayerModule> KEY;

    protected UUID soundId;
    @Nullable
    protected Float range;

    private AudioPlayerModule() {

    }

    public AudioPlayerModule(UUID soundId, @Nullable Float range) {
        this.soundId = soundId;
        this.range = range;
    }

    public static void onInitialize() {
        if (KEY != null) {
            return;
        }
        KEY = AudioPlayerApi.instance().registerModuleType(AudioPlayerModule.ID, AudioPlayerModule::new);
    }

    public UUID getSoundId() {
        return soundId;
    }

    @Nullable
    public Float getRange() {
        return range;
    }

    @Override
    public void load(JsonObject json) throws Exception {
        JsonElement id = json.get("id");
        if (id == null) {
            throw new Exception("Missing sound ID");
        }
        soundId = UUID.fromString(id.getAsString());
        JsonElement rangeElement = json.get("range");
        range = rangeElement == null ? null : rangeElement.getAsFloat();
    }

    @Override
    public void save(JsonObject json) throws Exception {
        json.addProperty("id", soundId.toString());
        if (range != null) {
            json.addProperty("range", range);
        }
    }

}

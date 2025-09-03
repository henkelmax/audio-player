package de.maxhenkel.audioplayer.audioloader;

import de.maxhenkel.audioplayer.AudioPlayerMod;
import de.maxhenkel.audioplayer.api.AudioPlayerApi;
import de.maxhenkel.audioplayer.api.data.AudioDataModule;
import de.maxhenkel.audioplayer.api.data.DataAccessor;
import de.maxhenkel.audioplayer.api.data.DataModifier;
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
    public void load(DataAccessor accessor) throws Exception {
        String id = accessor.getString("id");
        if (id == null) {
            throw new Exception("Missing sound ID");
        }
        soundId = UUID.fromString(id);
        range = accessor.getFloat("range");
    }

    @Override
    public void save(DataModifier modifier) throws Exception {
        modifier.setString("id", soundId.toString());
        if (range != null) {
            modifier.setFloat("range", range);
        }
    }

}

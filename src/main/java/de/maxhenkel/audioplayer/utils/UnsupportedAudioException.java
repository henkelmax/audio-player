package de.maxhenkel.audioplayer.utils;

import de.maxhenkel.audioplayer.lang.Lang;
import net.minecraft.network.chat.Component;

public class UnsupportedAudioException extends ComponentException {

    public UnsupportedAudioException(Throwable cause) {
        super(Lang.translatable("audioplayer.unsupported_audio_format"), cause);
    }

    public UnsupportedAudioException(Component component) {
        super(component);
    }

    public UnsupportedAudioException() {
        this(Lang.translatable("audioplayer.unsupported_audio_format"));
    }

}

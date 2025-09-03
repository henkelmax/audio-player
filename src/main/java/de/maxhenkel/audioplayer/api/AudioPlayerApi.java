package de.maxhenkel.audioplayer.api;

import de.maxhenkel.audioplayer.apiimpl.AudioPlayerApiImpl;

public interface AudioPlayerApi {

    static AudioPlayerApi instance() {
        return AudioPlayerApiImpl.INSTANCE;
    }

}

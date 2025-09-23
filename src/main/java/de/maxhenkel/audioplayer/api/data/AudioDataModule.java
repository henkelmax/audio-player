package de.maxhenkel.audioplayer.api.data;

import com.google.gson.JsonObject;

public interface AudioDataModule {

    void load(JsonObject json) throws Exception;

    void save(JsonObject json) throws Exception;

}
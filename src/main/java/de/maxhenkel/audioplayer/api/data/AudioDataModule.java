package de.maxhenkel.audioplayer.api.data;

public interface AudioDataModule {

    void load(DataAccessor accessor) throws Exception;

    void save(DataModifier modifier) throws Exception;

}
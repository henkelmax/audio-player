package de.maxhenkel.audioplayer.api.data;

public interface DataModifier extends DataAccessor {

    void setString(String key, String value);

    void setInt(String key, int value);

    void setLong(String key, long value);

    void setDouble(String key, double value);

    void setFloat(String key, float value);

    void setBoolean(String key, boolean value);

    void remove(String key);

}

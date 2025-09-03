package de.maxhenkel.audioplayer.api.data;

import javax.annotation.Nullable;
import java.util.Set;

public interface DataAccessor {

    boolean has(String key);

    Set<String> keys();

    @Nullable
    String getString(String key);

    @Nullable
    Integer getInt(String key);

    @Nullable
    Long getLong(String key);

    @Nullable
    Double getDouble(String key);

    @Nullable
    Float getFloat(String key);

    @Nullable
    Boolean getBoolean(String key);

}

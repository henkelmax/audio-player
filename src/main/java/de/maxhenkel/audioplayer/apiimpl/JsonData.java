package de.maxhenkel.audioplayer.apiimpl;

import de.maxhenkel.audioplayer.api.data.DataModifier;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Set;

public class JsonData implements DataModifier {

    protected final JSONObject rawData;

    public JsonData(JSONObject rawData) {
        this.rawData = rawData;
    }

    public JSONObject getRawData() {
        return rawData;
    }

    @Override
    public boolean has(String key) {
        return rawData.has(key);
    }

    @Override
    public Set<String> keys() {
        return rawData.keySet();
    }

    @Override
    @Nullable
    public String getString(String key) {
        Object opt = rawData.opt(key);
        if (opt instanceof String s) {
            return s;
        }
        return null;
    }

    @Override
    @Nullable
    public Integer getInt(String key) {
        Object opt = rawData.opt(key);
        if (opt instanceof Integer i) {
            return i;
        }
        if (opt instanceof BigInteger bi) {
            return bi.intValue();
        }
        return null;
    }

    @Override
    @Nullable
    public Long getLong(String key) {
        Object opt = rawData.opt(key);
        if (opt instanceof Long l) {
            return l;
        }
        if (opt instanceof Integer i) {
            return Long.valueOf(i);
        }
        if (opt instanceof BigInteger bi) {
            return bi.longValue();
        }
        if (opt instanceof BigDecimal bd) {
            return bd.longValue();
        }
        return null;
    }

    @Override
    @Nullable
    public Double getDouble(String key) {
        Object opt = rawData.opt(key);
        if (opt instanceof Number n) {
            return n.doubleValue();
        }
        return null;
    }

    @Override
    @Nullable
    public Float getFloat(String key) {
        Object opt = rawData.opt(key);
        if (opt instanceof Number n) {
            return n.floatValue();
        }
        return null;
    }

    @Override
    @Nullable
    public Boolean getBoolean(String key) {
        Object opt = rawData.opt(key);
        if (opt instanceof Boolean b) {
            return b;
        }
        return null;
    }

    @Override
    public void setString(String key, String value) {
        rawData.put(key, value);
    }

    @Override
    public void setInt(String key, int value) {
        rawData.put(key, value);
    }

    @Override
    public void setLong(String key, long value) {
        rawData.put(key, value);
    }

    @Override
    public void setDouble(String key, double value) {
        rawData.put(key, value);
    }

    @Override
    public void setFloat(String key, float value) {
        rawData.put(key, value);
    }

    @Override
    public void setBoolean(String key, boolean value) {
        rawData.put(key, value);
    }

    @Override
    public void remove(String key) {
        rawData.remove(key);
    }
}

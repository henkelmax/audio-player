package de.maxhenkel.audioplayer.api.importer;

import javax.annotation.Nullable;

public final class AudioImportInfo {

    @Nullable
    private final String name;

    public AudioImportInfo(@Nullable String name) {
        this.name = name;
    }

    @Nullable
    public String getName() {
        return name;
    }

}

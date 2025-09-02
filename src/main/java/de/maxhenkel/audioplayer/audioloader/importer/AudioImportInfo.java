package de.maxhenkel.audioplayer.audioloader.importer;

import javax.annotation.Nullable;
import java.util.UUID;

public record AudioImportInfo(UUID soundId, @Nullable String name) {
}

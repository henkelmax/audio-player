package de.maxhenkel.audioplayer.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.maxhenkel.admiral.annotations.*;
import de.maxhenkel.audioplayer.audioloader.AudioStorageManager;
import de.maxhenkel.audioplayer.lang.Lang;
import de.maxhenkel.audioplayer.permission.AudioPlayerPermissionManager;
import net.minecraft.commands.CommandSourceStack;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.UUID;

@Command("audioplayer")
@RequiresPermission(AudioPlayerPermissionManager.VOLUME_PERMISSION_STRING)
public class VolumeCommands {

    public static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("#.00");

    @Command("volume")
    public void volumeWithId(CommandContext<CommandSourceStack> context, @Name("id") UUID uuid, @OptionalArgument @Name("volume_percent") @Min("0.01") @Max("100") Float volume) {
        volumeCommand(context, uuid, volume);
    }

    @Command("volume")
    public void volumeHeldItem(CommandContext<CommandSourceStack> context, @OptionalArgument @Name("volume_percent") @Min("0.01") @Max("100") Float volume) throws CommandSyntaxException {
        UUID id = UtilityCommands.getHeldItemId(context);
        if (id == null) {
            return;
        }
        volumeCommand(context, id, volume);
    }

    private void volumeCommand(CommandContext<CommandSourceStack> context, UUID id, @Nullable Float volume) {
        if (!AudioStorageManager.instance().checkSoundExists(id)) {
            context.getSource().sendFailure(Lang.translatable("audioplayer.audio_file_not_found"));
            return;
        }
        if (volume == null) {
            float currentVolume = AudioStorageManager.metadataManager().getVolumeOverride(id).orElse(1F);
            context.getSource().sendSuccess(() -> Lang.translatable("audioplayer.current_volume", PERCENT_FORMAT.format(currentVolume * 100F)), false);
            return;
        }
        if (volume >= 100F) {
            // Will remove volume from metadata, to keep meta file smaller
            AudioStorageManager.metadataManager().setVolumeOverride(id, null);
        }
        AudioStorageManager.metadataManager().setVolumeOverride(id, volume / 100F);
        AudioStorageManager.audioCache().invalidate(id);
        context.getSource().sendSuccess(() -> Lang.translatable("audioplayer.set_volume_successful", PERCENT_FORMAT.format(volume)), false);
    }

}

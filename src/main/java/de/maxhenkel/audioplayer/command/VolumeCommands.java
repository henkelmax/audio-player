package de.maxhenkel.audioplayer.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.maxhenkel.admiral.annotations.*;
import de.maxhenkel.audioplayer.*;
import de.maxhenkel.audioplayer.audioloader.AudioStorageManager;
import de.maxhenkel.audioplayer.audioloader.VolumeOverrideManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.UUID;

@Command("audioplayer")
public class VolumeCommands {

    @RequiresPermission("audioplayer.volume")
    @Command("volume")
    public void volumeWithId(CommandContext<CommandSourceStack> context, @Name("id") UUID uuid, @OptionalArgument @Name("volume") @Min("0.01") @Max("100") Float volume) {
        volumeCommand(context, uuid, volume);
    }

    @RequiresPermission("audioplayer.volume")
    @Command("volume")
    public void volumeHeldItem(CommandContext<CommandSourceStack> context, @OptionalArgument @Name("volume") @Min("0.01") @Max("100") Float volume) throws CommandSyntaxException {
        CustomSound customSound = UtilityCommands.getHeldSound(context);
        if (customSound == null) {
            return;
        }
        if (customSound.isRandomized()) {
            for (UUID id : customSound.getRandomSounds()) {
                volumeCommand(context, id, volume);
            }
            return;
        }
        volumeCommand(context, customSound.getSoundId(), volume);
    }

    private void volumeCommand(CommandContext<CommandSourceStack> context, UUID id, @Nullable Float volume) {
        if (!AudioStorageManager.instance().checkSoundExists(id)) {
            context.getSource().sendFailure(Component.literal("Sound does not exist"));
            return;
        }
        VolumeOverrideManager mgr = AudioStorageManager.volumeOverrideManager();
        DecimalFormat percentFormat = new DecimalFormat("#.00");
        if (volume == null) {
            float currentVolumeLog = mgr.getAudioVolume(id);
            float currentVolume = VolumeOverrideManager.convertToLinearScaleFactor(currentVolumeLog);

            context.getSource().sendSuccess(() -> Component.literal("Current volume is %s%%".formatted(percentFormat.format(currentVolume * 100F))), false);
            return;
        }
        if (volume == 100F) {
            // Will remove volume from json, to keep json file smaller
            mgr.setAudioVolume(id, null);
        }
        float volumeLinear = volume / 100F;
        mgr.setAudioVolume(id, VolumeOverrideManager.convertToLogarithmicScaleFactor(volumeLinear));
        AudioStorageManager.audioCache().invalidate(id);
        context.getSource().sendSuccess(() -> Component.literal("Successfully set sound volume to %s%%, this will apply next time the sound plays".formatted(percentFormat.format(volume))), false);
    }

}

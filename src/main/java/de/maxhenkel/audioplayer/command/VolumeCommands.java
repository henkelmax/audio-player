package de.maxhenkel.audioplayer.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.maxhenkel.admiral.annotations.*;
import de.maxhenkel.audioplayer.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

@Command("audioplayer")
public class VolumeCommands {

    @RequiresPermission("audioplayer.volume")
    @Command("volume")
    public void volumeWithId(CommandContext<CommandSourceStack> context, @Name("id") UUID uuid, @OptionalArgument @Name("volume") @Min("1") @Max("100") Integer volume) {
        volumeCommand(context, uuid, volume);
    }

    @RequiresPermission("audioplayer.volume")
    @Command("volume")
    public void volumeHeldItem(CommandContext<CommandSourceStack> context, @OptionalArgument @Name("volume") @Min("1") @Max("100") Integer volume) throws CommandSyntaxException {
        CustomSound customSound = UtilityCommands.getHeldSound(context);
        if (customSound == null) {
            return;
        }
        volumeCommand(context, customSound.getSoundId(), volume);
    }

    private void volumeCommand(CommandContext<CommandSourceStack> context, UUID id, @Nullable Integer volume) {
        if (!AudioManager.checkSoundExists(context.getSource().getServer(), id)) {
            context.getSource().sendFailure(Component.literal("Sound does not exist"));
            return;
        }
        Optional<VolumeOverrideManager> optionalMgr = VolumeOverrideManager.instance();
        if (optionalMgr.isEmpty()) {
            context.getSource().sendFailure(Component.literal("An internal error occurred"));
            return;
        }
        VolumeOverrideManager mgr = optionalMgr.get();
        if (volume == null) {
            float currentVolume = mgr.getAudioVolume(id);
            context.getSource().sendSuccess(() -> Component.literal("Current volume is %d%%".formatted(Math.round(currentVolume * 100F))), false);
            return;
        }
        if (volume == 100) {
            // Will remove volume from json, to keep json file smaller
            mgr.setAudioVolume(id, null);
        }
        mgr.setAudioVolume(id, volume / 100F);
        AudioPlayer.AUDIO_CACHE.remove(id);
        context.getSource().sendSuccess(() -> Component.literal("Successfully set sound volume to %d%%, this will apply next time the sound plays".formatted(volume)), false);
    }

}

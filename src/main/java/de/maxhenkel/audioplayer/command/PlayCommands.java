package de.maxhenkel.audioplayer.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.maxhenkel.admiral.annotations.Command;
import de.maxhenkel.admiral.annotations.Min;
import de.maxhenkel.admiral.annotations.Name;
import de.maxhenkel.admiral.annotations.RequiresPermission;
import de.maxhenkel.audioplayer.api.data.AudioFileMetadata;
import de.maxhenkel.audioplayer.audioplayback.PlayerManager;
import de.maxhenkel.audioplayer.lang.Lang;
import de.maxhenkel.audioplayer.permission.AudioPlayerPermissionManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

@Command("audioplayer")
@RequiresPermission(AudioPlayerPermissionManager.PLAY_COMMAND_PERMISSION_STRING)
public class PlayCommands {

    @Command("play")
    public void play(CommandContext<CommandSourceStack> context, @Name("audio") AudioFileMetadata metadata, @Name("location") Vec3 location, @Name("range") @Min("0") float range) throws CommandSyntaxException {
        @Nullable ServerPlayer player = context.getSource().getPlayer();
        PlayerManager.instance().playLocational(
                context.getSource().getLevel(),
                location,
                metadata.getAudioId(),
                player,
                range,
                null,
                null
        );
        context.getSource().sendSuccess(() -> Lang.translatable("audioplayer.play_successful", metadata), false);
    }

    @Command("stop")
    private static int stop(CommandContext<CommandSourceStack> context, @Name("audio") AudioFileMetadata metadata) {
        int count = PlayerManager.instance().stopAll(metadata.getAudioId());

        if (count > 0) {
            context.getSource().sendSuccess(() -> Lang.translatable("audioplayer.stop_streams_successful", count), false);
        } else {
            context.getSource().sendFailure(Lang.translatable("audioplayer.no_audio_file_id_found", metadata.getAudioId()));
        }
        return count;
    }

}

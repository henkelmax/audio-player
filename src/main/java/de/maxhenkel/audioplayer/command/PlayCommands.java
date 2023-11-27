package de.maxhenkel.audioplayer.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.maxhenkel.admiral.annotations.Command;
import de.maxhenkel.admiral.annotations.Min;
import de.maxhenkel.admiral.annotations.Name;
import de.maxhenkel.admiral.annotations.RequiresPermission;
import de.maxhenkel.audioplayer.PlayerManager;
import de.maxhenkel.audioplayer.Plugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Command("audioplayer")
public class PlayCommands {

    @RequiresPermission("audioplayer.play_command")
    @Command("play")
    public void play(CommandContext<CommandSourceStack> context, @Name("sound") UUID sound, @Name("location") Vec3 location, @Name("range") @Min("0") float range) throws CommandSyntaxException {
        @Nullable ServerPlayer player = context.getSource().getPlayer();
        VoicechatServerApi api = Plugin.voicechatServerApi;
        if (api == null) {
            return;
        }
        PlayerManager.instance().playLocational(
                api,
                context.getSource().getLevel(),
                location,
                sound,
                player,
                range,
                null,
                Integer.MAX_VALUE
        );
        context.getSource().sendSuccess(() -> Component.literal("Successfully played %s".formatted(sound)), false);
    }

    @RequiresPermission("audioplayer.play_command")
    @Command("stop")
    private static int stop(CommandContext<CommandSourceStack> context, @Name("sound") UUID sound) {
        UUID channelID = PlayerManager.instance().findChannelID(sound);

        if (channelID != null) {
            PlayerManager.instance().stop(channelID);
                context.getSource().sendSuccess(() -> Component.literal("Successfully stopped %s.".formatted(sound)), false);
                return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to stop, Could not find %s".formatted(sound)));
        }
        return 0;
    }

}

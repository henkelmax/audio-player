package de.maxhenkel.audioplayer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.audioplayer.Filebin;
import de.maxhenkel.audioplayer.AudioPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class AudioPlayerCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx, Commands.CommandSelection environment) {
        LiteralArgumentBuilder<CommandSourceStack> literalBuilder = Commands.literal("audioplayer")
                .requires((commandSource) -> commandSource.hasPermission(Math.min(AudioPlayer.SERVER_CONFIG.uploadPermissionLevel.get(), AudioPlayer.SERVER_CONFIG.musicDiscPermissionLevel.get())));

        literalBuilder.then(Commands.literal("upload")
                .requires((commandSource) -> commandSource.hasPermission(AudioPlayer.SERVER_CONFIG.uploadPermissionLevel.get()))
                .executes((context) -> {
                    UUID uuid = UUID.randomUUID();
                    String uploadURL = Filebin.getBin(uuid);

                    MutableComponent msg = Component.literal("Click ")
                            .append(Component.literal("this link")
                                    .withStyle(style -> {
                                        return style
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, uploadURL))
                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to open")));
                                    })
                                    .withStyle(ChatFormatting.GREEN)
                            )
                            .append(" and upload your sound as ")
                            .append(Component.literal("wav").withStyle(ChatFormatting.GRAY))
                            .append(".\n")
                            .append("Once you have uploaded the file, click ")
                            .append(Component.literal("here")
                                    .withStyle(style -> {
                                        return style
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/audioplayer load " + uuid))
                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to confirm upload")));
                                    })
                                    .withStyle(ChatFormatting.GREEN)
                            )
                            .append(".");

                    context.getSource().sendSuccess(msg, false);

                    return 1;
                }));

        literalBuilder.then(Commands.literal("load")
                .requires((commandSource) -> commandSource.hasPermission(AudioPlayer.SERVER_CONFIG.uploadPermissionLevel.get()))
                .then(Commands.argument("id", UuidArgument.uuid())
                        .executes((context) -> {
                            UUID sound = UuidArgument.getUuid(context, "id");

                            new Thread(() -> {
                                try {
                                    context.getSource().sendSuccess(Component.literal("Downloading sound, please wait..."), false);
                                    Filebin.downloadSound(context.getSource().getServer(), sound);
                                    MutableComponent tc = Component.literal("Successfully downloaded sound. ")
                                            .append(ComponentUtils.wrapInSquareBrackets(Component.literal("Copy ID"))
                                                    .withStyle(style -> {
                                                        return style
                                                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, sound.toString()))
                                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Copy sound ID")));
                                                    })
                                                    .withStyle(ChatFormatting.GREEN)
                                            )
                                            .append(" ")
                                            .append(ComponentUtils.wrapInSquareBrackets(Component.literal("Put on music disc"))
                                                    .withStyle(style -> {
                                                        return style
                                                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/audioplayer musicdisc %s".formatted(sound.toString())))
                                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Put the sound on a music disc")));
                                                    })
                                                    .withStyle(ChatFormatting.GREEN)
                                            );

                                    context.getSource().sendSuccess(tc, false);
                                } catch (Exception e) {
                                    AudioPlayer.LOGGER.warn("{} failed to download a sound: {}", context.getSource().getTextName(), e.getMessage());
                                    context.getSource().sendFailure(Component.literal("Failed to download sound: %s".formatted(e.getMessage())));
                                }
                            }).start();

                            return 1;
                        })));

        literalBuilder.then(Commands.literal("musicdisc")
                .requires((commandSource) -> commandSource.hasPermission(AudioPlayer.SERVER_CONFIG.musicDiscPermissionLevel.get()))
                .then(Commands.argument("sound", UuidArgument.uuid())
                        .executes((context) -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            UUID sound = UuidArgument.getUuid(context, "sound");
                            ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
                            renameRecord(context, itemInHand, sound, null);
                            return 1;
                        })));

        literalBuilder.then(Commands.literal("musicdisc")
                .requires((commandSource) -> commandSource.hasPermission(AudioPlayer.SERVER_CONFIG.musicDiscPermissionLevel.get()))
                .then(Commands.argument("sound", UuidArgument.uuid())
                        .then(Commands.argument("custom_name", StringArgumentType.string())
                                .executes((context) -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    UUID sound = UuidArgument.getUuid(context, "sound");
                                    ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
                                    String customName = StringArgumentType.getString(context, "custom_name");
                                    renameRecord(context, itemInHand, sound, customName);
                                    return 1;
                                }))));

        dispatcher.register(literalBuilder);
    }

    private static void renameRecord(CommandContext<CommandSourceStack> context, ItemStack stack, UUID soundID, @Nullable String name) {
        if (!(stack.getItem() instanceof RecordItem)) {
            context.getSource().sendFailure(Component.literal("You don't have a music disc in your main hand"));
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID("CustomSound", soundID);

        ListTag lore = new ListTag();
        if (name != null) {
            lore.add(0, StringTag.valueOf(Component.Serializer.toJson(Component.literal(name).withStyle(style -> style.withItalic(false)).withStyle(ChatFormatting.GRAY))));
        }

        CompoundTag display = new CompoundTag();
        display.put(ItemStack.TAG_LORE, lore);
        tag.put(ItemStack.TAG_DISPLAY, display);

        tag.putInt("HideFlags", ItemStack.TooltipPart.ADDITIONAL.getMask());

        context.getSource().sendSuccess(Component.literal("Successfully updated music disc"), false);
    }

}

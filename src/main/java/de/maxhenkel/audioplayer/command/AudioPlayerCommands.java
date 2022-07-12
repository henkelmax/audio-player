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
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Predicate;

public class AudioPlayerCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx, Commands.CommandSelection environment) {
        LiteralArgumentBuilder<CommandSourceStack> literalBuilder = Commands.literal("audioplayer")
                .requires((commandSource) -> commandSource.hasPermission(Math.min(AudioPlayer.SERVER_CONFIG.uploadPermissionLevel.get(), AudioPlayer.SERVER_CONFIG.applyToItemPermissionLevel.get())));

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
                                            ).append(" ")
                                            .append(ComponentUtils.wrapInSquareBrackets(Component.literal("Put on goat horn"))
                                                    .withStyle(style -> {
                                                        return style
                                                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/audioplayer goathorn %s".formatted(sound.toString())))
                                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Put the sound on a goat horn")));
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

        literalBuilder.then(applyCommand(Commands.literal("musicdisc"), itemStack -> itemStack.getItem() instanceof RecordItem, "Music Disc"));
        literalBuilder.then(applyCommand(Commands.literal("goathorn"), itemStack -> itemStack.getItem() instanceof InstrumentItem, "Goat Horn"));

        dispatcher.register(literalBuilder);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> applyCommand(LiteralArgumentBuilder<CommandSourceStack> builder, Predicate<ItemStack> validator, String itemTypeName) {
        return builder.requires((commandSource) -> commandSource.hasPermission(AudioPlayer.SERVER_CONFIG.applyToItemPermissionLevel.get()))
                .then(Commands.argument("sound", UuidArgument.uuid())
                        .executes((context) -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            UUID sound = UuidArgument.getUuid(context, "sound");
                            ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
                            if (validator.test(itemInHand)) {
                                renameItem(context, itemInHand, sound, null);
                            } else {
                                context.getSource().sendFailure(Component.literal("You don't have a %s in your main hand".formatted(itemTypeName)));
                            }
                            return 1;
                        })
                        .then(Commands.argument("custom_name", StringArgumentType.string())
                                .executes((context) -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    UUID sound = UuidArgument.getUuid(context, "sound");
                                    ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
                                    String customName = StringArgumentType.getString(context, "custom_name");
                                    if (validator.test(itemInHand)) {
                                        renameItem(context, itemInHand, sound, customName);
                                    } else {
                                        context.getSource().sendFailure(Component.literal("You don't have a %s in your main hand".formatted(itemTypeName)));
                                    }
                                    return 1;
                                })));
    }

    private static void renameItem(CommandContext<CommandSourceStack> context, ItemStack stack, UUID soundID, @Nullable String name) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID("CustomSound", soundID);

        if (tag.contains("instrument", Tag.TAG_STRING)) {
            tag.putString("instrument", "");
        }

        ListTag lore = new ListTag();
        if (name != null) {
            lore.add(0, StringTag.valueOf(Component.Serializer.toJson(Component.literal(name).withStyle(style -> style.withItalic(false)).withStyle(ChatFormatting.GRAY))));
        }

        CompoundTag display = new CompoundTag();
        display.put(ItemStack.TAG_LORE, lore);
        tag.put(ItemStack.TAG_DISPLAY, display);

        tag.putInt("HideFlags", ItemStack.TooltipPart.ADDITIONAL.getMask());

        context.getSource().sendSuccess(Component.literal("Successfully updated ").append(stack.getHoverName()), false);
    }

}

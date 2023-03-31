package de.maxhenkel.audioplayer.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.audioplayer.AudioManager;
import de.maxhenkel.audioplayer.AudioPlayer;
import de.maxhenkel.audioplayer.Filebin;
import net.fabricmc.fabric.api.util.NbtType;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.net.UnknownHostException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AudioPlayerCommands {

    public static final Pattern SOUND_FILE_PATTERN = Pattern.compile("^[a-z0-9_ -]+.((wav)|(mp3))$", Pattern.CASE_INSENSITIVE);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx, Commands.CommandSelection environment) {
        LiteralArgumentBuilder<CommandSourceStack> literalBuilder = Commands.literal("audioplayer")
                .requires((commandSource) -> commandSource.hasPermission(Math.min(AudioPlayer.SERVER_CONFIG.uploadPermissionLevel.get(), AudioPlayer.SERVER_CONFIG.applyToItemPermissionLevel.get())));

        literalBuilder.executes(context -> {
            context.getSource().sendSuccess(
                    Component.literal("Upload audio via Filebin ")
                            .append(Component.literal("here").withStyle(style -> {
                                return style
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/audioplayer upload"))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to show more")));
                            }).withStyle(ChatFormatting.GREEN))
                            .append(".")
                    , false);
            context.getSource().sendSuccess(
                    Component.literal("Upload audio with access to the servers file system ")
                            .append(Component.literal("here").withStyle(style -> {
                                return style
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/audioplayer serverfile"))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to show more")));
                            }).withStyle(ChatFormatting.GREEN))
                            .append(".")
                    , false);
            context.getSource().sendSuccess(
                    Component.literal("Upload audio from a URL ")
                            .append(Component.literal("here").withStyle(style -> {
                                return style
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/audioplayer url"))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to show more")));
                            }).withStyle(ChatFormatting.GREEN))
                            .append(".")
                    , false);
            return 1;
        });

        literalBuilder.then(Commands.literal("upload")
                .requires((commandSource) -> commandSource.hasPermission(AudioPlayer.SERVER_CONFIG.uploadPermissionLevel.get()))
                .executes(filebinCommand())
        );

        literalBuilder.then(Commands.literal("filebin")
                .requires((commandSource) -> commandSource.hasPermission(AudioPlayer.SERVER_CONFIG.uploadPermissionLevel.get()))
                .executes(filebinCommand())
                .then(Commands.argument("id", UuidArgument.uuid())
                        .executes((context) -> {
                            UUID sound = UuidArgument.getUuid(context, "id");

                            new Thread(() -> {
                                try {
                                    context.getSource().sendSuccess(Component.literal("Downloading sound, please wait..."), false);
                                    Filebin.downloadSound(context.getSource().getServer(), sound);
                                    context.getSource().sendSuccess(sendUUIDMessage(sound, Component.literal("Successfully downloaded sound.")), false);
                                } catch (Exception e) {
                                    AudioPlayer.LOGGER.warn("{} failed to download a sound: {}", context.getSource().getTextName(), e.getMessage());
                                    context.getSource().sendFailure(Component.literal("Failed to download sound: %s".formatted(e.getMessage())));
                                }
                            }).start();

                            return 1;
                        }))
        );

        literalBuilder.then(Commands.literal("url")
                .requires((commandSource) -> commandSource.hasPermission(AudioPlayer.SERVER_CONFIG.uploadPermissionLevel.get()))
                .executes(context -> {
                    context.getSource().sendSuccess(
                            Component.literal("If you have a direct link to a ")
                                    .append(Component.literal(".mp3").withStyle(ChatFormatting.GRAY))
                                    .append(" or ")
                                    .append(Component.literal(".wav").withStyle(ChatFormatting.GRAY))
                                    .append(" file, enter the following command: ")
                                    .append(Component.literal("/audioplayer url <link-to-your-file>").withStyle(ChatFormatting.GRAY).withStyle(style -> {
                                        return style
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/audioplayer url "))
                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to fill in the command")));
                                    }))
                                    .append(".")
                            , false);
                    return 1;
                })
                .then(Commands.argument("url", StringArgumentType.string())
                        .executes((context) -> {
                            String url = StringArgumentType.getString(context, "url");
                            UUID sound = UUID.randomUUID();
                            new Thread(() -> {
                                try {
                                    context.getSource().sendSuccess(Component.literal("Downloading sound, please wait..."), false);
                                    AudioManager.saveSound(context.getSource().getServer(), sound, url);
                                    context.getSource().sendSuccess(sendUUIDMessage(sound, Component.literal("Successfully downloaded sound.")), false);
                                } catch (UnknownHostException e) {
                                    AudioPlayer.LOGGER.warn("{} failed to download a sound: {}", context.getSource().getTextName(), e.toString());
                                    context.getSource().sendFailure(Component.literal("Failed to download sound: Unknown host"));
                                } catch (UnsupportedAudioFileException e) {
                                    AudioPlayer.LOGGER.warn("{} failed to download a sound: {}", context.getSource().getTextName(), e.toString());
                                    context.getSource().sendFailure(Component.literal("Failed to download sound: Invalid file format"));
                                } catch (Exception e) {
                                    AudioPlayer.LOGGER.warn("{} failed to download a sound: {}", context.getSource().getTextName(), e.toString());
                                    context.getSource().sendFailure(Component.literal("Failed to download sound: %s".formatted(e.getMessage())));
                                }
                            }).start();

                            return 1;
                        }))
        );

        literalBuilder.then(Commands.literal("serverfile")
                .requires((commandSource) -> commandSource.hasPermission(AudioPlayer.SERVER_CONFIG.uploadPermissionLevel.get()))
                .executes(context -> {
                    context.getSource().sendSuccess(
                            Component.literal("Upload a ")
                                    .append(Component.literal(".mp3").withStyle(ChatFormatting.GRAY))
                                    .append(" or ")
                                    .append(Component.literal(".wav").withStyle(ChatFormatting.GRAY))
                                    .append(" file to ")
                                    .append(Component.literal(AudioManager.getUploadFolder().toAbsolutePath().toString()).withStyle(ChatFormatting.GRAY))
                                    .append(" on the server and run the command ")
                                    .append(Component.literal("/audioplayer serverfile \"yourfile.mp3\"").withStyle(ChatFormatting.GRAY).withStyle(style -> {
                                        return style
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/audioplayer serverfile "))
                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to fill in the command")));
                                    }))
                                    .append(".")
                            , false);
                    return 1;
                })
                .then(Commands.argument("filename", StringArgumentType.string())
                        .executes((context) -> {
                            String fileName = StringArgumentType.getString(context, "filename");
                            Matcher matcher = SOUND_FILE_PATTERN.matcher(fileName);
                            if (!matcher.matches()) {
                                context.getSource().sendFailure(Component.literal("Invalid file name! Valid characters are ")
                                        .append(Component.literal("A-Z").withStyle(ChatFormatting.GRAY))
                                        .append(", ")
                                        .append(Component.literal("0-9").withStyle(ChatFormatting.GRAY))
                                        .append(", ")
                                        .append(Component.literal("_").withStyle(ChatFormatting.GRAY))
                                        .append(" and ")
                                        .append(Component.literal("-").withStyle(ChatFormatting.GRAY))
                                        .append(". The name must also end in ")
                                        .append(Component.literal(".mp3").withStyle(ChatFormatting.GRAY))
                                        .append(" or ")
                                        .append(Component.literal(".wav").withStyle(ChatFormatting.GRAY))
                                        .append(".")
                                );
                                return 1;
                            }
                            UUID uuid = UUID.randomUUID();
                            new Thread(() -> {
                                Path file = AudioManager.getUploadFolder().resolve(fileName);
                                try {
                                    AudioManager.saveSound(context.getSource().getServer(), uuid, file);
                                    context.getSource().sendSuccess(sendUUIDMessage(uuid, Component.literal("Successfully copied sound.")), false);
                                    context.getSource().sendSuccess(Component.literal("Deleted temporary file ").append(Component.literal(fileName).withStyle(ChatFormatting.GRAY)).append("."), false);
                                } catch (NoSuchFileException e) {
                                    context.getSource().sendFailure(Component.literal("Could not find file ").append(Component.literal(fileName).withStyle(ChatFormatting.GRAY)).append("."));
                                } catch (Exception e) {
                                    AudioPlayer.LOGGER.warn("{} failed to copy a sound: {}", context.getSource().getTextName(), e.getMessage());
                                    context.getSource().sendFailure(Component.literal("Failed to copy sound: %s".formatted(e.getMessage())));
                                }
                            }).start();

                            return 1;
                        }))
        );

        literalBuilder.then(applyCommand(Commands.literal("musicdisc"), itemStack -> itemStack.getItem() instanceof RecordItem, "Music Disc"));
        literalBuilder.then(applyCommand(Commands.literal("goathorn"), itemStack -> itemStack.getItem() instanceof InstrumentItem, "Goat Horn"));
        literalBuilder.then(bulkApplyCommand(Commands.literal("musicdisc_bulk"), itemStack -> itemStack.getItem() instanceof BlockItem blockitem && blockitem.getBlock() instanceof ShulkerBoxBlock, "Shulker Box"));

        literalBuilder.then(Commands.literal("clear")
                .executes((context) -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);

                    if (!(itemInHand.getItem() instanceof RecordItem) && !(itemInHand.getItem() instanceof InstrumentItem)) {
                        context.getSource().sendFailure(Component.literal("Invalid item"));
                        return 1;
                    }

                    if (!itemInHand.hasTag()) {
                        context.getSource().sendFailure(Component.literal("Item does not contain NBT data"));
                        return 1;
                    }

                    CompoundTag tag = itemInHand.getTag();

                    if (tag == null) {
                        return 1;
                    }

                    if (!tag.contains("CustomSound")) {
                        context.getSource().sendFailure(Component.literal("Item does not have custom audio"));
                        return 1;
                    }

                    tag.remove("CustomSound");

                    if (itemInHand.getItem() instanceof InstrumentItem) {
                        tag.putString("instrument", "minecraft:ponder_goat_horn");
                    }

                    tag.remove(ItemStack.TAG_DISPLAY);
                    tag.remove("HideFlags");

                    context.getSource().sendSuccess(Component.literal("Successfully cleared item"), false);
                    return 1;
                })
        );

        literalBuilder.then(Commands.literal("id")
                .executes((context) -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);

                    if (!(itemInHand.getItem() instanceof RecordItem) && !(itemInHand.getItem() instanceof InstrumentItem)) {
                        context.getSource().sendFailure(Component.literal("Invalid item"));
                        return 1;
                    }

                    if (!itemInHand.hasTag()) {
                        context.getSource().sendFailure(Component.literal("Item does not have custom audio"));
                        return 1;
                    }

                    CompoundTag tag = itemInHand.getTag();

                    if (tag == null) {
                        return 1;
                    }

                    if (!tag.contains("CustomSound")) {
                        context.getSource().sendFailure(Component.literal("Item does not have custom audio"));
                        return 1;
                    }

                    context.getSource().sendSuccess(sendUUIDMessage(tag.getUUID("CustomSound"), Component.literal("Successfully extracted sound ID.")), false);
                    return 1;
                })
        );

        dispatcher.register(literalBuilder);
    }

    public static MutableComponent sendUUIDMessage(UUID soundID, MutableComponent component) {
        return component.append(" ")
                .append(ComponentUtils.wrapInSquareBrackets(Component.literal("Copy ID"))
                        .withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, soundID.toString()))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Copy sound ID")));
                        })
                        .withStyle(ChatFormatting.GREEN)
                )
                .append(" ")
                .append(ComponentUtils.wrapInSquareBrackets(Component.literal("Put on music disc"))
                        .withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/audioplayer musicdisc %s".formatted(soundID.toString())))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Put the sound on a music disc")));
                        })
                        .withStyle(ChatFormatting.GREEN)
                ).append(" ")
                .append(ComponentUtils.wrapInSquareBrackets(Component.literal("Put on goat horn"))
                        .withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/audioplayer goathorn %s".formatted(soundID.toString())))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Put the sound on a goat horn")));
                        })
                        .withStyle(ChatFormatting.GREEN)
                );
    }

    private static Command<CommandSourceStack> filebinCommand() {
        return (context) -> {
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
                    .append(Component.literal("mp3").withStyle(ChatFormatting.GRAY))
                    .append(" or ")
                    .append(Component.literal("wav").withStyle(ChatFormatting.GRAY))
                    .append(".\n")
                    .append("Once you have uploaded the file, click ")
                    .append(Component.literal("here")
                            .withStyle(style -> {
                                return style
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/audioplayer filebin " + uuid))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to confirm upload")));
                            })
                            .withStyle(ChatFormatting.GREEN)
                    )
                    .append(".");

            context.getSource().sendSuccess(msg, false);

            return 1;
        };
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

    private static LiteralArgumentBuilder<CommandSourceStack> bulkApplyCommand(LiteralArgumentBuilder<CommandSourceStack> builder, Predicate<ItemStack> validator, String itemTypeName) {
        return builder.requires((commandSource) -> commandSource.hasPermission(AudioPlayer.SERVER_CONFIG.applyToItemPermissionLevel.get()))
                .then(Commands.argument("sound", UuidArgument.uuid())
                        .executes((context) -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            UUID sound = UuidArgument.getUuid(context, "sound");
                            ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
                            if (validator.test(itemInHand)) {
                                processShulker(context, itemInHand, sound, null);
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
                                        processShulker(context, itemInHand, sound, customName);
                                    } else {
                                        context.getSource().sendFailure(Component.literal("You don't have a %s in your main hand".formatted(itemTypeName)));
                                    }
                                    return 1;
                                })));
    }

    private static void processShulker(CommandContext<CommandSourceStack> context, ItemStack itemInHand, UUID soundID, @Nullable String name) {
        ListTag shulkerContents = itemInHand.getTagElement("BlockEntityTag").getList("Items", 10);
        for (int i = 0; i < shulkerContents.size(); i++) {
            CompoundTag currentItem = shulkerContents.getCompound(i);
            ItemStack itemStack = ItemStack.of(currentItem);
            if (itemStack.getItem() instanceof RecordItem) {
                renameItem(context, itemStack, soundID, name);
                shulkerContents.getCompound(i).put("tag", itemStack.getTag());
            }
        }
        itemInHand.getTagElement("BlockEntityTag").put("Items", shulkerContents);
        context.getSource().sendSuccess(Component.literal("Successfully updated %s contents".formatted(itemInHand.getHoverName())), false);
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

package de.maxhenkel.audioplayer.command;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.maxhenkel.admiral.annotations.Command;
import de.maxhenkel.admiral.annotations.RequiresPermission;
import de.maxhenkel.audioplayer.AudioManager;
import de.maxhenkel.audioplayer.FilenameMappings;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;

@Command("audioplayer")
public class UtilityCommands {

    @RequiresPermission("audioplayer.apply")
    @Command("clear")
    public void clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        Optional<CompoundTag> optionalTag = getId(context, player, itemInHand);

        if (optionalTag.isEmpty()) {
            return;
        }

        CompoundTag tag = optionalTag.get();

        tag.remove("CustomSound");
        tag.remove("CustomSoundRange");
        tag.remove("IsStaticCustomSound");

        if (itemInHand.getItem() instanceof InstrumentItem) {
            tag.putString("instrument", "minecraft:ponder_goat_horn");
        }

        tag.remove(ItemStack.TAG_DISPLAY);
        tag.remove("HideFlags");

        context.getSource().sendSuccess(() -> Component.literal("Successfully cleared item"), false);
    }

    @Command("id")
    public void id(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        Optional<CompoundTag> optionalTag = getId(context, player, itemInHand);

        if (optionalTag.isEmpty()) {
            return;
        }

        CompoundTag tag = optionalTag.get();
        context.getSource().sendSuccess(() -> UploadCommands.sendUUIDMessage(tag.getUUID("CustomSound"), Component.literal("Successfully extracted sound ID.")), false);
    }

    @Command("filename")
    public void filename(CommandContext<CommandSourceStack> context) throws CommandSyntaxException, IOException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        Optional<CompoundTag> optionalTag = getId(context, player, itemInHand);

        if (optionalTag.isEmpty()) {
            return;
        }

        CompoundTag tag = optionalTag.get();
        String soundId = tag.getUUID("CustomSound").toString();

        MinecraftServer server = player.getCommandSenderWorld().getServer();

        File mappingsFile = new File(server.getWorldPath(AudioManager.AUDIO_DATA).resolve("filename_mappings.dat").toUri());

        if (!mappingsFile.exists()) {
            context.getSource().sendFailure(Component.literal(
                "Item has custom audio but filename was not saved when it was created. "+
                "It may have been created in an older version of the AudioPlayer mod.")
            );
            return;
        }

        CompoundTag root = NbtIo.readCompressed(mappingsFile.toPath(), NbtAccounter.unlimitedHeap());
        ListTag mappings = (ListTag) root.get("mappings");

        for (int i = 0; i < mappings.size(); i++) {
            CompoundTag entry = mappings.getCompound(i);
            String uuid = entry.getString("uuid").replaceAll(".wav|.mp3", "");

            if (uuid.equals(soundId)) {
                String filename = entry.getString("filename");

                MutableComponent msg = Component.literal("Successfully retrieved sound Filename. ")
                    .append(Component.literal("[Copy Filename]")
                        .withStyle(style -> {
                            return style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, filename))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Copy sound Filename")));
                        })
                        .withStyle(ChatFormatting.GREEN)
                    );

                context.getSource().sendSuccess(() -> msg, false);
                FilenameMappings.verifyFilesExist(server);
                return;
            }
        }

        context.getSource().sendFailure(Component.literal(
            "Failed to retrieved sound filename.\n"+
            "If you updated from an older version of the mod, existing sounds won't be able to retrieve the original filenames."
        ));
    }

    private static Optional<CompoundTag> getId(CommandContext<CommandSourceStack> context, ServerPlayer player, ItemStack itemInHand) throws CommandSyntaxException {
        if (!(itemInHand.getItem() instanceof RecordItem) && !(itemInHand.getItem() instanceof InstrumentItem)) {
            context.getSource().sendFailure(Component.literal("Invalid item"));
            return Optional.empty();
        }

        if (!itemInHand.hasTag()) {
            context.getSource().sendFailure(Component.literal("Item does not have custom audio"));
            return Optional.empty();
        }

        CompoundTag tag = itemInHand.getTag();

        if (tag == null) {
            return Optional.empty();
        }

        if (!tag.contains("CustomSound")) {
            context.getSource().sendFailure(Component.literal("Item does not have custom audio"));
            return Optional.empty();
        }

        return Optional.of(tag);
    }
}

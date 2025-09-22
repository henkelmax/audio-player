package de.maxhenkel.audioplayer.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.maxhenkel.admiral.annotations.*;
import de.maxhenkel.audioplayer.api.AudioPlayerModule;
import de.maxhenkel.audioplayer.audioloader.AudioData;
import de.maxhenkel.audioplayer.audioloader.AudioStorageManager;
import de.maxhenkel.audioplayer.audioloader.Metadata;
import de.maxhenkel.audioplayer.lang.Lang;
import de.maxhenkel.audioplayer.permission.AudioPlayerPermissionManager;
import de.maxhenkel.audioplayer.audioplayback.PlayerType;
import de.maxhenkel.audioplayer.utils.ChatUtils;
import de.maxhenkel.configbuilder.entry.ConfigEntry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Command("audioplayer")
@RequiresPermission(AudioPlayerPermissionManager.APPLY_PERMISSION_STRING)
public class ApplyCommands {

    @Command("apply")
    public void apply(CommandContext<CommandSourceStack> context, @Name("file_name") String fileName, @OptionalArgument @Name("custom_name") String customName) throws CommandSyntaxException {
        UUID id = getId(context, fileName);
        if (id == null) {
            return;
        }
        applyBulk(context, AudioData.withSoundAndRange(id, null), customName);
    }

    // The apply commands for UUIDs must be below the ones with file names, so that the file name does not overwrite the UUID argument

    @Command("apply")
    public void apply(CommandContext<CommandSourceStack> context, @Name("sound_id") UUID sound, @OptionalArgument @Name("custom_name") String customName) throws CommandSyntaxException {
        applyBulk(context, AudioData.withSoundAndRange(sound, null), customName);
    }

    @Command("range")
    public void range(CommandContext<CommandSourceStack> context, @Name("range") @Min("0") float range) throws CommandSyntaxException {
        int amount = forEachHeldAudioItem(context, AudioData::of, (itemStack, data) -> {
            Optional<AudioPlayerModule> module = data.getModule(AudioPlayerModule.KEY);
            if (module.isEmpty()) {
                return false;
            }
            AudioPlayerModule oldMod = module.get();
            AudioPlayerModule mod = new AudioPlayerModule(oldMod.getSoundId(), range);
            data.setModule(AudioPlayerModule.KEY, mod);
            data.saveToItem(itemStack);
            return true;
        });
        sendUpdateFeedBack(context, amount);
    }

    @Nullable
    private static UUID getId(CommandContext<CommandSourceStack> context, String fileName) {
        try {
            return UUID.fromString(fileName);
        } catch (Exception ignored) {
        }

        List<Metadata> metadata = AudioStorageManager.metadataManager().getByFileName(fileName, true);

        if (metadata.isEmpty()) {
            context.getSource().sendFailure(Lang.translatable("audioplayer.no_audio_file_name_found", fileName));
            return null;
        }

        if (metadata.size() == 1) {
            return metadata.getFirst().getAudioId();
        }

        context.getSource().sendSuccess(() -> Lang.translatable("audioplayer.multiple_audio_files_name_found", fileName), false);
        for (Metadata meta : metadata) {
            context.getSource().sendSuccess(() -> ChatUtils.createInfoMessage(meta.getAudioId()), false);
        }
        return null;
    }

    private static <T> int forEachHeldAudioItem(CommandContext<CommandSourceStack> context, Function<ItemStack, T> shouldProcess, ApplyFunction<T> process) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        BundleContents bundle = itemInHand.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null) {
            List<ItemStack> bundleContents = bundle.itemCopyStream().toList();
            int amount = 0;
            for (ItemStack itemStack : bundleContents) {
                T value = shouldProcess.apply(itemStack);
                if (value == null) {
                    continue;
                }
                if (!process.apply(itemStack, value)) {
                    continue;
                }
                amount++;
            }
            itemInHand.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(bundleContents));
            return amount;
        }

        ItemContainerContents contents = itemInHand.get(DataComponents.CONTAINER);
        if (contents != null) {
            NonNullList<ItemStack> shulkerContents = NonNullList.withSize(ShulkerBoxBlockEntity.CONTAINER_SIZE, ItemStack.EMPTY);
            contents.copyInto(shulkerContents);
            int amount = 0;
            for (ItemStack itemStack : shulkerContents) {
                T value = shouldProcess.apply(itemStack);
                if (value == null) {
                    continue;
                }
                if (!process.apply(itemStack, value)) {
                    continue;
                }
                amount++;
            }
            itemInHand.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(shulkerContents));
            return amount;
        }

        T value = shouldProcess.apply(itemInHand);
        if (value == null) {
            return -1;
        }
        process.apply(itemInHand, value);
        return 1;
    }

    private static void applyBulk(CommandContext<CommandSourceStack> context, AudioData data, @Nullable String customName) throws CommandSyntaxException {
        UUID id = data.getActualSoundId();
        if (id == null || !AudioStorageManager.instance().checkSoundExists(id)) {
            context.getSource().sendFailure(Lang.translatable("audioplayer.no_audio_file_id_found", id == null ? "N/A" : id.toString()));
            return;
        }
        int amount = forEachHeldAudioItem(context, PlayerType::fromItemStack, (itemStack, playerType) -> applyToSingleItem(itemStack, playerType, data, customName));
        sendUpdateFeedBack(context, amount);
    }

    private static void sendUpdateFeedBack(CommandContext<CommandSourceStack> context, int amount) {
        if (amount < 0) {
            context.getSource().sendFailure(Lang.translatable("audioplayer.no_valid_item_in_hand"));
        } else if (amount == 0) {
            context.getSource().sendFailure(Lang.translatable("audioplayer.no_valid_items_found"));
        } else if (amount == 1) {
            context.getSource().sendSuccess(() -> Lang.translatable("audioplayer.item_update_successful"), false);
        } else {
            context.getSource().sendSuccess(() -> Lang.translatable("audioplayer.item_updates_successful", amount), false);
        }
    }

    private static boolean applyToSingleItem(ItemStack stack, PlayerType type, AudioData data, @Nullable String customName) throws CommandSyntaxException {
        checkRange(type.getMaxRange(), data.getRange());
        if (!type.isValid(stack)) {
            return false;
        }
        data.saveToItem(stack, customName);
        return true;
    }

    private static void checkRange(ConfigEntry<Float> maxRange, @Nullable Float range) throws CommandSyntaxException {
        if (range == null) {
            return;
        }
        if (range > maxRange.get()) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.floatTooHigh().create(range, maxRange.get());
        }
    }

    private interface ApplyFunction<T> {
        boolean apply(ItemStack stack, T t) throws CommandSyntaxException;
    }

}

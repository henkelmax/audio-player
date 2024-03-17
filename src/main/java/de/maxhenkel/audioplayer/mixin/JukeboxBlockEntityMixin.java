package de.maxhenkel.audioplayer.mixin;

import de.maxhenkel.audioplayer.AudioManager;
import de.maxhenkel.audioplayer.CustomSound;
import de.maxhenkel.audioplayer.PlayerManager;
import de.maxhenkel.audioplayer.PlayerType;
import de.maxhenkel.audioplayer.interfaces.ChannelHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.UUID;

@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlockEntityMixin extends BlockEntity implements Clearable, ChannelHolder {

    @Shadow
    private ItemStack item;

    @Unique
    @Nullable
    private UUID channelId;

    public JukeboxBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Nullable
    @Override
    public UUID soundplayer$getChannelID() {
        return channelId;
    }

    @Override
    public void soundplayer$setChannelID(@Nullable UUID channelID) {
        this.channelId = channelID;
        setChanged();
    }

    @Inject(method = "setTheItem", at = @At(value = "RETURN"))
    public void setItem(ItemStack itemStack, CallbackInfo ci) {
        if (itemStack.isEmpty() && channelId != null) {
            PlayerManager.instance().stop(channelId);
            channelId = null;
        }
    }

    @Inject(method = "splitTheItem", at = @At(value = "RETURN"))
    public void removeItem(int i, CallbackInfoReturnable<ItemStack> cir) {
        if (item.isEmpty() && channelId != null) {
            PlayerManager.instance().stop(channelId);
            channelId = null;
        }
    }

    @Redirect(method = "startPlaying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;levelEvent(Lnet/minecraft/world/entity/player/Player;ILnet/minecraft/core/BlockPos;I)V"))
    public void startPlaying(Level instance, Player player, int i1, BlockPos blockPos, int i2) {
        CustomSound customSound = CustomSound.of(item);
        if (customSound != null) {
            UUID channel = AudioManager.play((ServerLevel) level, getBlockPos(), PlayerType.MUSIC_DISC, customSound, player);
            if (channel != null) {
                channelId = channel;
                return;
            }
        }

        instance.levelEvent(player, i1, blockPos, i2);

    }

    @Inject(method = "shouldRecordStopPlaying", at = @At(value = "RETURN"), cancellable = true)
    public void shouldRecordStopPlaying(RecordItem recordItem, CallbackInfoReturnable<Boolean> ci) {
        if (channelId == null) {
            return;
        }
        ci.setReturnValue(!PlayerManager.instance().isPlaying(channelId));
    }

    @Inject(method = "load", at = @At(value = "RETURN"))
    public void load(CompoundTag compound, CallbackInfo ci) {
        if (compound.hasUUID("ChannelID") && !item.isEmpty()) {
            channelId = compound.getUUID("ChannelID");
        } else {
            channelId = null;
        }
    }

    @Inject(method = "saveAdditional", at = @At(value = "RETURN"))
    public void save(CompoundTag compound, CallbackInfo ci) {
        if (channelId != null && !item.isEmpty()) {
            compound.putUUID("ChannelID", channelId);
        }
    }
}

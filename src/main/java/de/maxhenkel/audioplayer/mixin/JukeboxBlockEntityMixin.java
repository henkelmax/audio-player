package de.maxhenkel.audioplayer.mixin;

import de.maxhenkel.audioplayer.AudioManager;
import de.maxhenkel.audioplayer.PlayerManager;
import de.maxhenkel.audioplayer.interfaces.ChannelHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
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
import org.spongepowered.asm.mixin.Final;
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
    @Final
    private NonNullList<ItemStack> items;

    @Unique
    @Nullable
    private UUID channelID;

    public JukeboxBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Nullable
    @Override
    public UUID soundplayer$getChannelID() {
        return channelID;
    }

    @Override
    public void soundplayer$setChannelID(@Nullable UUID channelID) {
        this.channelID = channelID;
        setChanged();
    }

    @Inject(method = "setItem", at = @At(value = "RETURN"))
    public void setItem(int i, ItemStack itemStack, CallbackInfo ci) {
        if (i != 0) {
            return;
        }
        if (itemStack.isEmpty() && channelID != null) {
            PlayerManager.instance().stop(channelID);
            channelID = null;
        }
    }

    @Inject(method = "removeItem", at = @At(value = "RETURN"))
    public void removeItem(int i, int j, CallbackInfoReturnable<ItemStack> ci) {
        if (items.get(i).isEmpty() && channelID != null) {
            PlayerManager.instance().stop(channelID);
            channelID = null;
        }
    }

    @Redirect(method = "startPlaying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;levelEvent(Lnet/minecraft/world/entity/player/Player;ILnet/minecraft/core/BlockPos;I)V"))
    public void startPlaying(Level instance, Player player, int i1, BlockPos blockPos, int i2) {
        if (!AudioManager.playCustomMusicDisc((ServerLevel) level, getBlockPos(), items.get(0), null)) {
            instance.levelEvent(player, i1, blockPos, i2);
        }
    }

    @Inject(method = "shouldRecordStopPlaying", at = @At(value = "RETURN"), cancellable = true)
    public void shouldRecordStopPlaying(RecordItem recordItem, CallbackInfoReturnable<Boolean> ci) {
        if (channelID == null) {
            return;
        }
        ci.setReturnValue(!PlayerManager.instance().isPlaying(channelID));
    }

    @Inject(method = "load", at = @At(value = "RETURN"))
    public void load(CompoundTag compound, CallbackInfo ci) {
        if (compound.hasUUID("ChannelID") && !items.get(0).isEmpty()) {
            channelID = compound.getUUID("ChannelID");
        } else {
            channelID = null;
        }
    }

    @Inject(method = "saveAdditional", at = @At(value = "RETURN"))
    public void save(CompoundTag compound, CallbackInfo ci) {
        if (channelID != null && !items.get(0).isEmpty()) {
            compound.putUUID("ChannelID", channelID);
        }
    }
}

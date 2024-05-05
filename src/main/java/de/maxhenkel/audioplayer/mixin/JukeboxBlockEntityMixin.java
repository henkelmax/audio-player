package de.maxhenkel.audioplayer.mixin;

import de.maxhenkel.audioplayer.PlayerManager;
import de.maxhenkel.audioplayer.interfaces.ChannelHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.UUID;

@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlockEntityMixin extends BlockEntity implements Clearable, ChannelHolder {

    @Shadow
    private ItemStack record;

    @Unique
    @Nullable
    private UUID channelId;

    public JukeboxBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Nullable
    @Override
    public UUID audioplayer$getChannelID() {
        return channelId;
    }

    @Override
    public void audioplayer$setChannelID(@Nullable UUID channelID) {
        this.channelId = channelID;
        setChanged();
    }

    @Inject(method = "setRecord", at = @At(value = "RETURN"))
    private void setRecord(ItemStack record, CallbackInfo ci) {
        if (record.isEmpty() && channelId != null) {
            PlayerManager.instance().stop(channelId);
            channelId = null;
        }
    }

    @Inject(method = "load", at = @At(value = "RETURN"))
    private void load(CompoundTag compound, CallbackInfo ci) {
        if (compound.hasUUID("ChannelID") && !record.isEmpty()) {
            channelId = compound.getUUID("ChannelID");
        } else {
            channelId = null;
        }
    }

    @Inject(method = "saveAdditional", at = @At(value = "RETURN"))
    private void save(CompoundTag compound, CallbackInfo ci) {
        if (channelId != null && !record.isEmpty()) {
            compound.putUUID("ChannelID", channelId);
        }
    }
}

package de.maxhenkel.audioplayer.mixin;

import de.maxhenkel.audioplayer.PlayerManager;
import de.maxhenkel.audioplayer.interfaces.IJukebox;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.UUID;

@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlockEntityMixin extends BlockEntity implements Clearable, IJukebox {

    @Shadow
    private ItemStack record;

    @Nullable
    private UUID channelID;

    public JukeboxBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Nullable
    @Override
    public UUID getChannelID() {
        return channelID;
    }

    @Override
    public void setChannelID(@Nullable UUID channelID) {
        this.channelID = channelID;
        setChanged();
    }


    @Inject(method = "setRecord", at = @At(value = "RETURN"))
    private void setRecord(ItemStack record, CallbackInfo ci) {
        if (record.isEmpty() && channelID != null) {
            PlayerManager.instance().stop(channelID);
        }
    }

    @Inject(method = "load", at = @At(value = "RETURN"))
    private void load(CompoundTag compound, CallbackInfo ci) {
        if (compound.hasUUID("ChannelID") && !record.isEmpty()) {
            channelID = compound.getUUID("ChannelID");
        } else {
            channelID = null;
        }
    }

    @Inject(method = "save", at = @At(value = "RETURN"))
    private void save(CompoundTag compound, CallbackInfoReturnable<CompoundTag> ci) {
        if (channelID != null && !record.isEmpty()) {
            compound.putUUID("ChannelID", channelID);
        }
    }
}

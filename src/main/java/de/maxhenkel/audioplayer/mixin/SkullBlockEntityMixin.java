package de.maxhenkel.audioplayer.mixin;

import de.maxhenkel.audioplayer.PlayerManager;
import de.maxhenkel.audioplayer.interfaces.ChannelHolder;
import de.maxhenkel.audioplayer.interfaces.CustomSoundHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

@Mixin(SkullBlockEntity.class)
public class SkullBlockEntityMixin extends BlockEntity implements CustomSoundHolder, ChannelHolder {

    @Unique
    @Nullable
    private UUID channelID;

    @Unique
    @Nullable
    private UUID soundID;

    @Unique
    @Nullable
    private Float range;

    @Unique
    private boolean isStatic;

    public SkullBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
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

    @Override
    public UUID soundplayer$getSoundID() {
        return soundID;
    }

    @Override
    public void soundplayer$setSoundID(UUID soundID) {
        this.soundID = soundID;
        setChanged();
    }

    @Override
    public Optional<Float> soundplayer$getRange() {
        return Optional.ofNullable(range);
    }

    @Override
    public void soundplayer$setRange(Optional<Float> range) {
        this.range = range.orElse(null);
        setChanged();
    }

    @Override
    public boolean soundplayer$isStatic() {
        return isStatic;
    }

    @Override
    public void soundplayer$setStatic(boolean staticSound) {
        this.isStatic = staticSound;
        setChanged();
    }

    @Inject(method = "saveAdditional", at = @At("RETURN"))
    private void saveAdditional(CompoundTag tag, CallbackInfo ci) {
        if (channelID != null) {
            tag.putUUID("ChannelID", channelID);
        }
        if (soundID != null) {
            tag.putUUID("CustomSound", soundID);
        }
        if (range != null) {
            tag.putFloat("CustomSoundRange", range);
        }
        if (isStatic) {
            tag.putBoolean("IsStaticCustomSound", true);
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void load(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("ChannelID")) {
            channelID = tag.getUUID("ChannelID");
        } else {
            channelID = null;
        }
        if (tag.contains("CustomSound")) {
            soundID = tag.getUUID("CustomSound");
        } else {
            soundID = null;
        }
        if (tag.contains("CustomSoundRange")) {
            range = tag.getFloat("CustomSoundRange");
        } else {
            range = null;
        }
        if (tag.contains("IsStaticCustomSound")) {
            isStatic = tag.getBoolean("IsStaticCustomSound");
        } else {
            isStatic = false;
        }
    }

    @Override
    public void setRemoved() {
        if (channelID != null) {
            PlayerManager.instance().stop(channelID);
        }
        super.setRemoved();
    }
}

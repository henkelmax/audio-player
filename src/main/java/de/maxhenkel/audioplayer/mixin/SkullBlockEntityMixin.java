package de.maxhenkel.audioplayer.mixin;

import de.maxhenkel.audioplayer.audioplayback.PlayerManager;
import de.maxhenkel.audioplayer.audioloader.AudioData;
import de.maxhenkel.audioplayer.interfaces.ChannelHolder;
import de.maxhenkel.audioplayer.interfaces.AudioDataHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.UUID;

@Mixin(SkullBlockEntity.class)
public class SkullBlockEntityMixin extends BlockEntity implements AudioDataHolder, ChannelHolder {

    @Unique
    @Nullable
    private UUID channelID;

    @Unique
    @Nullable
    private AudioData audioData;

    public SkullBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Nullable
    @Override
    public UUID audioplayer$getChannelID() {
        return channelID;
    }

    @Override
    public void audioplayer$setChannelID(@Nullable UUID channelID) {
        this.channelID = channelID;
        setChanged();
    }

    @Nullable
    @Override
    public AudioData audioplayer$getAudioData() {
        return audioData;
    }

    @Inject(method = "saveAdditional", at = @At("RETURN"))
    private void saveAdditional(ValueOutput valueOutput, CallbackInfo ci) {
        if (channelID != null) {
            valueOutput.store("ChannelID", UUIDUtil.CODEC, channelID);
        }
        if (audioData != null) {
            audioData.saveToValueOutput(valueOutput);
        }
    }

    @Inject(method = "loadAdditional", at = @At("RETURN"))
    private void load(ValueInput valueInput, CallbackInfo ci) {
        channelID = valueInput.read("ChannelID", UUIDUtil.CODEC).orElse(null);
        audioData = AudioData.of(valueInput);
    }

    @Override
    public void setRemoved() {
        if (channelID != null) {
            PlayerManager.instance().stop(channelID);
        }
        super.setRemoved();
    }
}

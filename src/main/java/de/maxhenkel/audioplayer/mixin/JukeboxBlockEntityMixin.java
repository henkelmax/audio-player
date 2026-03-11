package de.maxhenkel.audioplayer.mixin;

import de.maxhenkel.audioplayer.interfaces.CustomJukeboxSongPlayer;
import de.maxhenkel.audioplayer.interfaces.PlayerHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlockEntityMixin extends BlockEntity implements PlayerHolder {

    @Shadow
    private ItemStack item;

    @Shadow
    @Final
    private JukeboxSongPlayer jukeboxSongPlayer;

    /**
     * A weak reference to the player that inserted the item into the jukebox or null if it has been inserted by other means.
     */
    @Unique
    @Nullable
    private WeakReference<ServerPlayer> player;

    public JukeboxBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Redirect(method = "setTheItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/JukeboxSongPlayer;play(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/Holder;)V"))
    public void play(JukeboxSongPlayer instance, LevelAccessor levelAccessor, Holder<JukeboxSong> holder) {
        @Nullable ServerPlayer causingPlayer = audioplayer$getPlayer();
        player = null;
        if (!(levelAccessor instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!(jukeboxSongPlayer instanceof CustomJukeboxSongPlayer customJukeboxSongPlayer)) {
            return;
        }
        boolean custom = customJukeboxSongPlayer.audioplayer$customPlay(serverLevel, item, causingPlayer);
        if (!custom) {
            instance.play(levelAccessor, holder);
        }
    }

    @Redirect(method = "setTheItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/JukeboxSongPlayer;stop(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    public void stop(JukeboxSongPlayer instance, LevelAccessor levelAccessor, BlockState blockState) {
        player = null;
        if (!(jukeboxSongPlayer instanceof CustomJukeboxSongPlayer customJukeboxSongPlayer)) {
            return;
        }
        boolean custom = customJukeboxSongPlayer.audioplayer$customStop();
        if (!custom) {
            instance.stop(levelAccessor, blockState);
        }
    }

    @Inject(method = "loadAdditional", at = @At(value = "RETURN"))
    public void load(ValueInput valueInput, CallbackInfo ci) {
        player = null;
        if (!(jukeboxSongPlayer instanceof CustomJukeboxSongPlayer customJukeboxSongPlayer)) {
            return;
        }
        customJukeboxSongPlayer.audioplayer$onLoad(item, valueInput);
    }

    @Inject(method = "saveAdditional", at = @At(value = "RETURN"))
    public void save(ValueOutput valueOutput, CallbackInfo ci) {
        if (!(jukeboxSongPlayer instanceof CustomJukeboxSongPlayer customJukeboxSongPlayer)) {
            return;
        }
        customJukeboxSongPlayer.audioplayer$onSave(item, valueOutput);
    }

    @Override
    public void audioplayer$setPlayer(@Nullable ServerPlayer player) {
        if (player == null) {
            this.player = null;
            return;
        }
        this.player = new WeakReference<>(player);
    }

    @Nullable
    @Override
    public ServerPlayer audioplayer$getPlayer() {
        return player == null ? null : player.get();
    }
}

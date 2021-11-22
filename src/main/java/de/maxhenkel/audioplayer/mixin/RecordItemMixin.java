package de.maxhenkel.audioplayer.mixin;

import de.maxhenkel.audioplayer.PlayerManager;
import de.maxhenkel.audioplayer.Plugin;
import de.maxhenkel.audioplayer.interfaces.IJukebox;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.util.UUID;

@Mixin(RecordItem.class)
public class RecordItemMixin {

    @Inject(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;levelEvent(Lnet/minecraft/world/entity/player/Player;ILnet/minecraft/core/BlockPos;I)V"), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    private void useOn(UseOnContext useOnContext, CallbackInfoReturnable<InteractionResult> ci, Level level, BlockPos blockPos, BlockState blockState, ItemStack itemStack) {
        if (useOnContext.getLevel().isClientSide()) {
            return;
        }
        CompoundTag tag = itemStack.getTag();
        if (tag == null || !tag.hasUUID("CustomSound")) {
            return;
        }

        UUID customSound = tag.getUUID("CustomSound");

        VoicechatServerApi api = Plugin.voicechatApi;
        if (api == null) {
            ci.setReturnValue(InteractionResult.sidedSuccess(false));
            return;
        }

        @Nullable UUID channelID = PlayerManager.instance().play(api, (ServerLevel) useOnContext.getLevel(), blockPos, customSound);

        if (level.getBlockEntity(blockPos) instanceof IJukebox jukebox) {
            jukebox.setChannelID(channelID);
        }

        itemStack.shrink(1);
        Player player = useOnContext.getPlayer();
        if (player != null) {
            player.awardStat(Stats.PLAY_RECORD);
        }

        ci.setReturnValue(InteractionResult.sidedSuccess(false));
    }

}

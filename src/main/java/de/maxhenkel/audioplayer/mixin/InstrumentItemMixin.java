package de.maxhenkel.audioplayer.mixin;

import de.maxhenkel.audioplayer.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(InstrumentItem.class)
public class InstrumentItemMixin {

    @Inject(method = "use", at = @At(value = "HEAD"), cancellable = true)
    private void useOn(Level level, Player p, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> ci) {
        ItemStack itemInHand = p.getItemInHand(interactionHand);
        CustomSound customSound = CustomSound.of(itemInHand);
        if (customSound == null) {
            return;
        }
        if (!(p instanceof ServerPlayer player)) {
            ci.setReturnValue(InteractionResult.CONSUME);
            return;
        }
        itemInHand.set(DataComponents.INSTRUMENT, ComponentUtils.EMPTY_INSTRUMENT);
        UUID channel = AudioManager.play((ServerLevel) level, p.blockPosition(), PlayerType.GOAT_HORN, customSound, player);
        if (channel == null) {
            return;
        }
        player.startUsingItem(interactionHand);
        player.getCooldowns().addCooldown(itemInHand, AudioPlayer.SERVER_CONFIG.goatHornCooldown.get());
        level.gameEvent(GameEvent.INSTRUMENT_PLAY, player.position(), GameEvent.Context.of(player));
        ci.setReturnValue(InteractionResult.CONSUME);
    }
}

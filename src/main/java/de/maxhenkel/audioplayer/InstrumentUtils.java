package de.maxhenkel.audioplayer;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Instrument;

public class InstrumentUtils {

    public static final Holder<Instrument> EMPTY_INSTRUMENT = Holder.direct(new Instrument(Holder.direct(SoundEvent.createVariableRangeEvent(new ResourceLocation("empty"))), 140, 256F));

}

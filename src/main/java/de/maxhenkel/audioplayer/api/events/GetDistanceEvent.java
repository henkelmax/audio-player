package de.maxhenkel.audioplayer.api.events;

import de.maxhenkel.audioplayer.api.data.ModuleAccessor;
import net.minecraft.world.phys.Vec3;

public interface GetDistanceEvent {

    ModuleAccessor getData();

    float getDefaultDistance();

    float getItemDistance();

    void setDistance(float distance);

    float getDistance();

    Vec3 getPosition();

}

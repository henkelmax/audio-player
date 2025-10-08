package de.maxhenkel.audioplayer.apiimpl.events;

import de.maxhenkel.audioplayer.api.data.ModuleAccessor;
import de.maxhenkel.audioplayer.api.events.GetDistanceEvent;
import de.maxhenkel.audioplayer.audioloader.AudioData;
import net.minecraft.world.phys.Vec3;

public class GetDistanceEventImpl implements GetDistanceEvent {

    private final AudioData itemData;
    private final float defaultDistance;
    private final float itemDistance;
    private float distance;
    private final Vec3 position;

    public GetDistanceEventImpl(AudioData itemData, float defaultDistance, float itemDistance, Vec3 position) {
        this.itemData = itemData;
        this.defaultDistance = defaultDistance;
        this.itemDistance = itemDistance;
        this.distance = itemDistance;
        this.position = position;
    }

    @Override
    public ModuleAccessor getData() {
        return itemData;
    }

    @Override
    public float getDefaultDistance() {
        return defaultDistance;
    }

    @Override
    public float getItemDistance() {
        return itemDistance;
    }

    @Override
    public void setDistance(float distance) {
        this.distance = distance;
    }

    @Override
    public float getDistance() {
        return distance;
    }

    @Override
    public Vec3 getPosition() {
        return position;
    }
}

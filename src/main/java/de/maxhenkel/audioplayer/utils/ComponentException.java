package de.maxhenkel.audioplayer.utils;

import net.minecraft.network.chat.Component;

public class ComponentException extends Exception {

    private final Component component;

    public ComponentException(Component component, Throwable cause) {
        super(component.getString(), cause);
        this.component = component;
    }

    public ComponentException(Component component) {
        super(component.getString());
        this.component = component;
    }

    public Component getComponent() {
        return component;
    }

}

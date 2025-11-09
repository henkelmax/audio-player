package de.maxhenkel.audioplayer.api;

import net.minecraft.network.chat.Component;

public interface MessageReceiver {
    void sendMessage(Component message);
}

package de.maxhenkel.audioplayer;

import de.maxhenkel.admiral.permissions.PermissionManager;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.List;

public class AudioPlayerPermissionManager implements PermissionManager<CommandSourceStack> {

    public static final AudioPlayerPermissionManager INSTANCE = new AudioPlayerPermissionManager();

    private static final Permission UPLOAD_PERMISSION = new Permission("audioplayer.upload", PermissionType.EVERYONE);
    private static final Permission APPLY_PERMISSION = new Permission("audioplayer.apply", PermissionType.EVERYONE);
    private static final Permission PLAY_COMMAND_PERMISSION = new Permission("audioplayer.play_command", PermissionType.OPS);

    private static final List<Permission> PERMISSIONS = List.of(
            UPLOAD_PERMISSION,
            APPLY_PERMISSION,
            PLAY_COMMAND_PERMISSION
    );

    @Override
    public boolean hasPermission(CommandSourceStack stack, String permission) {
        for (Permission p : PERMISSIONS) {
            if (p.permission.equals(permission)) {
                return p.hasPermission(stack.getPlayer());
            }
        }
        return false;
    }

    private static Boolean loaded;

    private static boolean isFabricPermissionsAPILoaded() {
        if (loaded == null) {
            loaded = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");
            if (loaded) {
                AudioPlayer.LOGGER.info("Using Fabric Permissions API");
            }
        }
        return loaded;
    }

    private static class Permission {
        private final String permission;
        private final PermissionType type;

        public Permission(String permission, PermissionType type) {
            this.permission = permission;
            this.type = type;
        }

        public boolean hasPermission(@Nullable ServerPlayer player) {
            if (isFabricPermissionsAPILoaded()) {
                return checkFabricPermission(player);
            }
            return type.hasPermission(player);
        }

        private boolean checkFabricPermission(@Nullable ServerPlayer player) {
            if (player == null) {
                return false;
            }
            TriState permissionValue = Permissions.getPermissionValue(player, permission);
            switch (permissionValue) {
                case DEFAULT:
                    return type.hasPermission(player);
                case TRUE:
                    return true;
                case FALSE:
                default:
                    return false;
            }
        }
    }

    private static enum PermissionType {

        EVERYONE, NOONE, OPS;

        boolean hasPermission(@Nullable ServerPlayer player) {
            return switch (this) {
                case EVERYONE -> true;
                case NOONE -> false;
                case OPS -> player != null && player.hasPermissions(player.server.getOperatorUserPermissionLevel());
            };
        }

    }

}

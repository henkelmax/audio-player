package de.maxhenkel.audioplayer.permission;

import de.maxhenkel.admiral.permissions.PermissionManager;
import de.maxhenkel.audioplayer.AudioPlayerMod;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class AudioPlayerPermissionManager implements PermissionManager<CommandSourceStack> {

    public static final AudioPlayerPermissionManager INSTANCE = new AudioPlayerPermissionManager();

    public static final String VOLUME_PERMISSION_STRING = "audioplayer.volume";
    public static final String UPLOAD_PERMISSION_STRING = "audioplayer.upload";
    public static final String APPLY_PERMISSION_STRING = "audioplayer.apply";
    public static final String PLAY_COMMAND_PERMISSION_STRING = "audioplayer.play_command";

    private static final Permission VOLUME_PERMISSION = new Permission(VOLUME_PERMISSION_STRING, PermissionType.EVERYONE);
    private static final Permission UPLOAD_PERMISSION = new Permission(UPLOAD_PERMISSION_STRING, PermissionType.EVERYONE);
    private static final Permission APPLY_PERMISSION = new Permission(APPLY_PERMISSION_STRING, PermissionType.EVERYONE);
    private static final Permission PLAY_COMMAND_PERMISSION = new Permission(PLAY_COMMAND_PERMISSION_STRING, PermissionType.OPS);

    private static final List<Permission> PERMISSIONS = List.of(
            UPLOAD_PERMISSION,
            APPLY_PERMISSION,
            PLAY_COMMAND_PERMISSION,
            VOLUME_PERMISSION
    );

    @Override
    public boolean hasPermission(CommandSourceStack stack, String permission) {
        for (Permission p : PERMISSIONS) {
            if (!p.permission.equals(permission)) {
                continue;
            }
            if (!p.canUse()) {
                return false;
            }
            if (stack.isPlayer()) {
                return p.hasPermission(stack.getPlayer());
            }
            return stack.hasPermission(2);
        }
        return false;
    }

    private static Boolean loaded;

    private static boolean isFabricPermissionsAPILoaded() {
        if (loaded == null) {
            loaded = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");
            if (loaded) {
                AudioPlayerMod.LOGGER.info("Using Fabric Permissions API");
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

        public boolean canUse() {
            return true;
        }

        public boolean hasPermission(ServerPlayer player) {
            if (isFabricPermissionsAPILoaded()) {
                return checkFabricPermission(player);
            }
            return type.hasPermission(player);
        }

        private boolean checkFabricPermission(ServerPlayer player) {
            TriState permissionValue = Permissions.getPermissionValue(player, permission);
            return switch (permissionValue) {
                case DEFAULT -> type.hasPermission(player);
                case TRUE -> true;
                default -> false;
            };
        }

        public PermissionType getType() {
            return type;
        }

        public String getPermission() {
            return permission;
        }
    }

    private static enum PermissionType {

        EVERYONE, NOONE, OPS;

        boolean hasPermission(ServerPlayer player) {
            return switch (this) {
                case EVERYONE -> true;
                case NOONE -> false;
                case OPS ->
                        player != null && player.hasPermissions(player.level().getServer().operatorUserPermissionLevel());
            };
        }

    }

}

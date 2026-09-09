package de.maxhenkel.audioplayer.permission;

import de.maxhenkel.admiral.permissions.PermissionManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.Permissions;

import java.util.Map;

public class AudioPlayerPermissionManager implements PermissionManager<CommandSourceStack> {

    public static final AudioPlayerPermissionManager INSTANCE = new AudioPlayerPermissionManager();

    public static final String VOLUME_PERMISSION_STRING = "audioplayer:volume";
    public static final String UPLOAD_FILEBIN_PERMISSION_STRING = "audioplayer:upload.filebin";
    public static final String UPLOAD_SERVERFILE_PERMISSION_STRING = "audioplayer:upload.serverfile";
    public static final String UPLOAD_URL_PERMISSION_STRING = "audioplayer:upload.url";
    public static final String UPLOAD_WEB_PERMISSION_STRING = "audioplayer:upload.web";
    public static final String APPLY_PERMISSION_STRING = "audioplayer:apply";
    public static final String RENAME_PERMISSION_STRING = "audioplayer:rename";
    public static final String PLAY_COMMAND_PERMISSION_STRING = "audioplayer:play_command";

    private static final Permission VOLUME_PERMISSION = new Permission(VOLUME_PERMISSION_STRING, PermissionLevel.ALL);
    private static final Permission UPLOAD_FILEBIN_PERMISSION = new Permission(UPLOAD_FILEBIN_PERMISSION_STRING, PermissionLevel.ALL);
    private static final Permission UPLOAD_SERVERFILE_PERMISSION = new Permission(UPLOAD_SERVERFILE_PERMISSION_STRING, PermissionLevel.ALL);
    private static final Permission UPLOAD_URL_PERMISSION = new Permission(UPLOAD_URL_PERMISSION_STRING, PermissionLevel.ALL);
    private static final Permission UPLOAD_WEB_PERMISSION = new Permission(UPLOAD_WEB_PERMISSION_STRING, PermissionLevel.ALL);
    private static final Permission APPLY_PERMISSION = new Permission(APPLY_PERMISSION_STRING, PermissionLevel.ALL);
    private static final Permission RENAME_PERMISSION = new Permission(RENAME_PERMISSION_STRING, PermissionLevel.ALL);
    private static final Permission PLAY_COMMAND_PERMISSION = new Permission(PLAY_COMMAND_PERMISSION_STRING, PermissionLevel.ADMINS);

    private static final Map<String, Permission> PERMISSIONS = Map.of(
            VOLUME_PERMISSION.permissionString(), VOLUME_PERMISSION,
            UPLOAD_FILEBIN_PERMISSION.permissionString(), UPLOAD_FILEBIN_PERMISSION,
            UPLOAD_SERVERFILE_PERMISSION.permissionString(), UPLOAD_SERVERFILE_PERMISSION,
            UPLOAD_URL_PERMISSION.permissionString(), UPLOAD_URL_PERMISSION,
            UPLOAD_WEB_PERMISSION.permissionString(), UPLOAD_WEB_PERMISSION,
            APPLY_PERMISSION.permissionString(), APPLY_PERMISSION,
            RENAME_PERMISSION.permissionString(), RENAME_PERMISSION,
            PLAY_COMMAND_PERMISSION.permissionString(), PLAY_COMMAND_PERMISSION
    );

    @Override
    public boolean hasPermission(CommandSourceStack stack, String permissionString) {
        Permission permission = PERMISSIONS.get(permissionString);
        if (permission == null) {
            return false;
        }
        if (stack.isPlayer()) {
            return permission.hasPermission(stack.getPlayer());
        }
        return stack.permissions().hasPermission(Permissions.COMMANDS_MODERATOR);
    }

    private static class Permission {

        private final Identifier permission;
        private final PermissionLevel level;

        public Permission(String permission, PermissionLevel level) {
            this.permission = Identifier.parse(permission);
            this.level = level;
        }

        public boolean hasPermission(ServerPlayer player) {
            return player.checkPermission(permission, level);
        }

        public String permissionString() {
            return permission.toString();
        }
    }

}

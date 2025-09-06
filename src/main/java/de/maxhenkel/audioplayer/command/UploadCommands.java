package de.maxhenkel.audioplayer.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.maxhenkel.admiral.annotations.Command;
import de.maxhenkel.admiral.annotations.Name;
import de.maxhenkel.admiral.annotations.RequiresPermission;
import de.maxhenkel.audioplayer.audioloader.AudioStorageManager;
import de.maxhenkel.audioplayer.audioloader.importer.FilebinImporter;
import de.maxhenkel.audioplayer.audioloader.importer.ServerfileImporter;
import de.maxhenkel.audioplayer.audioloader.importer.UrlImporter;
import de.maxhenkel.audioplayer.lang.Lang;
import de.maxhenkel.audioplayer.permission.AudioPlayerPermissionManager;
import de.maxhenkel.audioplayer.webserver.WebServer;
import de.maxhenkel.audioplayer.webserver.WebServerEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.*;

import java.net.URI;
import java.util.UUID;

@Command("audioplayer")
@RequiresPermission(AudioPlayerPermissionManager.UPLOAD_PERMISSION_STRING)
public class UploadCommands {

    @Command
    public void audioPlayer(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() ->
                        Lang.translatable("audioplayer.upload_filebin")
                                .append(" ")
                                .append(Lang.translatable("audioplayer.here").withStyle(style -> {
                                    return style
                                            .withClickEvent(new ClickEvent.RunCommand("/audioplayer filebin"))
                                            .withHoverEvent(new HoverEvent.ShowText(Lang.translatable("audioplayer.click_show_more")));
                                }).withStyle(ChatFormatting.GREEN))
                                .append(".")
                , false);
        context.getSource().sendSuccess(() ->
                        Lang.translatable("audioplayer.upload_serverfile")
                                .append(" ")
                                .append(Lang.translatable("audioplayer.here").withStyle(style -> {
                                    return style
                                            .withClickEvent(new ClickEvent.RunCommand("/audioplayer serverfile"))
                                            .withHoverEvent(new HoverEvent.ShowText(Lang.translatable("audioplayer.click_show_more")));
                                }).withStyle(ChatFormatting.GREEN))
                                .append(".")
                , false);
        context.getSource().sendSuccess(() ->
                        Lang.translatable("audioplayer.upload_url")
                                .append(" ")
                                .append(Lang.translatable("audioplayer.here").withStyle(style -> {
                                    return style
                                            .withClickEvent(new ClickEvent.RunCommand("/audioplayer url"))
                                            .withHoverEvent(new HoverEvent.ShowText(Lang.translatable("audioplayer.click_show_more")));
                                }).withStyle(ChatFormatting.GREEN))
                                .append(".")
                , false);
    }

    @Command("filebin")
    public void filebin(CommandContext<CommandSourceStack> context) {
        FilebinImporter.sendFilebinUploadMessage(context.getSource());
    }

    @Command("filebin")
    public void filebinUpload(CommandContext<CommandSourceStack> context, @Name("id") UUID sound) {
        context.getSource().sendSuccess(() -> Lang.translatable("audioplayer.downloading_sound"), false);
        AudioStorageManager.instance().handleImport(new FilebinImporter(sound), context.getSource().getPlayer());
    }

    @Command("url")
    public void url(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() ->
                        Lang.translatable("audioplayer.url_command",
                                Component.literal(".mp3").withStyle(ChatFormatting.GRAY),
                                Component.literal(".wav").withStyle(ChatFormatting.GRAY),
                                Component.literal("/audioplayer url <link-to-your-file>").withStyle(ChatFormatting.GRAY).withStyle(style -> {
                                    return style
                                            .withClickEvent(new ClickEvent.SuggestCommand("/audioplayer url "))
                                            .withHoverEvent(new HoverEvent.ShowText(Lang.translatable("audioplayer.click_fill_command")));
                                })
                        )
                , false);
    }

    @Command("url")
    public void urlUpload(CommandContext<CommandSourceStack> context, @Name("url") String url) {
        context.getSource().sendSuccess(() -> Lang.translatable("audioplayer.downloading_sound"), false);
        AudioStorageManager.instance().handleImport(new UrlImporter(url), context.getSource().getPlayer());
    }

    @Command("web")
    public void web(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        WebServer webServer = WebServerEvents.getWebServer();
        if (webServer == null) {
            context.getSource().sendFailure(Lang.translatable("audioplayer.webserver_not_running"));
            return;
        }

        UUID token = webServer.getTokenManager().generateToken(context.getSource().getPlayerOrException().getUUID());

        URI uploadUrl = WebServer.generateUploadUrl(token);

        if (uploadUrl != null) {
            context.getSource().sendSuccess(() ->
                            Lang.translatable("audioplayer.click_upload",
                                    Lang.translatable("audioplayer.here").withStyle(ChatFormatting.GREEN, ChatFormatting.UNDERLINE).withStyle(style -> {
                                        return style
                                                .withClickEvent(new ClickEvent.OpenUrl(uploadUrl))
                                                .withHoverEvent(new HoverEvent.ShowText(Lang.translatable("audioplayer.click_open")));
                                    })
                            )
                    , false);
            return;
        }

        context.getSource().sendSuccess(() ->
                        Lang.translatable("audioplayer.visit_website",
                                Lang.translatable("audioplayer.this_token").withStyle(ChatFormatting.GREEN).withStyle(style -> {
                                    return style
                                            .withClickEvent(new ClickEvent.CopyToClipboard(token.toString()))
                                            .withHoverEvent(new HoverEvent.ShowText(Lang.translatable("audioplayer.click_copy")));
                                })
                        )
                , false);
    }

    @Command("serverfile")
    public void serverFile(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() ->
                        Lang.translatable("audioplayer.upload_serverfile_instructions",
                                Component.literal(".mp3").withStyle(ChatFormatting.GRAY),
                                Component.literal(".wav").withStyle(ChatFormatting.GRAY),
                                Component.literal(AudioStorageManager.getUploadFolder().toAbsolutePath().toString()).withStyle(ChatFormatting.GRAY),
                                Component.literal("/audioplayer serverfile \"yourfile.mp3\"").withStyle(ChatFormatting.GRAY).withStyle(style -> {
                                    return style
                                            .withClickEvent(new ClickEvent.SuggestCommand("/audioplayer serverfile "))
                                            .withHoverEvent(new HoverEvent.ShowText(Lang.translatable("audioplayer.click_fill_command")));
                                })
                        )
                , false);
    }

    @Command("serverfile")
    public void serverFileUpload(CommandContext<CommandSourceStack> context, @Name("filename") ServerFileArgument serverFile) {
        AudioStorageManager.instance().handleImport(new ServerfileImporter(serverFile.getFileName()), context.getSource().getPlayer());
    }

}

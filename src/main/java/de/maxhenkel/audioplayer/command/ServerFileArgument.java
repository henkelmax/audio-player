package de.maxhenkel.audioplayer.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.maxhenkel.admiral.argumenttype.ArgumentTypeConverter;
import de.maxhenkel.admiral.argumenttype.ArgumentTypeSupplier;
import de.maxhenkel.audioplayer.AudioPlayerMod;
import de.maxhenkel.audioplayer.audioloader.AudioStorageManager;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class ServerFileArgument {

    private final String fileName;

    public ServerFileArgument(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public static class ServerFileArgumentSupplier implements ArgumentTypeSupplier<CommandSourceStack, ServerFileArgument, String> {

        @Override
        public ArgumentType<String> get() {
            return StringArgumentType.string();
        }

        @Override
        public SuggestionProvider<CommandSourceStack> getSuggestionProvider() {
            return new ServerFileSuggestionProvider();
        }
    }

    public static class ServerFileArgumentTypeConverter implements ArgumentTypeConverter<CommandSourceStack, String, ServerFileArgument> {

        @Override
        @Nullable
        public ServerFileArgument convert(CommandContext<CommandSourceStack> commandContext, String s) throws CommandSyntaxException {
            return new ServerFileArgument(s);
        }
    }

    public static final class ServerFileSuggestionProvider implements SuggestionProvider<CommandSourceStack> {

        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
            Path uploadFolder = AudioStorageManager.getUploadFolder();
            try (Stream<Path> uploadFiles = Files.list(uploadFolder)) {
                uploadFiles.forEach(path -> builder.suggest(StringArgumentType.escapeIfRequired(path.getFileName().toString())));
            } catch (IOException e) {
                AudioPlayerMod.LOGGER.error("Failed to list upload folder", e);
                return builder.buildFuture();
            }
            return builder.buildFuture();
        }
    }

}

package pl.xnik3e.Guardian.Listeners;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.Components.Command.CommandManager;

import java.util.Objects;
import java.util.Optional;

@Component
public class SlashCommandInteractionListener extends ListenerAdapter {

    private final CommandManager manager;
    private final MessageUtils messageUtils;

    @Autowired
    public SlashCommandInteractionListener(CommandManager manager, MessageUtils messageUtils) {
        this.manager = manager;
        this.messageUtils = messageUtils;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if(!messageUtils.checkAuthority(event.getMember()))
            return;

        event.deferReply().setEphemeral(true).queue();
        StringBuilder command = new StringBuilder();
        String commandName = event.getName();
        command.append(commandName);
        switch (commandName) {
            case "help":
                Optional<OptionMapping> optionHelp = Optional.ofNullable(event.getOption("command"));
                if (optionHelp.isPresent()) {
                    String argument = optionHelp.get().getAsString();
                    command.append(" ")
                            .append(argument);
                }
                manager.handle(event, command.toString());
                break;
            case "init":
                Optional<OptionMapping> optionInit = Optional.ofNullable(event.getOption("option"));
                if (optionInit.isPresent()) {
                    String argument = optionInit.get().getAsString();
                    command.append(" ")
                            .append(argument);
                }
                manager.handle(event, command.toString());
                break;
            case "prefix":
                Optional<OptionMapping> optionPrefix = Optional.ofNullable(event.getOption("prefix"));
                if (optionPrefix.isPresent()) {
                    String argument = optionPrefix.get().getAsString();
                    command.append(" ")
                            .append(argument);
                }
                manager.handle(event, command.toString());
                break;
            case "fetch":
            case "purge":
                Optional<OptionMapping> optionPurge = Optional.ofNullable(event.getOption("role"));
                if (optionPurge.isPresent()) {
                    String argument = optionPurge.get().getAsRole().getId();
                    command.append(" ")
                            .append(argument);
                }
                manager.handle(event, command.toString());
                break;
            case "whitelist":
                command.append(" ")
                        .append(Objects.requireNonNull(event.getOption("user")).getAsString());
                manager.handle(event, command.toString());
                break;
            case "blacklist":
                command.append(" ")
                        .append(Objects.requireNonNull(event.getOption("user")).getAsString())
                        .append(" ")
                        .append(Objects.requireNonNull(event.getOption("id")).getAsString());
                manager.handle(event, command.toString());
                break;
            case "bobify":
                command.append(" ")
                        .append(Objects.requireNonNull(event.getOption("id")).getAsString());
                manager.handle(event, command.toString());
                break;
            case "mention":
            case "tbr":
            case "dt":
            case "reset":
            case "getbob":
            case "curse":
                manager.handle(event, command.toString());
                break;
            default:
                event.getHook()
                        .sendMessage("Command not found")
                        .setEphemeral(true)
                        .queue();

        }
    }
}

package pl.xnik3e.Guardian.listeners;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.xnik3e.Guardian.components.Command.CommandManager;

import java.util.Optional;

@Component
public class SlashCommandInteractionListener extends ListenerAdapter {

    private final CommandManager manager;

    @Autowired
    public SlashCommandInteractionListener(CommandManager manager) {
        this.manager = manager;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        event.deferReply().setEphemeral(true).queue();
        String commandName = event.getName();
        StringBuilder command = new StringBuilder();
        switch (commandName) {
            case "help":
                command.append("help");
                Optional<OptionMapping> optionHelp = Optional.ofNullable(event.getOption("command"));
                if (optionHelp.isPresent()) {
                    String argument = optionHelp.get().getAsString();
                    command.append(" ").append(argument);
                }
                manager.handle(event, command.toString());
                break;
            case "init":
                command.append("init");
                Optional<OptionMapping> optionInit = Optional.ofNullable(event.getOption("option"));
                if (optionInit.isPresent()) {
                    String argument = optionInit.get().getAsString();
                    command.append(" ").append(argument);
                }
                manager.handle(event, command.toString());
                break;
            case "prefix":
                command.append("prefix");
                Optional<OptionMapping> optionPrefix = Optional.ofNullable(event.getOption("prefix"));
                if (optionPrefix.isPresent()) {
                    String argument = optionPrefix.get().getAsString();
                    command.append(" ").append(argument);
                }
                manager.handle(event, command.toString());
                break;
            case "mention":
                command.append("mention");
                manager.handle(event, command.toString());
                break;
            case "fetch":
                command.append("fetch");
                Optional<OptionMapping> optionFetch = Optional.ofNullable(event.getOption("role"));
                if (optionFetch.isPresent()) {
                    String argument = optionFetch.get().getAsRole().getId();
                    command.append(" ").append(argument);
                }
                manager.handle(event, command.toString());
                break;
            case "purge":
                command.append("purge");
                Optional<OptionMapping> optionPurge = Optional.ofNullable(event.getOption("role"));
                if (optionPurge.isPresent()) {
                    String argument = optionPurge.get().getAsRole().getId();
                    command.append(" ").append(argument);
                }
                manager.handle(event, command.toString());
                break;
            case "tbr":
                command.append("tbr");
                manager.handle(event, command.toString());
                break;
            case "dt":
                command.append("dt");
                manager.handle(event, command.toString());
                break;
            case "reset":
                command.append("reset");
                manager.handle(event, command.toString());
                break;
            case "whitelist":
                command.append("whitelist");
                Optional<OptionMapping> optionWhitelist = Optional.of(event.getOption("user"));
                if(optionWhitelist.isPresent()){
                    String argument = optionWhitelist.get().getAsString();
                    command.append(" ").append(argument);
                }
                manager.handle(event, command.toString());
                break;
            case "blacklist":
                command.append("blacklist");
                Optional<OptionMapping> optionBlacklistUser = Optional.of(event.getOption("user"));
                Optional<OptionMapping> optionBlacklistId = Optional.of(event.getOption("id"));
                if(optionBlacklistUser.isPresent() && optionBlacklistId.isPresent()){
                    String argumentUser = optionBlacklistUser.get().getAsString();
                    String argumentId = optionBlacklistId.get().getAsString();
                    command.append(" ").append(argumentUser).append(" ").append(argumentId);
                }
                manager.handle(event, command.toString());
                break;
            default:
                event.getHook().sendMessage("Command not found").setEphemeral(true).queue();

        }
    }
}

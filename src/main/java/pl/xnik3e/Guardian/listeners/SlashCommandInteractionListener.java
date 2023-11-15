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
        switch(commandName){
            case "help":
                command.append("help");
                Optional<OptionMapping> optionHelp = Optional.ofNullable(event.getOption("command"));
                if(optionHelp.isPresent()){
                    String argument = optionHelp.get().getAsString();
                    command.append(" ").append(argument);
                }
                manager.handle(event, command.toString());
                break;
            case "init":
                command.append("init");
                Optional<OptionMapping> optionInit = Optional.ofNullable(event.getOption("option"));
                if(optionInit.isPresent()){
                    String argument = optionInit.get().getAsString();
                    command.append(" ").append(argument);
                }
                manager.handle(event, command.toString());
                break;
            case "prefix":
                command.append("prefix");
                Optional<OptionMapping> optionPrefix = Optional.ofNullable(event.getOption("prefix"));
                if(optionPrefix.isPresent()){
                    String argument = optionPrefix.get().getAsString();
                    command.append(" ").append(argument);
                }
                manager.handle(event, command.toString());
                break;
                default:
                event.getHook().sendMessage("Command not found").setEphemeral(true).queue();

        }
    }
}

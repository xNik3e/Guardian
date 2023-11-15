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
        switch(commandName){
            case "help":
                StringBuilder command = new StringBuilder();
                command.append("help");
                Optional<OptionMapping> option = Optional.ofNullable(event.getOption("command"));
                if(option.isPresent()){
                    String argument = option.get().getAsString();
                    command.append(" ").append(argument);
                }
                manager.handle(event, command.toString());
                break;
            default:
                event.getHook().sendMessage("Command not found").setEphemeral(true).queue();

        }
    }
}

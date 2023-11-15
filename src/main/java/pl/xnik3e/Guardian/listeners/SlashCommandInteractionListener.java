package pl.xnik3e.Guardian.listeners;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import pl.xnik3e.Guardian.components.Command.CommandManager;

import java.util.Optional;

public class SlashCommandInteractionListener extends ListenerAdapter {

    private final CommandManager manager;

    public SlashCommandInteractionListener(CommandManager manager) {
        this.manager = manager;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        event.deferReply().queue();
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
                event.reply("Command not found").setEphemeral(true).queue();

        }
    }
}

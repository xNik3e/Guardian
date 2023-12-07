package pl.xnik3e.Guardian.Components.Command;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.List;

public interface ICommand {
    void handle(CommandContext ctx);
    void handleSlash(SlashCommandInteractionEvent event, List<String> args);
    String getName();
    MessageEmbed getHelp();
    String getDescription();
    String getTitle();
    default boolean isAfterInit(){return true;}

    default List<String> getAliases(){
        return List.of();
    }
}

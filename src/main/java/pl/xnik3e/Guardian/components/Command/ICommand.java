package pl.xnik3e.Guardian.components.Command;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.List;

public interface ICommand {
    void handle(CommandContext ctx);
    String getName();
    MessageEmbed getHelp();
    String getDescription();
    String getTitle();
    default boolean isAfterInit(){return true;}

    default List<String> getAliases(){
        return List.of();
    }
}

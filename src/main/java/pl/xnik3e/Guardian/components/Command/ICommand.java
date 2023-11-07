package pl.xnik3e.Guardian.components.Command;
import java.util.List;

public interface ICommand {
    void handle(CommandContext ctx);
    String getName();
    String getHelp();
    default boolean isAfterInit(){return true;}

    default List<String> getAliases(){
        return List.of();
    }
}

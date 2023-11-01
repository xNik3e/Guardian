package pl.xnik3e.Guardian.components.Command;

import lombok.Getter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.Commands.TestCommand;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Getter
@Component
public class CommandManager {
    private final MessageUtils messageUtils;
    private final List<ICommand> commands = new ArrayList<>();

    @Autowired
    public CommandManager(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        addCommand(new TestCommand());
    }

    private void addCommand(ICommand cmd){
        boolean nameFound = this.commands.stream().anyMatch(it -> it.getName().equalsIgnoreCase(cmd.getName()));
        if(nameFound){
            throw new IllegalArgumentException("Command with this name is already present");
        }
        commands.add(cmd);
    }


    @Nullable
    public ICommand getCommand(String search){
        String searchLower = search.toLowerCase();
        for(ICommand cmd : this.commands){
            if(cmd.getName().equals(searchLower) || cmd.getAliases().contains(searchLower)){
                return cmd;
            }
        }
        return null;
    }

    public void handle(MessageReceivedEvent event){
        String[] split = messageUtils.rawCommandContent(event)
                .split("\\s+");
        String invoke = split[0].toLowerCase();
        ICommand cmd = this.getCommand(invoke);

        if(cmd != null){
            event.getChannel().sendTyping().queue();
            List<String> args = Arrays.asList(split).subList(1, split.length);
            CommandContext ctx = new CommandContext(event, args);
            cmd.handle(ctx);
        }
    }
}

package pl.xnik3e.Guardian.components.Command.Commands;

import org.springframework.stereotype.Component;
import pl.xnik3e.Guardian.components.Command.CommandContext;
import pl.xnik3e.Guardian.components.Command.ICommand;

@Component
public class TestCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) {
        ctx.getChannel().sendMessage("Test").queue();
    }

    @Override
    public String getName() {
        return "test";
    }

    @Override
    public String getHelp() {
        return "Just a test command";
    }
}

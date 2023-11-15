package pl.xnik3e.Guardian.components.Command;

import lombok.Getter;
import me.duncte123.botcommons.commands.ICommandContext;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

@Getter
public class CommandContext implements ICommandContext {
    private final MessageReceivedEvent event;
    private final List<String> args;
    private final boolean isSlash;

    public CommandContext(MessageReceivedEvent event, List<String> args, boolean isSlash) {
        this.event = event;
        this.args = args;
        this.isSlash = isSlash;
    }
}

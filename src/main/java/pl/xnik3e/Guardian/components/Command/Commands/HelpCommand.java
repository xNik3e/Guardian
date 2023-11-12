package pl.xnik3e.Guardian.components.Command.Commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.CommandContext;
import pl.xnik3e.Guardian.components.Command.CommandManager;
import pl.xnik3e.Guardian.components.Command.ICommand;

import java.util.List;

public class HelpCommand implements ICommand {

    private final CommandManager commandManager;
    private final MessageUtils messageUtils;

    public HelpCommand(CommandManager commandManager, MessageUtils messageUtils) {
        this.commandManager = commandManager;
        this.messageUtils = messageUtils;
    }

    @Override
    public void handle(CommandContext ctx) {
        List<String> args = ctx.getArgs();
        if(args.isEmpty()){
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle("Commands");

            commandManager.getCommands().stream()
                    .forEach((it) -> {
                        builder.addField(it.getName(),
                                it.getHelp().getDescription() + "\n", false);
                    });
            messageUtils.openPrivateChannelAndMessageUser(ctx.getMember().getUser(), builder.build());
            return;
        }
        String search = args.get(0);
        ICommand command = commandManager.getCommand(search);
        if(command == null){
            messageUtils.openPrivateChannelAndMessageUser(ctx.getMember().getUser(), "Wrong command");
            return;
        }
        messageUtils.openPrivateChannelAndMessageUser(ctx.getMember().getUser(), command.getHelp());
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Help");
        builder.setDescription("Send list of commands or help for specific command\n" +
                "Usage: `{prefix or mention} help [command]`\n" +
                "Available aliases: `h`");
        return builder.build();
    }

    @Override
    public boolean isAfterInit() {
        return false;
    }

    @Override
    public List<String> getAliases() {
        return List.of("h");
    }
}

package pl.xnik3e.Guardian.components.Command.Commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.CommandContext;
import pl.xnik3e.Guardian.components.Command.CommandManager;
import pl.xnik3e.Guardian.components.Command.ICommand;

import java.awt.*;
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
        ctx.getMessage().delete().queue();
        List<String> args = ctx.getArgs();
        if (args.isEmpty()) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle("Commands");
            Color color = new Color((int) (Math.random() * 0x1000000));
            builder.setColor(color);

            commandManager.getCommands().stream()
                    .forEach((it) -> {
                        StringBuilder aliases = new StringBuilder();
                        it.getAliases()
                                .stream()
                                .map((alias) -> "`" + alias + "`")
                                .forEachOrdered(
                                        alias -> aliases.append(alias).append(" ")
                                );
                        builder.addField(it.getName(),
                                it.getDescription() + "\n"
                                        + "Aliases: " + aliases.toString(), false);
                    });
            messageUtils.respondToUser(ctx, builder.build());
            return;
        }
        String search = args.get(0);
        ICommand command = commandManager.getCommand(search);
        if (command == null) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle("Command not found");
            builder.setDescription("Command `" + search + "` not found");
            builder.setColor(Color.RED);
            messageUtils.respondToUser(ctx, builder.build());
            return;
        }
        messageUtils.respondToUser(ctx, command.getHelp());
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(getTitle());
        builder.setDescription(getDescription());
        builder.addField("Usage", "`{prefix or mention} help <command>`", false);
        builder.addField("Available aliases", "`h`", false);
        Color color = new Color((int) (Math.random() * 0x1000000));
        builder.setColor(color);
        return builder.build();
    }

    @Override
    public String getDescription() {
        return "Send list of commands or help for specific command";
    }

    @Override
    public String getTitle() {
        return "Help";
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

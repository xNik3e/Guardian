package pl.xnik3e.Guardian.components.Command.Commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.Channel;
import pl.xnik3e.Guardian.Models.ConfigModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.CommandContext;
import pl.xnik3e.Guardian.components.Command.ICommand;

import java.awt.*;
import java.util.List;

public class InitCommand implements ICommand {

    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;
    private final ConfigModel model;

    public InitCommand(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
        this.model = fireStoreService.getModel();
    }

    @Override
    public void handle(CommandContext ctx) {
        ctx.getMessage().delete().queue();
        Guild guild = ctx.getGuild();
        Channel channel = ctx.getChannel();
        List<String> args = ctx.getArgs();
        EmbedBuilder embedBuilder = new EmbedBuilder();
        if (args.size() == 1) {
            String arg = args.get(0);
            switch (arg) {
                case "ban":
                    String previousChannel = model.getChannelIdToSendDeletedMessages();
                    model.setChannelIdToSendDeletedMessages(channel.getId());
                    if (previousChannel.isEmpty()) {
                        embedBuilder.setTitle("Ban channel set");
                        embedBuilder.setDescription("Setting up the ban utility for channel: **"
                                + channel.getName() +"**");
                        embedBuilder.setColor(Color.GREEN);
                        messageUtils.respondToUser(ctx, embedBuilder.build());
                    } else {
                        embedBuilder.setTitle("Ban channel changed");
                        embedBuilder.setDescription("Setting up the ban utility for channel: **"
                                + channel.getName() + "**"
                                + "\nPrevious channel was: **" + guild.getChannelById(Channel.class, previousChannel).getName() +"**");
                        embedBuilder.setColor(Color.GREEN);
                        messageUtils.respondToUser(ctx, embedBuilder.build());
                    }
                    model.setInit(true);
                    fireStoreService.updateConfigModel();
                    break;
                case "log":
                    String previousLogChannel = model.getChannelIdToSendLog();
                    model.setChannelIdToSendLog(channel.getId());
                    if (previousLogChannel.isEmpty()) {
                        embedBuilder.setTitle("Log channel set");
                        embedBuilder.setDescription("Setting up the log utility for channel: **"
                                + channel.getName() +"**");
                        embedBuilder.setColor(Color.GREEN);
                        messageUtils.respondToUser(ctx, embedBuilder.build());
                    } else {
                        embedBuilder.setTitle("Log channel changed");
                        embedBuilder.setDescription("Setting up the log utility for channel: **"
                                + channel.getName() + "**"
                                + "\nPrevious channel was: **" + guild.getChannelById(Channel.class, previousLogChannel).getName() +"**");
                        embedBuilder.setColor(Color.GREEN);
                        messageUtils.respondToUser(ctx, embedBuilder.build());
                    }
                    fireStoreService.updateConfigModel();
                    break;
                default:
                    embedBuilder.setTitle("Error");
                    embedBuilder.setDescription("Invalid argument");
                    embedBuilder.addField("Valid arguments", "`ban`, `log`", false);
                    embedBuilder.setColor(Color.RED);
                    messageUtils.respondToUser(ctx, embedBuilder.build());
                    break;
            }
        } else {
            embedBuilder.setTitle("Hey! Let's get started!");
            embedBuilder.setDescription("I will guide you through the setup process");
            StringBuilder banMessage = new StringBuilder();
            banMessage.append("In order to set up a ban command, go to desired channel").append(
                   model.isRespondByPrefix() ?
                            " and type " +model.getPrefix() + "init ban" :
                            ", mention me and type init ban"
            ).append("\nBan command is **required**.");
            embedBuilder.addField("Ban command", banMessage.toString(), false);
            StringBuilder logMessage = new StringBuilder();
            logMessage.append("In order to set up a log command, go to desired channel").append(
                    model.isRespondByPrefix() ?
                            " and type " + model.getPrefix() + "init log" :
                            ", mention me and type init log"
            ).append("\nLog command is **optional**.");
            embedBuilder.addField("Log command", logMessage.toString(), false);
            embedBuilder.addField("Aliases", "You can always use a command aliases listed in help command", false);
            embedBuilder.setColor((int)(Math.random() * 0x1000000));
            messageUtils.respondToUser(ctx, embedBuilder.build());
        }
    }

    @Override
    public String getName() {
        return "init";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(getTitle());
        embedBuilder.setDescription(getDescription());
        embedBuilder.addField("Optional arguments", "`ban`, `log`", false);
        embedBuilder.addField("Usage", "`{prefix or mention} init {optional <ban or log>}`", false);
        embedBuilder.addField("Available aliases", "`i`, `setup`", false);
        Color color = new Color((int)(Math.random() * 0x1000000));
        embedBuilder.setColor(color);
        return embedBuilder.build();
    }

    @Override
    public String getDescription() {
        return "Required to initialize the bot\nYou need to be in desired channel to set it as ban or log channel";
    }

    @Override
    public String getTitle() {
        return "Init command";
    }

    @Override
    public List<String> getAliases() {
        return List.of("i", "setup");
    }

    @Override
    public boolean isAfterInit() {
        return false;
    }
}

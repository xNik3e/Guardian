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
        if (args.size() == 1) {
            String arg = args.get(0);
            switch (arg) {
                case "ban":
                    String previousChannel = model.getChannelIdToSendDeletedMessages();
                    model.setChannelIdToSendDeletedMessages(channel.getId());
                    if (previousChannel.isEmpty()) {
                        messageUtils.openPrivateChannelAndMessageUser(ctx.getMember().getUser(), "Setting up the ban utility for channel: **"
                                + channel.getName() +"**");
                    } else {
                        messageUtils.openPrivateChannelAndMessageUser(ctx.getMember().getUser(), "Setting up the ban utility for channel: **"
                                + channel.getName()
                                + "**"
                                + "\nPrevious channel was: **" + guild.getChannelById(Channel.class, previousChannel).getName() +"**");
                    }
                    model.setInit(true);
                    fireStoreService.updateConfigModel();
                    break;
                case "log":
                    String previousLogChannel = model.getChannelIdToSendLog();
                    model.setChannelIdToSendLog(channel.getId());
                    if (previousLogChannel.isEmpty()) {
                        messageUtils.openPrivateChannelAndMessageUser(ctx.getMember().getUser(), "Setting up the log utility for channel: **"
                                + channel.getName() +"**");
                    } else {
                        messageUtils.openPrivateChannelAndMessageUser(ctx.getMember().getUser(), "Setting up the log utility for channel: **"
                                + channel.getName() + "**"
                                + "\nPrevious channel was: **" + guild.getChannelById(Channel.class, previousLogChannel).getName() + "**");
                    }
                    fireStoreService.updateConfigModel();
                    break;
                default:
                    messageUtils.openPrivateChannelAndMessageUser(ctx.getMember().getUser(), "Invalid argument");
                    messageUtils.openPrivateChannelAndMessageUser(ctx.getMember().getUser(), "Valid arguments: ban, log");
                    break;
            }
        } else {
            messageUtils.openPrivateChannelAndMessageUser(ctx.getMember().getUser(), "Hey! Let's get started!");
            StringBuilder banMessage = new StringBuilder();
            banMessage.append("In order to set up a ban command, go to desired channel").append(
                   model.isRespondByPrefix() ?
                            " and type " +model.getPrefix() + "init ban" :
                            ", mention me and type init ban"
            ).append("\nBan command is **required**.");
            messageUtils.openPrivateChannelAndMessageUser(ctx.getMember().getUser(), banMessage.toString());
            StringBuilder logMessage = new StringBuilder();
            logMessage.append("In order to set up a log command, go to desired channel").append(
                    model.isRespondByPrefix() ?
                            " and type " + model.getPrefix() + "init log" :
                            ", mention me and type init log"
            ).append("\nLog command is **optional**.");
            messageUtils.openPrivateChannelAndMessageUser(ctx.getMember().getUser(), logMessage.toString());
            messageUtils.openPrivateChannelAndMessageUser(ctx.getMember().getUser(), "You can always use a command aliases listed in help command");
        }
    }

    @Override
    public String getName() {
        return "init";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Init command");
        embedBuilder.setDescription("Required to initialize the bot\nYou need to be in desired channel to set it as ban or log channel");
        embedBuilder.addField("Optional arguments", "`ban`, `log`", false);
        embedBuilder.addField("Usage", "```{prefix or mention} init {optional <ban or log>}```", false);
        embedBuilder.addField("Available aliases", "`i`, `setup`", false);
        return embedBuilder.build();
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

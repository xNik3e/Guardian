package pl.xnik3e.Guardian.components.Command.Commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
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
        boolean deleteTriggerMessage = fireStoreService.getModel().isDeleteTriggerMessage();
        if(deleteTriggerMessage)
            ctx.getMessage().delete().queue();
        Guild guild = ctx.getGuild();
        Channel channel = ctx.getChannel();
        List<String> args = ctx.getArgs();
        configInit(ctx, args, channel, guild, null);
    }

    @Override
    public void handleSlash(SlashCommandInteractionEvent event, List<String> args) {
        Guild guild = event.getGuild();
        Channel channel = event.getChannel();
        configInit(null, args, channel, guild, event);
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
        embedBuilder.addField("Optional arguments", "`ban`, `log`, 'echolog'", false);
        embedBuilder.addField("Usage", "`{prefix or mention} init {optional <ban, log or echolog>}`", false);
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

    private void configInit(CommandContext ctx, List<String> args, Channel channel, Guild guild,SlashCommandInteractionEvent event) {
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
                        replyToUser(ctx, event, embedBuilder);
                    } else {
                        embedBuilder.setTitle("Ban channel changed");
                        embedBuilder.setDescription("Setting up the ban utility for channel: **"
                                + channel.getName() + "**"
                                + "\nPrevious channel was: **" + guild.getChannelById(Channel.class, previousChannel).getName() +"**");
                        embedBuilder.setColor(Color.GREEN);
                        replyToUser(ctx, event, embedBuilder);
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
                        replyToUser(ctx, event, embedBuilder);
                    } else {
                        embedBuilder.setTitle("Log channel changed");
                        embedBuilder.setDescription("Setting up the log utility for channel: **"
                                + channel.getName() + "**"
                                + "\nPrevious channel was: **" + guild.getChannelById(Channel.class, previousLogChannel).getName() +"**");
                        embedBuilder.setColor(Color.GREEN);
                        replyToUser(ctx, event, embedBuilder);
                    }
                    fireStoreService.updateConfigModel();
                    break;
                case "echolog":
                    String previousEchoLogChannel = model.getChannelIdToSendEchoLog();
                    model.setChannelIdToSendEchoLog(channel.getId());
                    if(previousEchoLogChannel.isEmpty()){
                        embedBuilder.setTitle("Echo Log channel set");
                        embedBuilder.setDescription("Setting up the echo log utility for channel: **"
                                + channel.getName() +"**");
                        embedBuilder.setColor(Color.GREEN);
                        replyToUser(ctx, event, embedBuilder);
                    }else{
                        embedBuilder.setTitle("Echo Log channel changed");
                        embedBuilder.setDescription("Setting up the echo log utility for channel: **"
                                + channel.getName() + "**"
                                + "\nPrevious channel was: **" + guild.getChannelById(Channel.class, previousEchoLogChannel).getName() +"**");
                        embedBuilder.setColor(Color.GREEN);
                    }
                    fireStoreService.updateConfigModel();
                    break;
                default:
                    embedBuilder.setTitle("Error");
                    embedBuilder.setDescription("Invalid argument");
                    embedBuilder.addField("Valid arguments", "`ban`, `log`", false);
                    embedBuilder.setColor(Color.RED);
                    replyToUser(ctx, event, embedBuilder);
                    break;
            }
        } else {
            embedBuilder.setTitle("Hey! Let's get started!");
            embedBuilder.setDescription("I will guide you through the setup process");
            StringBuilder banMessage = new StringBuilder();
            banMessage.append("In order to set up a ban command, go to desired channel").append(
                    model.isRespondByPrefix() ?
                            " and type *" +model.getPrefix() + "init ban*" :
                            ", mention me and type *init ban*"
            ).append("\nBan command is **required**.");
            embedBuilder.addField("Ban command", banMessage.toString(), false);
            StringBuilder logMessage = new StringBuilder();
            logMessage.append("In order to set up a log command, go to desired channel").append(
                    model.isRespondByPrefix() ?
                            " and type *" + model.getPrefix() + "init log*" :
                            ", mention me and type *init log*"
            ).append("\nLog command is **optional**.");
            embedBuilder.addField("Log command", logMessage.toString(), false);
            StringBuilder echoLogMessage = new StringBuilder();
            echoLogMessage.append("In order to set up a log command, go to desired channel").append(
                    model.isRespondByPrefix() ?
                            " and type *" + model.getPrefix() + "init log*" :
                            ", mention me and type *init log*"
            ).append("\nLog command is **optional**.");
            embedBuilder.addField("Echo log command", echoLogMessage.toString(), false);
            embedBuilder.addField("Aliases", "You can always use a command aliases listed in help command", false);
            embedBuilder.setColor((int)(Math.random() * 0x1000000));
            replyToUser(ctx, event, embedBuilder);
        }
    }

    private void replyToUser(CommandContext ctx, SlashCommandInteractionEvent event, EmbedBuilder embedBuilder) {
        if(ctx != null)
            messageUtils.respondToUser(ctx, embedBuilder.build());
        else
            event.getHook().sendMessageEmbeds(embedBuilder.build()).setEphemeral(true).queue();
    }
}

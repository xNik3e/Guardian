package pl.xnik3e.Guardian.Components.Command.Commands.ConfigCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import pl.xnik3e.Guardian.Models.ConfigModel;
import pl.xnik3e.Guardian.Models.ContextModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.Components.Command.CommandContext;
import pl.xnik3e.Guardian.Components.Command.ICommand;

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
        messageUtils.deleteTrigger(ctx);
        ContextModel context = new ContextModel(ctx);
        configInit(context);
    }

    @Override
    public void handleSlash(SlashCommandInteractionEvent event, List<String> args) {
        ContextModel context = new ContextModel(event, args);
        configInit(context);
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
        embedBuilder.addField("Optional arguments", "`ban`, `log`, `echolog`", false);
        embedBuilder.addField("Usage", "`{prefix or mention} init {optional <ban, log or echolog>}`", false);
        embedBuilder.addField("Available aliases", messageUtils.createAliasString(getAliases()), false);
        Color color = new Color((int) (Math.random() * 0x1000000));
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

    private void configInit(ContextModel context) {
        if (context.args.size() == 1) {
            String arg = context.args.get(0);
            switch (arg) {
                case "ban":
                    ban(context);
                    break;
                case "log":
                    log(context);
                    break;
                case "echolog":
                    echoLog(context);
                    break;
                default:
                    sendErrorInvalidArgs(context);
                    break;
            }
        } else {
            messageUtils.respondToUser(context.ctx, context.event, createDefaultInitMessage());
        }
    }

    private EmbedBuilder createDefaultInitMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Hey! Let's get started!");
        embedBuilder.setDescription("I will guide you through the setup process");
        addBanMessage(embedBuilder);
        addLogMessage(embedBuilder);
        addEchoLogMessage(embedBuilder);
        embedBuilder.addField("Aliases", "You can always use a command aliases listed in help command", false);
        embedBuilder.setColor((int) (Math.random() * 0x1000000));
        return embedBuilder;
    }

    private void sendErrorInvalidArgs(ContextModel context) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Error");
        embedBuilder.setDescription("Invalid argument");
        embedBuilder.addField("Valid arguments", messageUtils.createAliasString(getAliases()), false);
        embedBuilder.setColor(Color.RED);
        messageUtils.respondToUser(context.ctx, context.event, embedBuilder);
    }

    private void ban(ContextModel context) {
        String previousChannel = model.getChannelIdToSendDeletedMessages();
        model.setChannelIdToSendDeletedMessages(context.channel.getId());
        if (previousChannel.isEmpty()) {
            messageUtils.respondToUser(context.ctx, context.event, getBanEmbedBuilderNoPreviousChannel(context));
        } else {
            messageUtils.respondToUser(context.ctx, context.event, getBanEmbedBuilderWithPreviousChannel(context, previousChannel));
        }
        model.setInit(true);
        fireStoreService.updateConfigModel();
    }

    private void log(ContextModel context) {
        String previousLogChannel = model.getChannelIdToSendLog();
        model.setChannelIdToSendLog(context.channel.getId());
        if (previousLogChannel.isEmpty()) {
            messageUtils.respondToUser(context.ctx, context.event, getLogEmbedBuilderNoPreviousChannel(context));
        } else {
            messageUtils.respondToUser(context.ctx, context.event, getLogEmbedBuilderWithPreviousChannel(context, previousLogChannel));
        }
        fireStoreService.updateConfigModel();
    }

    private void echoLog(ContextModel context) {
        String previousEchoLogChannel = model.getChannelIdToSendEchoLog();
        model.setChannelIdToSendEchoLog(context.channel.getId());
        if (previousEchoLogChannel.isEmpty()) {
            messageUtils.respondToUser(context.ctx, context.event, getEchoLogEmbedBuilderNoPreviousChannel(context));
        } else {
            messageUtils.respondToUser(context.ctx, context.event, getEchoLogEmbedBuilderWithPreviousChannel(context, previousEchoLogChannel));
        }
        fireStoreService.updateConfigModel();
    }

    private void addBanMessage(EmbedBuilder embedBuilder) {
        String banMessage = "In order to set up a ban command, go to desired channel" +
                (model.isRespondByPrefix() ?
                        " and type *" + model.getPrefix() + "init ban*" :
                        ", mention me and type *init ban*") +
                "\nBan command is **required**." +
                "\nThis option will log any ban actions caused by *purge* command";
        embedBuilder.addField("Ban command", banMessage, false);
    }

    private void addLogMessage(EmbedBuilder embedBuilder) {
        String logMessage = "In order to set up a log command, go to desired channel" +
                (model.isRespondByPrefix() ?
                        " and type *" + model.getPrefix() + "init log*" :
                        ", mention me and type *init log*") +
                "\nLog command is **optional**.";
        embedBuilder.addField("Log command", logMessage, false);
    }

    private void addEchoLogMessage(EmbedBuilder embedBuilder) {
        String echoLogMessage = "In order to set up a echo log command, go to desired channel" +
                (model.isRespondByPrefix() ?
                        " and type *" + model.getPrefix() + "init echolog*" :
                        ", mention me and type *init echolog*") +
                "\nLog command is **optional**." +
                "\nEcho log command will send duplicate message to specified channel";
        embedBuilder.addField("Echo log command", echoLogMessage, false);
    }

    private static EmbedBuilder getEchoLogEmbedBuilderWithPreviousChannel(ContextModel context, String previousEchoLogChannel) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Echo Log channel changed");
        embedBuilder.setDescription("Setting up the echo log utility for channel: **"
                + context.channel.getName() + "**"
                + "\nPrevious channel was: **" + context.guild.getChannelById(Channel.class, previousEchoLogChannel).getName() + "**");
        embedBuilder.setColor(Color.GREEN);
        return embedBuilder;
    }

    @NotNull
    private static EmbedBuilder getEchoLogEmbedBuilderNoPreviousChannel(ContextModel context) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Echo Log channel set");
        embedBuilder.setDescription("Setting up the echo log utility for channel: **"
                + context.channel.getName() + "**");
        embedBuilder.setColor(Color.GREEN);
        return embedBuilder;
    }

    @NotNull
    private static EmbedBuilder getLogEmbedBuilderWithPreviousChannel(ContextModel context, String previousLogChannel) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Log channel changed");
        embedBuilder.setDescription("Setting up the log utility for channel: **"
                + context.channel.getName() + "**"
                + "\nPrevious channel was: **" + context.guild.getChannelById(Channel.class, previousLogChannel).getName() + "**");
        embedBuilder.setColor(Color.GREEN);
        return embedBuilder;
    }

    @NotNull
    private static EmbedBuilder getLogEmbedBuilderNoPreviousChannel(ContextModel context) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Log channel set");
        embedBuilder.setDescription("Setting up the log utility for channel: **"
                + context.channel.getName() + "**");
        embedBuilder.setColor(Color.GREEN);
        return embedBuilder;
    }

    @NotNull
    private static EmbedBuilder getBanEmbedBuilderWithPreviousChannel(ContextModel context, String previousChannel) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Ban channel changed");
        embedBuilder.setDescription("Setting up the ban utility for channel: **"
                + context.channel.getName() + "**"
                + "\nPrevious channel was: **" + context.guild.getChannelById(Channel.class, previousChannel).getName() + "**");
        embedBuilder.setColor(Color.GREEN);
        return embedBuilder;
    }

    @NotNull
    private static EmbedBuilder getBanEmbedBuilderNoPreviousChannel(ContextModel context) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Ban channel set");
        embedBuilder.setDescription("Setting up the ban utility for channel: **"
                + context.channel.getName() + "**");
        embedBuilder.setColor(Color.GREEN);
        return embedBuilder;
    }


}

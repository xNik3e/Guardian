package pl.xnik3e.Guardian.Components.Command.Commands.BobCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import pl.xnik3e.Guardian.Models.ContextModel;
import pl.xnik3e.Guardian.Models.NickNameModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.Components.Command.CommandContext;
import pl.xnik3e.Guardian.Components.Command.ICommand;

import java.awt.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NickBlackListCommand implements ICommand {

    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    public NickBlackListCommand(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
    }

    @Override
    public void handle(CommandContext ctx) {
        messageUtils.deleteTrigger(ctx);
        blackList(new ContextModel(ctx));
    }

    @Override
    public void handleSlash(SlashCommandInteractionEvent event, List<String> args) {
        blackList(new ContextModel(event, args));
    }

    @Override
    public String getName() {
        return "blacklist";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(getTitle());
        builder.setDescription(getDescription());
        builder.addField("Usage",
                "`{prefix or mention} blacklist <user id> <nickname index or **ALL**>`",
                false);
        builder.addField("Example usage",
                "`" + fireStoreService.getModel().getPrefix() + "blacklist 1164645019769131029 1`\n" +
                        "`" + fireStoreService.getModel().getPrefix() + "blacklist @xnik3e ALL`",
                false);
        builder.addField("Available aliases", messageUtils.createAliasString(getAliases()), false);
        Color color = new Color((int) (Math.random() * 0x1000000));
        builder.setColor(color);
        return builder.build();
    }

    @Override
    public String getDescription() {
        return "Blacklist chosen nickname for specific user";
    }

    @Override
    public String getTitle() {
        return "Blacklist nickname";
    }

    @Override
    public boolean isAfterInit() {
        return true;
    }

    @Override
    public List<String> getAliases() {
        return List.of("bl");
    }

    public void blackList(ContextModel context) {
        if (context.args.isEmpty()) {
            sendErrorMessageNoArguments(context);
            return;
        }

        if (context.args.size() != 2) {
            sendErrorMessageInvalidArgumentsSize(context);
            return;
        }

        Matcher matcher = Pattern.compile("\\d+")
                .matcher(context.args.get(0));
        if (!matcher.find()) {
            sendErrorMessageNoValidUserID(context);
            return;
        }
        String userID = matcher.group();

        String nickNameIndex = context.args.get(1);
        if(nickNameIndex.equalsIgnoreCase("all")){
            revokeWhitelistAndNotifyUser(context, userID);
            return;
        }

        matcher = Pattern.compile("\\d+")
                .matcher(nickNameIndex);
        if (!matcher.find()) {
            sendErrorInvalidIndex(context);
            return;
        }
        int index = Integer.parseInt(matcher.group());
        NickNameModel model = fireStoreService.fetchNickNameModel(userID);
        if(model == null){
            sendInfiMessageEmptyWhitelist(context, userID);
            return;
        }

        if(index > model.getNickName().size()){
            sendErrorMessageInvalidIndexRange(context, userID, index);
            return;
        }

        model.getNickName().remove(index - 1);
        if(model.getNickName().isEmpty())
            fireStoreService.deleteNickNameModel(userID);
        else
            fireStoreService.updateNickModel(model);

        context.guild.retrieveMemberById(userID).delay(2, TimeUnit.SECONDS).queue(member -> {
            if(!messageUtils.hasMentionableNickName(member))
                messageUtils.bobify(member);

            sendSuccessMessage(context, index, userID);
        });
    }

    private void sendSuccessMessage(ContextModel context, int index, String userID) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Success");
        builder.setDescription("Deleted nickname with index: " + index + " for user with id: " + userID);
        builder.setColor(Color.GREEN);
        messageUtils.respondToUser(context.ctx, context.event, builder);
    }

    private void sendErrorMessageInvalidIndexRange(ContextModel context, String userID, int index) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Error");
        builder.setDescription("User with id: " + userID + " does not have nickname with index: " + index);
        builder.setColor(Color.RED);
        messageUtils.respondToUser(context.ctx, context.event, builder);
    }

    private void sendInfiMessageEmptyWhitelist(ContextModel context, String userID) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Info");
        builder.setDescription("User with id: " + userID + " does not have any whitelisted nicknames");
        builder.setColor(Color.YELLOW);
        messageUtils.respondToUser(context.ctx, context.event, builder);
    }

    private void sendErrorInvalidIndex(ContextModel context) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Error");
        builder.setDescription("Please provide valid nickname index or type **ALL**");
        builder.setColor(Color.RED);
        messageUtils.respondToUser(context.ctx, context.event, builder);
    }

    private void revokeWhitelistAndNotifyUser(ContextModel context, String userID) {
        fireStoreService.deleteNickNameModel(userID);
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Success");
        builder.setDescription("Deleted all whitelisted nicknames for user with id: " + userID);
        builder.setColor(Color.GREEN);
        context.guild.retrieveMemberById(userID).delay(2, TimeUnit.SECONDS).queue(member -> {
            if(!messageUtils.hasMentionableNickName(member))
                messageUtils.bobify(member);
        });
        messageUtils.respondToUser(context.ctx, context.event, builder);
    }

    private void sendErrorMessageNoValidUserID(ContextModel context) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Error");
        builder.setDescription("Please provide valid user id");
        builder.setColor(Color.RED);
        messageUtils.respondToUser(context.ctx, context.event, builder);
    }

    private void sendErrorMessageInvalidArgumentsSize(ContextModel context) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Error");
        builder.setDescription("Provided " + context.args.size() + " arguments, but expected 2");
        builder.addField("User", "User id or mention", false);
        builder.addField("Nickname index", "Index of nickname to blacklist or **ALL** if you want to delete all whitelisted roles", false);
        builder.setColor(Color.RED);
        messageUtils.respondToUser(context.ctx, context.event, builder);
    }

    private void sendErrorMessageNoArguments(ContextModel context) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Error");
        builder.setDescription("You need to provide user id and nickname index");
        builder.setColor(Color.RED);
        messageUtils.respondToUser(context.ctx, context.event, builder);
    }


}

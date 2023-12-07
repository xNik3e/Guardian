package pl.xnik3e.Guardian.Components.Command.Commands.BobCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BobifyCommand implements ICommand {

    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    public BobifyCommand(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
    }

    @Override
    public void handle(CommandContext ctx) {
        messageUtils.deleteTrigger(ctx);
        bobifyUser(new ContextModel(ctx));
    }

    @Override
    public void handleSlash(SlashCommandInteractionEvent event, List<String> args) {
        bobifyUser(new ContextModel(event, args));
    }

    @Override
    public String getName() {
        return "bobify";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(getTitle());
        embedBuilder.setDescription(getDescription());
        embedBuilder.addField("Usage",
                "`" + fireStoreService.getModel().getPrefix() + "bobify <user id or mention>`", false);
        embedBuilder.addField("Example",
                "`" + fireStoreService.getModel().getPrefix() + "bobify @xnik3e`", false);
        embedBuilder.addField("Requirements", "User must have non-mentionable nickname", false);
        embedBuilder.addField("Aliases", messageUtils.createAliasString(getAliases()), false);
        Color color = new Color((int) (Math.random() * 0x1000000));
        embedBuilder.setColor(color);
        return embedBuilder.build();
    }

    @Override
    public String getDescription() {
        return "Enlarge the Bob army!\nChange user's nickname to *Bob*";
    }

    @Override
    public String getTitle() {
        return "Bobify user";
    }

    @Override
    public boolean isAfterInit() {
        return true;
    }

    @Override
    public List<String> getAliases() {
        return List.of("bob", "b");
    }


    private void bobifyUser(ContextModel context) {
        if (context.args.isEmpty()) {
            sendErrorMessageNoArguments(context);
            return;
        }
        if (context.args.size() != 1) {
            sendErrorMessageToManyArguments(context);
            return;
        }

        Matcher matcher = Pattern.compile("\\d+")
                .matcher(context.args.get(0));
        if (!matcher.find()) {
            sendErrorMessageInvalidID(context);
            return;
        }
        String userID = matcher.group();

        if (userID.equalsIgnoreCase("all")) {
            //UNLEASH THE BEAST
            bobifyAllUsers(context);
        } else {
            bobifySingleUser(context, userID);
        }
    }

    private void bobifySingleUser(ContextModel context, String userID) {
        context.guild.retrieveMemberById(userID).queue(member -> {
            NickNameModel nickNameModel = fireStoreService.fetchNickNameModel(member.getId());
            if(nickNameModel != null) {
                String nickName = member.getEffectiveName();
                nickNameModel.getNickName()
                        .remove(nickName);
                fireStoreService.setNickModel(nickNameModel);
            }
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Success");
            embedBuilder.setDescription("Bobified user " + member.getAsMention());
            embedBuilder.addField("Previous nickname", member.getEffectiveName(), false);
            embedBuilder.setColor(Color.GREEN);
            messageUtils.bobify(member);
            messageUtils.respondToUser(context.ctx, context.event, embedBuilder);
        });
    }

    private void bobifyAllUsers(ContextModel context) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        context.guild.findMembers(member -> !messageUtils.hasMentionableNickName(member))
                .onSuccess(members -> {
                    members.forEach(messageUtils::bobify);
                    embedBuilder.setTitle("Success");
                    embedBuilder.setDescription("Bobified " + members.size() + " users");
                    embedBuilder.setColor(Color.GREEN);
                    messageUtils.respondToUser(context.ctx, context.event, embedBuilder);
                }).onError(error -> {
                    embedBuilder.setTitle("Error");
                    embedBuilder.setDescription("Something went wrong");
                    embedBuilder.setColor(Color.RED);
                    messageUtils.respondToUser(context.ctx, context.event, embedBuilder);
                });
    }

    private void sendErrorMessageInvalidID(ContextModel context) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Error");
        embedBuilder.setDescription("Please provide valid user id");
        embedBuilder.setColor(Color.RED);
        messageUtils.respondToUser(context.ctx, context.event, embedBuilder);
    }

    private void sendErrorMessageToManyArguments(ContextModel context) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Too many arguments");
        embedBuilder.setDescription("You must provide only one argument");
        embedBuilder.setColor(Color.RED);
        messageUtils.respondToUser(context.ctx, context.event, embedBuilder);
    }

    private void sendErrorMessageNoArguments(ContextModel context) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Missing argument");
        embedBuilder.setDescription("You must provide user id or mention");
        embedBuilder.setColor(Color.RED);
        messageUtils.respondToUser(context.ctx, context.event, embedBuilder);
    }


}

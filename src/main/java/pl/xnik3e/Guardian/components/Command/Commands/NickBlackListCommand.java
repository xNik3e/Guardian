package pl.xnik3e.Guardian.components.Command.Commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import pl.xnik3e.Guardian.Models.NickNameModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.CommandContext;
import pl.xnik3e.Guardian.components.Command.ICommand;

import java.awt.*;
import java.util.List;
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
        boolean deleteTriggerMessage = fireStoreService.getModel().isDeleteTriggerMessage();
        if (deleteTriggerMessage)
            ctx.getMessage().delete().queue();
        Guild guild = ctx.getGuild();
        List<String> args = ctx.getArgs();
        blackList(ctx, null, args, guild);
    }

    @Override
    public void handleSlash(SlashCommandInteractionEvent event, List<String> args) {
        blackList(null, event, args, event.getGuild());
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
        builder.addField("Available aliases", "`bl`", false);
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

    public void blackList(CommandContext ctx, SlashCommandInteractionEvent event, List<String> args, Guild guild) {
        EmbedBuilder builder = new EmbedBuilder();
        if (args.isEmpty()) {
            builder.setTitle("Error");
            builder.setDescription("You need to provide user id and nickname index");
            builder.setColor(Color.RED);
            respondToUser(ctx, event, builder);
            return;
        }
        if (args.size() != 2) {
            builder.setTitle("Error");
            builder.setDescription("Provided " + args.size() + " arguments, but expected 2");
            builder.addField("User", "User id or mention", false);
            builder.addField("Nickname index", "Index of nickname to blacklist or **ALL** if you want to delete all whitelisted roles", false);
            builder.setColor(Color.RED);
            respondToUser(ctx, event, builder);
            return;
        }
        Matcher matcher = Pattern.compile("\\d+")
                .matcher(args.get(0));
        if (!matcher.find()) {
            builder.setTitle("Error");
            builder.setDescription("Please provide valid user id");
            builder.setColor(Color.RED);
            respondToUser(ctx, event, builder);
            return;
        }
        String userID = matcher.group();
        String nickNameIndex = args.get(1);
        if(nickNameIndex.toLowerCase().equals("all")){
            fireStoreService.deleteNickNameModel(userID);
            builder.setTitle("Success");
            builder.setDescription("Deleted all whitelisted nicknames for user with id: " + userID);
            builder.setColor(Color.GREEN);
            respondToUser(ctx, event, builder);
            return;
        }
        matcher = Pattern.compile("\\d+")
                .matcher(nickNameIndex);
        if (!matcher.find()) {
            builder.setTitle("Error");
            builder.setDescription("Please provide valid nickname index or type **ALL**");
            builder.setColor(Color.RED);
            respondToUser(ctx, event, builder);
            return;
        }
        int index = Integer.parseInt(matcher.group());
        NickNameModel model = fireStoreService.getNickNameModel(userID);
        if(model == null){
            builder.setTitle("Success");
            builder.setDescription("User with id: " + userID + " does not have any whitelisted nicknames");
            builder.setColor(Color.GREEN);
            respondToUser(ctx, event, builder);
            return;
        }

        if(index > model.getNickName().size()){
            builder.setTitle("Error");
            builder.setDescription("User with id: " + userID + " does not have nickname with index: " + index);
            builder.setColor(Color.RED);
            respondToUser(ctx, event, builder);
            return;
        }

        model.getNickName().remove(index - 1);
        if(model.getNickName().isEmpty())
            fireStoreService.deleteNickNameModel(userID);
        else
            fireStoreService.updateNickModel(model);
        builder.setTitle("Success");
        builder.setDescription("Deleted nickname with index: " + index + " for user with id: " + userID);
        builder.setColor(Color.GREEN);
        respondToUser(ctx, event, builder);
    }


    private void respondToUser(CommandContext ctx, SlashCommandInteractionEvent event, EmbedBuilder eBuilder) {
        if (ctx != null)
            messageUtils.respondToUser(ctx, eBuilder.build());
        else
            event.getHook().sendMessageEmbeds(eBuilder.build()).setEphemeral(true).queue();
    }
}

package pl.xnik3e.Guardian.components.Command.Commands.BobCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import pl.xnik3e.Guardian.Models.NickNameModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.CommandContext;
import pl.xnik3e.Guardian.components.Command.ICommand;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
        boolean deleteTriggerMessage = fireStoreService.getModel().isDeleteTriggerMessage();
        if (deleteTriggerMessage)
            ctx.getMessage().delete().queue();
        Guild guild = ctx.getGuild();
        List<String> args = ctx.getArgs();
        bobifyUser(ctx, null, args, guild);
    }

    @Override
    public void handleSlash(SlashCommandInteractionEvent event, List<String> args) {
        bobifyUser(null, event, args, event.getGuild());
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
        embedBuilder.addField("Aliases", "`bob`, `b`", false);
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


    private void bobifyUser(CommandContext ctx, SlashCommandInteractionEvent event, List<String> args, Guild guild) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        if (args.isEmpty()) {
            embedBuilder.setTitle("Missing argument");
            embedBuilder.setDescription("You must provide user id or mention");
            embedBuilder.setColor(Color.RED);
            respondToUser(ctx, event, embedBuilder);
            return;
        }
        if (args.size() != 1) {
            embedBuilder.setTitle("Too many arguments");
            embedBuilder.setDescription("You must provide only one argument");
            embedBuilder.setColor(Color.RED);
            respondToUser(ctx, event, embedBuilder);
            return;
        }

        Matcher matcher = Pattern.compile("\\d+")
                .matcher(args.get(0));
        if (!matcher.find()) {
            embedBuilder.setTitle("Error");
            embedBuilder.setDescription("Please provide valid user id");
            embedBuilder.setColor(Color.RED);
            respondToUser(ctx, event, embedBuilder);
            return;
        }
        String userID = matcher.group();

        if (userID.equalsIgnoreCase("all")) {
            //UNLEASH THE BEAST
            guild.findMembers(member -> !messageUtils.hasMentionableNickName(member))
                    .onSuccess(members -> {
                        members.forEach(messageUtils::bobify);
                        embedBuilder.setTitle("Success");
                        embedBuilder.setDescription("Bobified " + members.size() + " users");
                        embedBuilder.setColor(Color.GREEN);
                        respondToUser(ctx, event, embedBuilder);
                    }).onError(error -> {
                        embedBuilder.setTitle("Error");
                        embedBuilder.setDescription("Something went wrong");
                        embedBuilder.setColor(Color.RED);
                        respondToUser(ctx, event, embedBuilder);
                    });
        } else {
            guild.retrieveMemberById(userID).queue(member -> {
                NickNameModel nickNameModel = fireStoreService.getNickNameModel(member.getId());
                if(nickNameModel != null)
                {
                    String nickName = member.getEffectiveName();
                    nickNameModel.getNickName().remove(nickName);
                    fireStoreService.addNickModel(nickNameModel);
                }
                embedBuilder.setTitle("Success");
                embedBuilder.setDescription("Bobified user " + member.getAsMention());
                embedBuilder.addField("Previous nickname", member.getEffectiveName(), false);
                embedBuilder.setColor(Color.GREEN);
                messageUtils.bobify(member);
                respondToUser(ctx, event, embedBuilder);
            });
        }
    }

    private CompletableFuture<Message> respondToUser(CommandContext ctx, SlashCommandInteractionEvent event, EmbedBuilder eBuilder) {
        if (ctx != null)
            return messageUtils.respondToUser(ctx, eBuilder.build());
        else
            return event.getHook().sendMessageEmbeds(eBuilder.build()).setEphemeral(true).submit();
    }
}

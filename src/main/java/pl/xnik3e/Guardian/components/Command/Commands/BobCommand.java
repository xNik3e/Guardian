package pl.xnik3e.Guardian.components.Command.Commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.CommandContext;
import pl.xnik3e.Guardian.components.Command.ICommand;

import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


public class BobCommand implements ICommand {

    private final FireStoreService fireStoreService;
    private final MessageUtils messageUtils;

    public BobCommand(MessageUtils messageUtils) {
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
        getBob(ctx, null, args, guild);
    }

    @Override
    public void handleSlash(SlashCommandInteractionEvent event, List<String> args) {
        getBob(null, event, args, event.getGuild());
    }

    @Override
    public String getName() {
        return "getbob";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(getTitle());
        embedBuilder.setDescription(getDescription());
        embedBuilder.addField("Usage",
                "`" + fireStoreService.getModel().getPrefix() + "getbob", false);
        embedBuilder.addField("Available aliases", "`gb`, `fetchbob`, `fb`", false);
        Color color = new Color((int) (Math.random() * 0x1000000));
        embedBuilder.setColor(color);
        return embedBuilder.build();
    }

    @Override
    public String getDescription() {
        return "Return list of user which username are marked as not being mentionable";
    }

    @Override
    public String getTitle() {
        return "Get users qualified for being Bob";
    }

    @Override
    public boolean isAfterInit() {
        return true;
    }

    @Override
    public List<String> getAliases() {
        return List.of("gb", "fetchbob", "fb");
    }

    private void getBob(CommandContext ctx, SlashCommandInteractionEvent event, List<String> args, Guild guild) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        EmbedBuilder defaultEmbedBuilder = new EmbedBuilder();
        defaultEmbedBuilder.setTitle("Please wait");
        defaultEmbedBuilder.setDescription("Fetching data from database. This operation is resource heavy and may take a while");
        defaultEmbedBuilder.addField("On success", "I will edit this message when list is ready", false);
        defaultEmbedBuilder.setColor(Color.YELLOW);
        try {
            Message oryginalMessage = respondToUser(ctx, event, defaultEmbedBuilder).get(5, TimeUnit.SECONDS);
            if (!args.isEmpty()) {
                embedBuilder.setTitle("Error");
                embedBuilder.setDescription("This command doesn't take any arguments");
                embedBuilder.setColor(Color.RED);
                respondToUser(ctx, event, embedBuilder);
                return;
            }
            embedBuilder.setTitle("Bob list");
            embedBuilder.setDescription("List of users which username are marked as not being mentionable");
            guild.findMembers(member -> !messageUtils.hasMentionableNickName(member))
                    .onSuccess(members -> {
                        members.forEach(member -> {
                            embedBuilder.addField(member.getEffectiveName(), member.getId(), true);
                        });
                        embedBuilder.setColor(Color.GREEN);
                        embedBuilder.setFooter("To Bobify specific user use `" + fireStoreService.getModel().getPrefix() + "bobify <userID>` command");
                        Button button = Button.primary("bobifyall", "Bobify all");
                        MessageCreateData messageCreateData = new MessageCreateBuilder().addEmbeds(embedBuilder.build()).addActionRow(button).build();
                        MessageEditBuilder messageEditBuilder = new MessageEditBuilder().applyCreateData(messageCreateData);
                        oryginalMessage.editMessage(messageEditBuilder.build()).queue();
                    }).onError(error -> {
                        embedBuilder.setTitle("Error");
                        embedBuilder.setDescription("Something went wrong");
                        embedBuilder.setColor(Color.RED);
                        oryginalMessage.editMessageEmbeds(embedBuilder.build()).queue();
                    });
        } catch (Exception e) {
            System.err.println("Error while sending message to user");
        }
    }

    private CompletableFuture<Message> respondToUser(CommandContext ctx, SlashCommandInteractionEvent event, EmbedBuilder eBuilder) {
        if (ctx != null)
            return messageUtils.respondToUser(ctx, eBuilder.build());
        else
            return event.getHook().sendMessageEmbeds(eBuilder.build()).setEphemeral(true).submit();
    }
}

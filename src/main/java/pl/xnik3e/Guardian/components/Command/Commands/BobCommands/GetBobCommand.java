package pl.xnik3e.Guardian.components.Command.Commands.BobCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import pl.xnik3e.Guardian.Models.FetchedRoleModel;
import pl.xnik3e.Guardian.Models.ToBobifyMembersModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.CommandContext;
import pl.xnik3e.Guardian.components.Command.ICommand;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class GetBobCommand implements ICommand {

    private final int MAX_USERS;
    private final FireStoreService fireStoreService;
    private final MessageUtils messageUtils;

    public GetBobCommand(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
        this.MAX_USERS = fireStoreService.getModel().getMaxElementsInEmbed();
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
                "`" + fireStoreService.getModel().getPrefix() + "getbob`", false);
        embedBuilder.addField("Available aliases", messageUtils.createAliasString(getAliases()), false);
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
        EmbedBuilder defaultEmbedBuilder = getDefaultEmbedBuilder();
        try {
            Message oryginalMessage = messageUtils.respondToUser(ctx, event, defaultEmbedBuilder).get(5, TimeUnit.SECONDS);
            if (!args.isEmpty()) {
                embedBuilder.setTitle("Error");
                embedBuilder.setDescription("This command doesn't take any arguments");
                embedBuilder.setColor(Color.RED);
                messageUtils.respondToUser(ctx, event, embedBuilder);
                return;
            }
            guild.findMembers(member -> !messageUtils.hasMentionableNickName(member))
                    .onSuccess(members -> {
                        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder();
                        ToBobifyMembersModel deletedModel = fireStoreService.deleteCacheUntilNow(ToBobifyMembersModel.class);
                        if(deletedModel != null){
                            JDA jda = event != null ? event.getJDA() : ctx.getJDA();
                            messageUtils.deleteMessage(jda, deletedModel);
                        }
                        List<Map<String, String>> maps = new ArrayList<>();
                        List<Map<String, String>> temp = new ArrayList<>();
                        AtomicInteger ordinal = new AtomicInteger();

                        ToBobifyMembersModel model = new ToBobifyMembersModel();
                        model.setTimestamp(System.currentTimeMillis() + 1000 * 60 * 5);

                        members.forEach(member -> {
                            mapMember(member, ordinal, maps);
                        });
                        model.setMaps(maps);
                        model.setAllEntries(maps.size());
                        model.setMessageID(oryginalMessage.getId());
                        model.setChannelId(oryginalMessage.getChannelId());
                        model.setUserID(oryginalMessage.getAuthor().getId());
                        model.setPrivateChannel(oryginalMessage.getChannelType() == ChannelType.PRIVATE);
                        fireStoreService.setCacheModel(model);

                        embedBuilder.setTitle("Bob list");
                        embedBuilder.setDescription("List of users which username are marked as not being mentionable");
                        embedBuilder.appendDescription("\nTo Bobify specific user use `" + fireStoreService.getModel().getPrefix() + "bobify <userID>` command");
                        embedBuilder.setColor(Color.GREEN);

                        int additionalPages = model.getAllEntries() % MAX_USERS == 0 ?
                                0 : MAX_USERS == 1 ?
                                0 : 1;

                        if(model.getAllEntries() >= MAX_USERS){
                            temp.addAll(maps.subList(0, MAX_USERS));
                            embedBuilder.setFooter("Showing page {**1/" + ((model.getAllEntries() / MAX_USERS) + additionalPages) + "**} for [GetBob]");
                            messageCreateBuilder.setActionRow(Button.primary("nextPage", "Next page"));
                        }else{
                            temp.addAll(maps);
                        }
                        if (!members.isEmpty()) {
                            Button button = Button.danger("bobifyall", "Bobify all");
                            messageCreateBuilder.addActionRow(button);
                        }

                        temp.forEach(fetchedMap -> {
                            embedBuilder.addField(fetchedMap.get("effectiveName"), fetchedMap.get("mention") + "\n[" + fetchedMap.get("userID")+"]", true);
                        });

                        messageCreateBuilder.addEmbeds(embedBuilder.build());
                        MessageEditBuilder messageEditBuilder = new MessageEditBuilder().applyCreateData(messageCreateBuilder.build());
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

    private static EmbedBuilder getDefaultEmbedBuilder() {
        EmbedBuilder defaultEmbedBuilder = new EmbedBuilder();
        defaultEmbedBuilder.setTitle("Please wait");
        defaultEmbedBuilder.setDescription("Fetching data from database. This operation is resource heavy and may take a while");
        defaultEmbedBuilder.addField("On success", "I will edit this message when list is ready", false);
        defaultEmbedBuilder.setColor(Color.YELLOW);
        return defaultEmbedBuilder;
    }

    private void mapMember(Member member, AtomicInteger ordinal, List<Map<String, String>> maps) {
        Map<String, String> map = new HashMap<>();
        map.put("ordinal", String.valueOf(ordinal.getAndIncrement()));
        map.put("userID", member.getId());
        map.put("effectiveName", member.getEffectiveName());
        map.put("mention", member.getAsMention());
        maps.add(map);
    }

}

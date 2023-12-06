package pl.xnik3e.Guardian.Components.Command.Commands.BobCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import pl.xnik3e.Guardian.Models.ContextModel;
import pl.xnik3e.Guardian.Models.ToBobifyMembersModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.Components.Command.CommandContext;
import pl.xnik3e.Guardian.Components.Command.ICommand;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class GetBobCommand implements ICommand {

    private final int MAX_USERS;
    private final FireStoreService fireStoreService;
    private final MessageUtils messageUtils;

    private ToBobifyMembersModel model;
    private List<Map<String, String>> maps;
    private MessageCreateBuilder messageCreateBuilder;
    private EmbedBuilder defaultResponseEmbedBuilder;

    public GetBobCommand(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
        this.MAX_USERS = fireStoreService.getModel().getMaxElementsInEmbed();
    }

    @Override
    public void handle(CommandContext ctx) {
        messageUtils.deleteTrigger(ctx);
        getBob(new ContextModel(ctx));
    }

    @Override
    public void handleSlash(SlashCommandInteractionEvent event, List<String> args) {
        getBob(new ContextModel(event, args));
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

    private void getBob(ContextModel context) {
        try {
            EmbedBuilder defaultEmbedBuilder = getDefaultEmbedBuilder();
            if (!context.args.isEmpty()) {
                sendErrorMessageTooMuchArguments(context);
                return;
            }
            Message oryginalMessage = messageUtils.respondToUser(context.ctx, context.event, defaultEmbedBuilder)
                    .get(5, TimeUnit.SECONDS);

            context.guild.findMembers(member -> !messageUtils.hasMentionableNickName(member))
                    .onSuccess(members -> {
                        deleteCachedModelAndMessage(context);
                        buildResponse(context, members, oryginalMessage);
                    }).onError(error -> {
                        setError(oryginalMessage);
                    });
        } catch (Exception e) {
            System.err.println("Error while sending message to user");
        }
    }

    private void buildResponse(ContextModel context, List<Member> members, Message oryginalMessage) {
        messageCreateBuilder = new MessageCreateBuilder();
        updateMaps(members);
        createToBobifyModel(context, oryginalMessage);

        createDefaultEmbedBuilder();
        addCacheWarning();
        populateEmbedFields();
        addFooterIfRequired();
        addBibifyButtonIfRequired(members);

        editOriginalMessage(oryginalMessage);
    }

    private void editOriginalMessage(Message oryginalMessage) {
        messageCreateBuilder.addEmbeds(defaultResponseEmbedBuilder.build());
        MessageEditBuilder messageEditBuilder = new MessageEditBuilder().applyCreateData(messageCreateBuilder.build());
        oryginalMessage.editMessage(messageEditBuilder.build()).queue();
    }

    private void populateEmbedFields() {
        List<Map<String, String>> temp = new ArrayList<>(model.getAllEntries() >= MAX_USERS ? maps.subList(0, MAX_USERS) : maps);
        temp.forEach(fetchedMap -> {
            defaultResponseEmbedBuilder.addField(fetchedMap.get("effectiveName"), fetchedMap.get("mention") + "\n[" + fetchedMap.get("userID")+"]", true);
        });
    }

    private void addBibifyButtonIfRequired(List<Member> members) {
        if (!members.isEmpty()) {
            Button button = Button.danger("bobifyall", "Bobify all");
            messageCreateBuilder.addActionRow(button);
        }
    }

    private void addFooterIfRequired() {
        int additionalPages = model.getAllEntries() % MAX_USERS == 0 ?
                0 : MAX_USERS == 1 ?
                0 : 1;

        if(model.getAllEntries() >= MAX_USERS){
            defaultResponseEmbedBuilder.setFooter("Showing page {**1/" + ((model.getAllEntries() / MAX_USERS) + additionalPages) + "**} for [GetBob]");
            messageCreateBuilder.setActionRow(Button.primary("nextPage", "Next page"));
        }

    }

    private void updateMaps(List<Member> members) {
        this.maps = new ArrayList<>();
        AtomicInteger ordinal = new AtomicInteger();

        members.forEach(member -> {
            mapMember(member, ordinal, maps);
        });
    }

    private void addCacheWarning() {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date(model.getTimestamp()));
        if(model.getAllEntries() != 0){
            fireStoreService.setCacheModel(model);
            defaultResponseEmbedBuilder.appendDescription("\n\n**CACHED DATA WILL BE ISSUED FOR DELETION AFTER: **"+ time+ "\n*ANY REQUESTS AFTER THAT TIME CAN RESULT IN FAILURE*\n");
        }
    }

    private void createDefaultEmbedBuilder() {
        defaultResponseEmbedBuilder = new EmbedBuilder();
        defaultResponseEmbedBuilder.setTitle("Bob list");
        defaultResponseEmbedBuilder.setDescription("List of users which username are marked as not being mentionable");
        defaultResponseEmbedBuilder.appendDescription("\nTo Bobify specific user use `" + fireStoreService.getModel().getPrefix() + "bobify <userID>` command");
        defaultResponseEmbedBuilder.setColor(Color.GREEN);
    }

    private static void setError(Message oryginalMessage) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Error");
        embedBuilder.setDescription("Something went wrong");
        embedBuilder.setColor(Color.RED);
        oryginalMessage.editMessageEmbeds(embedBuilder.build()).queue();
    }

    private void createToBobifyModel(ContextModel context, Message oryginalMessage) {
        model = new ToBobifyMembersModel();
        model.setTimestamp(System.currentTimeMillis() + 1000 * 60 * 5);
        model.setMaps(maps);
        model.setAllEntries(maps.size());
        model.setMessageID(oryginalMessage.getId());
        model.setChannelId(oryginalMessage.getChannelId());
        model.setUserID(context.from == ContextModel.From.EVENT ?  context.event.getUser().getId() :  context.ctx.getAuthor().getId());
        model.setPrivateChannel(oryginalMessage.getChannelType() == ChannelType.PRIVATE);
    }

    private void deleteCachedModelAndMessage(ContextModel context) {
        ToBobifyMembersModel deletedModel = fireStoreService.deleteCacheUntilNow(ToBobifyMembersModel.class);
        if(deletedModel != null){
            JDA jda = context.from == ContextModel.From.EVENT ? context.event.getJDA() :  context.ctx.getJDA();
            messageUtils.deleteMessage(jda, deletedModel);
        }
    }

    private void sendErrorMessageTooMuchArguments(ContextModel context) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Error");
        embedBuilder.setDescription("This command doesn't take any arguments");
        embedBuilder.setColor(Color.RED);
        messageUtils.respondToUser(context.ctx, context.event, embedBuilder);
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

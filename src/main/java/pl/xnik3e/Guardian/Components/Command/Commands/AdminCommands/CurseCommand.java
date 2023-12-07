package pl.xnik3e.Guardian.Components.Command.Commands.AdminCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.concurrent.Task;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import pl.xnik3e.Guardian.Models.ContextModel;
import pl.xnik3e.Guardian.Models.CurseModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.Components.Command.CommandContext;
import pl.xnik3e.Guardian.Components.Command.ICommand;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class CurseCommand implements ICommand {

    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;
    public final int MAX_USERS;

    private CurseModel model;
    private List<Map<String, String>> maps;
    private EmbedBuilder defaultResponseEmbedBuilder;
    private MessageCreateBuilder createData;

    public CurseCommand(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
        this.MAX_USERS = fireStoreService.getModel().getMaxElementsInEmbed();
    }

    @Override
    public void handle(CommandContext ctx) {
        messageUtils.deleteTrigger(ctx);
        curse(new ContextModel(ctx));
    }

    @Override
    public void handleSlash(SlashCommandInteractionEvent event, List<String> args) {
        curse(new ContextModel(event, args));
    }

    @Override
    public String getName() {
        return "curse";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(getTitle());
        embedBuilder.setDescription(getDescription());
        embedBuilder.addField("Usage", "`{prefix or mention} curse`", false);
        embedBuilder.addField("Available aliases", messageUtils.createAliasString(getAliases()), false);
        Color color = new Color((int) (Math.random() * 0x1000000));
        embedBuilder.setColor(color);
        return embedBuilder.build();
    }

    @Override
    public String getDescription() {
        return "Ban users that don't have **kultysta** role from the existence";
    }

    @Override
    public String getTitle() {
        return "Banish the unholy spirits!";
    }

    @Override
    public boolean isAfterInit() {
        return true;
    }

    @Override
    public List<String> getAliases() {
        return List.of("curse");
    }

    private void curse(ContextModel context) {
        if (!context.args.isEmpty()) {
            sendErrorMessageToMuchArguments(context);
            return;
        }
        String defaultRoleId = fireStoreService.getModel().getDefaultRoleId();
        EmbedBuilder eBuilder = new EmbedBuilder();
        eBuilder.setTitle("Curse");
        eBuilder.setDescription("Providing you with the list of unholy spirits...\n*Please wait...*");
        eBuilder.setColor(Color.YELLOW);

        CompletableFuture<Message> future = messageUtils.respondToUser(context.ctx, context.event, eBuilder);
        context.guild.findMembers(member -> member.getRoles()
                        .stream()
                        .map(Role::getId)
                        .noneMatch(defaultRoleId::equals))
                .onSuccess(members -> {
                    Message message = future.join();
                    createData = new MessageCreateBuilder();

                    deleteCachedModelAndMessage(context);
                    updateMaps(members);
                    createModel(context, message);

                    createDefaultEmbedBuilder();
                    addCacheWarning();
                    populateEmbedFields();
                    addFooterIfRequired();
                    addCurseButtonIfRequired(members);

                    editOriginalMessage( message);
                }).onError(throwable -> {
                    sendErrorMessageFetchingMembers(context);
                });
    }

    private void sendErrorMessageFetchingMembers(ContextModel context) {
        EmbedBuilder eBuilder = new EmbedBuilder();
        eBuilder.setTitle("Error");
        eBuilder.setDescription("Something went wrong while fetching members");
        eBuilder.setColor(Color.RED);
        messageUtils.respondToUser(context.ctx, context.event, eBuilder);
    }

    private void editOriginalMessage(Message message) {
        createData.setEmbeds(defaultResponseEmbedBuilder.build());
        MessageEditData messageCreateData = new MessageEditBuilder().applyCreateData(createData.build()).build();
        message.editMessage(messageCreateData).queue();
    }

    private void populateEmbedFields() {
        List<Map<String, String>> temp = new ArrayList<>(model.getAllEntries() >= MAX_USERS ? model.getMaps().subList(0, MAX_USERS) : model.getMaps());

        temp.forEach(fetchedMap -> {
            defaultResponseEmbedBuilder.addField(fetchedMap.get("effectiveName"), fetchedMap.get("mention") + "\nJoined: " +
                    new SimpleDateFormat("yyyy.MM.dd [HH:mm]").format(new Date(Long.parseLong(fetchedMap.get("timeJoined")))), true);
        });
    }

    private void addCurseButtonIfRequired(List<Member> members) {
        if(!members.isEmpty())
            createData.addActionRow(Button.danger("curse", "Curse them!"));
    }

    private void addFooterIfRequired() {
        int additionalPages = model.getAllEntries() % MAX_USERS == 0 ?
                0 : MAX_USERS == 1 ?
                0 : 1;

        if (model.getAllEntries() >= MAX_USERS) {
            defaultResponseEmbedBuilder.setFooter("Showing page {**1/" + ((model.getAllEntries() / MAX_USERS) + additionalPages) + "**} for [Curse]");
            createData.setActionRow(Button.primary("nextPage", "Next page"));
        }
    }

    private void addCacheWarning() {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date(model.getTimestamp()));
        if (model.getAllEntries() != 0) {
            fireStoreService.setCacheModel(model);
            defaultResponseEmbedBuilder.appendDescription("\n\n**CACHED DATA WILL BE ISSUED FOR DELETION AFTER: **" + time + "\n*ANY REQUESTS AFTER THAT TIME CAN RESULT IN FAILURE*\n");
        }
    }

    private void createDefaultEmbedBuilder() {
        defaultResponseEmbedBuilder = new EmbedBuilder();
        defaultResponseEmbedBuilder.setTitle("Evil spirits");
        defaultResponseEmbedBuilder.setDescription("The following " + model.getAllEntries() + " members are not blessed with the **kultysta** role");
        defaultResponseEmbedBuilder.setColor(Color.GREEN);
    }

    private void createModel(ContextModel context, Message message) {
        model = new CurseModel();
        model.setTimestamp(System.currentTimeMillis() + 1000 * 60 * 5);
        model.setMessageID(message.getId());
        model.setUserID(context.from == ContextModel.From.EVENT ? context.event.getUser().getId() : context.ctx.getAuthor().getId());
        model.setChannelId(message.getChannelId());
        model.setPrivateChannel(message.getChannelType() == ChannelType.PRIVATE);
        model.setMaps(maps);
        model.setAllEntries(model.getMaps().size());
    }

    private void updateMaps(List<Member> members) {
        maps = new ArrayList<>();
        AtomicInteger ordinal = new AtomicInteger();
        members.stream().sorted((m1, m2) -> {
            long time1 = m1.getTimeJoined().toEpochSecond();
            long time2 = m2.getTimeJoined().toEpochSecond();
            return Long.compare(time1, time2);
        }).forEachOrdered(member -> {
            mapMember(member, ordinal, maps);
        });
    }

    private void sendErrorMessageToMuchArguments(ContextModel context) {
        EmbedBuilder eBuilder = new EmbedBuilder();
        eBuilder.setTitle("Error");
        eBuilder.setDescription("This command doesn't take any arguments");
        eBuilder.setColor(Color.RED);
        messageUtils.respondToUser(context.ctx, context.event, eBuilder);
    }

    private void deleteCachedModelAndMessage(ContextModel context) {
        CurseModel deletedModel = fireStoreService.deleteCacheUntilNow(CurseModel.class);
        if (deletedModel != null) {
            JDA jda = context.from == ContextModel.From.EVENT ? context.event.getJDA() : context.ctx.getJDA();
            messageUtils.deleteMessage(jda, deletedModel);
        }
    }

    private void mapMember(Member member, AtomicInteger ordinal, List<Map<String, String>> maps) {
        if (messageUtils.performMemberCheck(member)) return;
        Map<String, String> map = Map.of(
                "userID", member.getId(),
                "effectiveName", member.getEffectiveName(),
                "ordinal", String.valueOf(ordinal.getAndIncrement()),
                "mention", member.getAsMention(),
                "timeJoined", String.valueOf(member.getTimeJoined().toEpochSecond() * 1000)
        );
        maps.add(map);
    }
}

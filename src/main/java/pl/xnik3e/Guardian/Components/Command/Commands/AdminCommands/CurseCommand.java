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

    public CurseCommand(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
        this.MAX_USERS = fireStoreService.getModel().getMaxElementsInEmbed();
    }

    @Override
    public void handle(CommandContext ctx) {
        boolean deleteTriggerMessage = fireStoreService.getModel().isDeleteTriggerMessage();
        if(deleteTriggerMessage)
            ctx.getMessage().delete().queue();
        curse(ctx, null, ctx.getArgs(), ctx.getGuild());
    }

    @Override
    public void handleSlash(SlashCommandInteractionEvent event, List<String> args) {
        curse(null, event, args, event.getGuild());
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
        Color color = new Color((int) (Math.random() *  0x1000000));
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

    private void curse(CommandContext ctx, SlashCommandInteractionEvent event, List<String> args, Guild guild) {
        EmbedBuilder eBuilder = new EmbedBuilder();
        if(!args.isEmpty()){
            eBuilder.setTitle("Error");
            eBuilder.setDescription("This command doesn't take any arguments");
            eBuilder.setColor(Color.RED);
            messageUtils.respondToUser(ctx, event, eBuilder);
            return;
        }
        String defaultRoleId = fireStoreService.getModel().getDefaultRoleId();
        eBuilder.setTitle("Curse");
        eBuilder.setDescription("Providing you with the list of unholy spirits...\n*Please wait...*");
        eBuilder.setColor(Color.YELLOW);
        CompletableFuture<Message> future = messageUtils.respondToUser(ctx, event, eBuilder);
        Task<List<Member>> excludedMembers = guild.findMembers(member -> member.getRoles()
                .stream()
                .map(Role::getId)
                .noneMatch(defaultRoleId::equals));
        excludedMembers.onSuccess(members -> {
            Message message = future.join();
            CurseModel deletedModel = fireStoreService.deleteCacheUntilNow(CurseModel.class);
            if(deletedModel != null){
                JDA jda = event != null ? event.getJDA() : ctx.getJDA();
                messageUtils.deleteMessage(jda, deletedModel);
            }

            MessageCreateBuilder createData = new MessageCreateBuilder();
            List<Map<String, String>> maps = new ArrayList<>();
            List<Map<String, String>> temp = new ArrayList<>();
            AtomicInteger ordinal = new AtomicInteger();

            CurseModel model = new CurseModel();
            model.setTimestamp(System.currentTimeMillis() + 1000 * 60 * 5);
            model.setMessageID(message.getId());
            model.setUserID(event != null ? event.getUser().getId() : ctx.getAuthor().getId());
            model.setChannelId(message.getChannelId());
            model.setPrivateChannel(message.getChannelType() == ChannelType.PRIVATE);

            String time = new SimpleDateFormat("HH:mm:ss").format(new Date(model.getTimestamp()));
            members.stream().sorted((m1, m2) -> {
                long time1 = m1.getTimeJoined().toEpochSecond();
                long time2 = m2.getTimeJoined().toEpochSecond();
                return Long.compare(time1, time2);
            }).forEachOrdered(member -> {
                mapMember(member, ordinal, maps);
            });

            model.setMaps(maps);
            model.setAllEntries(model.getMaps().size());

            eBuilder.setTitle("Evil spirits");
            eBuilder.setDescription("The following " + model.getAllEntries() + " members are not blessed with the **kultysta** role");
            eBuilder.setColor(Color.GREEN);

            if(model.getAllEntries() != 0){
                fireStoreService.setCacheModel(model);
                eBuilder.appendDescription("\n\n**CACHED DATA WILL BE ISSUED FOR DELETION AFTER: **" + time + "\n*ANY REQUESTS AFTER THAT TIME CAN RESULT IN FAILURE*\n");
            }

            int additionalPages = model.getAllEntries() % MAX_USERS == 0 ?
                    0 : MAX_USERS == 1 ?
                    0 : 1;

            if(model.getAllEntries() >= MAX_USERS){
                temp.addAll(model.getMaps().subList(0, MAX_USERS));
                eBuilder.setFooter("Showing page {**1/" + ((model.getAllEntries() / MAX_USERS) + additionalPages) + "**} for [Curse]");
                createData.setActionRow(Button.primary("nextPage", "Next page"));
            }else{
                temp.addAll(model.getMaps());
            }
            createData.addActionRow(Button.danger("curse", "Curse them!"));

            temp.forEach(fetchedMap -> {
                eBuilder.addField(fetchedMap.get("effectiveName"), fetchedMap.get("mention") + "\nJoined: " +
                        new SimpleDateFormat("yyyy.MM.dd [HH:mm]").format(new Date(Long.parseLong(fetchedMap.get("timeJoined")))), true);
            });

            createData.setEmbeds(eBuilder.build());
            MessageEditData messageCreateData = new MessageEditBuilder().applyCreateData(createData.build()).build();
            message.editMessage(messageCreateData).queue();
        }).onError(throwable -> {
            eBuilder.setTitle("Error");
            eBuilder.setDescription("Something went wrong while fetching members");
            eBuilder.setColor(Color.RED);
            messageUtils.respondToUser(ctx, event, eBuilder);
        });
    }

    private void mapMember(Member member, AtomicInteger ordinal, List<Map<String, String>> maps) {
        if(messageUtils.performMemberCheck(member)) return;
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

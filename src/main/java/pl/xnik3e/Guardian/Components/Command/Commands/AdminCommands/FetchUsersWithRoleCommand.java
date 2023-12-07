package pl.xnik3e.Guardian.Components.Command.Commands.AdminCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import pl.xnik3e.Guardian.Models.ContextModel;
import pl.xnik3e.Guardian.Models.FetchedRoleModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.Components.Command.CommandContext;
import pl.xnik3e.Guardian.Components.Command.ICommand;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FetchUsersWithRoleCommand implements ICommand {

    public final int MAX_USERS;
    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    private List<Map<String, String>> maps;
    private FetchedRoleModel model;
    private MessageEditBuilder editBuilder;
    private EmbedBuilder defaultResponseEmbedBuilder;

    public FetchUsersWithRoleCommand(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
        this.MAX_USERS = fireStoreService.getModel().getMaxElementsInEmbed();
    }

    @Override
    public void handle(CommandContext ctx) {
        messageUtils.deleteTrigger(ctx);
        fetchUsers(new ContextModel(ctx));
    }

    @Override
    public void handleSlash(SlashCommandInteractionEvent event, List<String> args) {
        fetchUsers(new ContextModel(event, args));
    }

    @Override
    public String getName() {
        return "getuserswithrole";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(getTitle());
        builder.setDescription(getDescription());
        builder.addField("Usage", "`{prefix or mention} getuserswithrole <role id>`", false);
        builder.addField("Available aliases", messageUtils.createAliasString(getAliases()), false);
        //get random color
        Color color = new Color((int) (Math.random() * 0x1000000));
        builder.setColor(color);
        return builder.build();
    }

    @Override
    public String getDescription() {
        return "Returns list of users with specified role";
    }

    @Override
    public String getTitle() {
        return "Get users with role command";
    }


    @Override
    public List<String> getAliases() {
        return List.of("fetchusers", "getusers", "findbyrole", "fetch");
    }

    private void fetchUsers(ContextModel context) {
        if (context.args.size() == 1) {
            //regular expression to check whether role was mentioned or not
            Matcher matcher = Pattern.compile("\\d+")
                    .matcher(context.args.get(0));

            if (!matcher.find()) {
                sendErrorMessageNoValidID(context);
                return;
            }
            String roleId = matcher.group(0);
            Role role = context.guild.getRoleById(roleId);
            if (role == null) {
                sendErrorMessageNoValidID(context);
                return;
            }

            context.guild.findMembersWithRoles(role)
                    .onSuccess(members -> {
                        deleteCachedModelAndMessage(context);
                        buildFetchingResponse(context, members, role);
                    });
        } else {
            sendErrorMessageToManyArguments(context);
        }
    }

    private void buildFetchingResponse(ContextModel context, List<Member> members, Role role) {
        updateMaps(members);
        createFetchedRoleModel(role);

        EmbedBuilder eBuilder = new EmbedBuilder();
        eBuilder.setTitle("Fetching role *" + role.getName() + "* users");
        eBuilder.setDescription("Please wait, this may take a while");
        eBuilder.setColor(Color.YELLOW);


        messageUtils.respondToUser(context.ctx, context.event, eBuilder)
                .thenAccept(message -> {
                    updateFetchMessage(context, role, message);
                });
    }

    private void updateFetchMessage(ContextModel context, Role role, Message message) {
        editBuilder = new MessageEditBuilder();
        updateModel(context, message);

        createDefaultEmbedBuilder(role);
        addCacheWarning();
        populateEmbedFields();
        addFooterIfRequired();

        editOriginalMessage(context, message);
    }

    private void editOriginalMessage(ContextModel context, Message message) {
        editBuilder.setEmbeds(defaultResponseEmbedBuilder.build());
        messageUtils.editOryginalMessage(message, context.event, editBuilder.build());
    }

    private void populateEmbedFields() {
        List<Map<String, String>> temp = new ArrayList<>(model.getAllEntries() >= MAX_USERS ?
                model.getMaps().subList(0, MAX_USERS) :
                model.getMaps());

        temp.forEach(fetchedMap -> {
            defaultResponseEmbedBuilder.addField(fetchedMap.get("userID"), fetchedMap.get("value"), true);
        });
    }

    private void addFooterIfRequired() {
        int additionalPages = model.getAllEntries() % MAX_USERS == 0 ?
                0 : MAX_USERS == 1 ?
                0 : 1;

        if (model.getAllEntries() >= MAX_USERS) {
            defaultResponseEmbedBuilder.setFooter("Showing page {**1/" + ((model.getAllEntries() / MAX_USERS) + additionalPages) + "**} for [Fetch]");
            editBuilder.setActionRow(Button.primary("nextPage", "Next page"));
        }
    }

    private void addCacheWarning() {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date(model.getTimestamp()));

        if (model.getAllEntries() != 0) {
            fireStoreService.setCacheModel(model);
            defaultResponseEmbedBuilder.appendDescription("\n\n**CACHED DATA WILL BE ISSUED FOR DELETION AFTER: **" + time + "\n*ANY REQUESTS AFTER THAT TIME CAN RESULT IN FAILURE*\n");
        }
    }

    private void createDefaultEmbedBuilder(Role role) {
        defaultResponseEmbedBuilder = new EmbedBuilder();
        defaultResponseEmbedBuilder.setTitle("Fetched role *" + role.getName() + "* users");
        defaultResponseEmbedBuilder.setDescription("I've found **" + model.getAllEntries() + "** users with role **" + role.getName() + "**");
        defaultResponseEmbedBuilder.setColor(Color.GREEN);
    }

    private void updateModel(ContextModel context, Message message) {
        model.setMessageID(message.getId());
        model.setUserID(context.from == ContextModel.From.EVENT ? context.event.getUser().getId() : context.ctx.getAuthor().getId());
        model.setChannelId(message.getChannelId());
        model.setPrivateChannel(message.getChannelType() == ChannelType.PRIVATE);
        model.setAllEntries(model.getMaps().size());
    }

    private void createFetchedRoleModel(Role role) {
        model = new FetchedRoleModel();
        model.setRoleName(role.getName());
        model.setRoleID(role.getId());
        model.setTimestamp(System.currentTimeMillis() + 1000 * 60 * 5);
        model.setMaps(maps);
    }

    private void updateMaps(List<Member> members) {
        maps = new ArrayList<>();
        AtomicInteger ordinal = new AtomicInteger();
        members.forEach(member -> {
            mapMember(member, ordinal, maps);
        });
    }

    private void sendErrorMessageToManyArguments(ContextModel context) {
        EmbedBuilder eBuilder = new EmbedBuilder();
        eBuilder.setTitle("An error has occurred");
        eBuilder.setDescription("You should only provide single role Id or role mention");
        eBuilder.setColor(Color.RED);
        messageUtils.respondToUser(context.ctx, context.event, eBuilder);
    }

    private void deleteCachedModelAndMessage(ContextModel context) {
        FetchedRoleModel deletedModel = fireStoreService.deleteCacheUntilNow(FetchedRoleModel.class);
        if (deletedModel != null) {
            JDA jda = context.event != null ? context.event.getJDA() : context.ctx.getJDA();
            messageUtils.deleteMessage(jda, deletedModel);
        }
    }


    private void sendErrorMessageNoValidID(ContextModel context) {
        EmbedBuilder eBuilder = new EmbedBuilder();
        eBuilder.setTitle("An error has occurred");
        eBuilder.setDescription("Please provide valid role id or mention");
        eBuilder.setColor(Color.RED);
        messageUtils.respondToUser(context.ctx, context.event, eBuilder);
    }

    private void mapMember(Member member, AtomicInteger ordinal, List<Map<String, String>> maps) {
        if (messageUtils.performMemberCheck(member)) return;
        Map<String, String> map = new HashMap<>();
        map.put("ordinal", String.valueOf(ordinal.getAndIncrement()));
        map.put("userID", member.getId());
        map.put("value", member.getEffectiveName() + " " + member.getAsMention());
        maps.add(map);
    }


}

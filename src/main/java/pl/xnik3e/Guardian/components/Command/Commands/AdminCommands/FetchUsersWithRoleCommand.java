package pl.xnik3e.Guardian.components.Command.Commands.AdminCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.concurrent.Task;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import pl.xnik3e.Guardian.Models.FetchedRoleModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.CommandContext;
import pl.xnik3e.Guardian.components.Command.ICommand;

import javax.annotation.Nullable;

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

    public FetchUsersWithRoleCommand(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
        this.MAX_USERS = fireStoreService.getModel().getMaxElementsInEmbed();
    }

    @Override
    public void handle(CommandContext ctx) {
        boolean deleteTriggerMessage = messageUtils.getFireStoreService().getModel().isDeleteTriggerMessage();
        if (deleteTriggerMessage)
            ctx.getMessage().delete().queue();
        fetchUsers(ctx, null, ctx.getArgs(), ctx.getGuild());
    }

    @Override
    public void handleSlash(SlashCommandInteractionEvent event, List<String> args) {
        fetchUsers(null, event, args, event.getGuild());
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

    private void fetchUsers(@Nullable CommandContext ctx, @Nullable SlashCommandInteractionEvent event, List<String> args, Guild guild) {
        EmbedBuilder eBuilder = new EmbedBuilder();
        if (args.size() == 1) {
            //regular expression to check whether role was mentioned or not
            Matcher matcher = Pattern.compile("\\d+")
                    .matcher(args.get(0));

            eBuilder.setTitle("An error has occurred");
            if (!matcher.find()) {
                eBuilder.setDescription("Please provide valid role id or mention");
                eBuilder.setColor(Color.RED);
                messageUtils.respondToUser(ctx, event, eBuilder);
                return;
            }
            String roleId = matcher.group(0);
            Role role = guild.getRoleById(roleId);
            if (role == null) {
                eBuilder.setDescription("Please provide valid role id or mention");
                eBuilder.setColor(Color.RED);
                messageUtils.respondToUser(ctx, event, eBuilder);
                return;
            }
            Task<List<Member>> task = guild.findMembersWithRoles(role);
            task.onSuccess(members -> {
                FetchedRoleModel deletedModel = fireStoreService.deleteCacheUntilNow(FetchedRoleModel.class);
                if(deletedModel != null){
                    JDA jda = event != null ? event.getJDA() : ctx.getJDA();
                    messageUtils.deleteMessage(jda, deletedModel);
                }
                List<Map<String, String>> maps = new ArrayList<>();
                AtomicInteger ordinal = new AtomicInteger();

                FetchedRoleModel model = new FetchedRoleModel();
                model.setRoleName(role.getName());
                model.setRoleID(role.getId());
                model.setTimestamp(System.currentTimeMillis() + 1000 * 60 * 5);

                eBuilder.setTitle("Fetching role *" + role.getName() + "* users");
                eBuilder.setDescription("Please wait, this may take a while");
                eBuilder.setColor(Color.YELLOW);

                members.forEach(member -> {
                    mapMember(member, ordinal, maps);
                });

                model.setMaps(maps);
                messageUtils.respondToUser(ctx, event, eBuilder)
                        .thenAccept(message -> {
                            MessageEditBuilder editBuilder = new MessageEditBuilder();
                            List<Map<String, String>> temp = new ArrayList<>();
                            model.setMessageID(message.getId());
                            model.setUserID(message.getAuthor().getId());
                            model.setChannelId(message.getChannelId());
                            model.setPrivateChannel(message.getChannelType() == ChannelType.PRIVATE);
                            model.setAllEntries(model.getMaps().size());

                            String time = new SimpleDateFormat("HH:mm:ss dd.MM.yyyy").format(new Date(model.getTimestamp()));

                            fireStoreService.setCacheModel(model);
                            eBuilder.setTitle("Fetched role *" + role.getName() + "* users");
                            eBuilder.setDescription("I've found **" + model.getAllEntries() + "** users with role **" + role.getName() + "**");
                            eBuilder.appendDescription("\n\n**CACHED DATA WILL BE DELETED IN 5 MINUTES** at: " + time + "\n\n");
                            eBuilder.setColor(Color.GREEN);

                            int additionalPages = model.getAllEntries() % MAX_USERS == 0 ?
                                    0 : MAX_USERS == 1 ?
                                    0 : 1;

                            if (model.getAllEntries() >= MAX_USERS) {
                                temp.addAll(model.getMaps().subList(0, MAX_USERS));
                                eBuilder.setFooter("Showing page {**1/" + ((model.getAllEntries() / MAX_USERS) + additionalPages) + "**} for [Fetch]");
                                editBuilder.setActionRow(Button.primary("nextPage", "Next page"));
                            } else {
                                temp.addAll(model.getMaps());
                            }

                            temp.forEach(fetchedMap -> {
                                eBuilder.addField(fetchedMap.get("userID"), fetchedMap.get("value"), true);
                            });
                            editBuilder.setEmbeds(eBuilder.build());
                            messageUtils.editOryginalMessage(message, event, editBuilder.build());
                        });
            });
        } else {
            eBuilder.setDescription("You should only provide single role Id or role mention");
            eBuilder.setColor(Color.RED);
            messageUtils.respondToUser(ctx, event, eBuilder);
        }
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

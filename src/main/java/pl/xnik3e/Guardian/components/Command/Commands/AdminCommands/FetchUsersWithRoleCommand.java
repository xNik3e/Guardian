package pl.xnik3e.Guardian.components.Command.Commands.AdminCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FetchUsersWithRoleCommand implements ICommand {

    public static final int MAX_USERS = 5;
    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    public FetchUsersWithRoleCommand(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
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
                fireStoreService.deleteFetchedRoleUserListBeforeNow();
                List<Map<String, String>> maps = new ArrayList<>();
                AtomicInteger ordinal = new AtomicInteger();
                FetchedRoleModel.FetchedRoleModelBuilder builder = FetchedRoleModel.builder();
                builder.roleName(role.getName());
                builder.roleID(role.getId());
                builder.timestamp(System.currentTimeMillis() + 1000 * 60 * 5);

                eBuilder.setTitle("Fetching role *" + role.getName() + "* users");
                eBuilder.setDescription("Please wait, this may take a while");
                eBuilder.setColor(Color.YELLOW);

                members.forEach(member -> {
                    mapMember(member, ordinal, maps);
                });

                builder.users(maps);
                messageUtils.respondToUser(ctx, event, eBuilder)
                        .thenAccept(message -> {
                            MessageEditBuilder editBuilder = new MessageEditBuilder();
                            List<Map<String, String>> temp = new ArrayList<>();
                            builder.messageID(message.getId());
                            FetchedRoleModel model = builder.build();
                            model.setAllEntries(model.getUsers().size());

                            fireStoreService.setFetchedRoleModel(model);
                            eBuilder.setTitle("Fetched role *" + role.getName() + "* users");
                            eBuilder.setDescription("I've found **" + model.getAllEntries() + "** users with role **" + role.getName() + "**");
                            eBuilder.appendDescription("\n\n**CACHED DATA WILL BE DELETED IN 5 MINUTES**\n\n");
                            eBuilder.setColor(Color.GREEN);

                            if (model.getAllEntries() > MAX_USERS) {
                                temp.addAll(model.getUsers().subList(0, MAX_USERS));
                                eBuilder.setFooter("Showing page {**1/" + ((model.getAllEntries() / MAX_USERS) + 1) + "**} for [Fetch]");
                                editBuilder.setActionRow(Button.primary("nextPage", "Next page"));
                            } else {
                                temp.addAll(model.getUsers());
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

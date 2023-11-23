package pl.xnik3e.Guardian.components.Command.Commands.AdminCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.concurrent.Task;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import pl.xnik3e.Guardian.Models.FetchedRoleUserModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.CommandContext;
import pl.xnik3e.Guardian.components.Command.ICommand;

import javax.annotation.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FetchUsersWithRoleCommand implements ICommand {

    public static final int MAX_USERS = 25;
    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    public FetchUsersWithRoleCommand(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
    }

    @Override
    public void handle(CommandContext ctx) {
        boolean deleteTriggerMessage = messageUtils.getFireStoreService().getModel().isDeleteTriggerMessage();
        if(deleteTriggerMessage)
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
        Color color = new Color((int)(Math.random() * 0x1000000));
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
        if(args.size() == 1){
            //regular expression to check whether role was mentioned or not
            Matcher matcher = Pattern.compile("\\d+")
                    .matcher(args.get(0));

            eBuilder.setTitle("An error has occurred");
            if(!matcher.find()){
                eBuilder.setDescription("Please provide valid role id or mention");
                eBuilder.setColor(Color.RED);
                messageUtils.respondToUser(ctx, event, eBuilder);
                return;
            }
            String roleId = matcher.group(0);
            Role role = guild.getRoleById(roleId);
            if(role == null){
                eBuilder.setDescription("Please provide valid role id or mention");
                eBuilder.setColor(Color.RED);
                messageUtils.respondToUser(ctx, event, eBuilder);
                return;
            }
            Task<List<Member>> task = guild.findMembersWithRoles(role);
            task.onSuccess(members -> {
                fireStoreService.deleteFetchedRoleUserListBeforeNow();
                eBuilder.setTitle("Fetching role *" + role.getName() + "* users");
                eBuilder.setDescription("Please wait, this may take a while");
                List<FetchedRoleUserModel> fetchedUsers = new ArrayList<>();
                AtomicInteger ordinal = new AtomicInteger();
                members.forEach(member -> {
                    if(messageUtils.performMemberCheck(member)) return;
                    FetchedRoleUserModel fetchedUser = FetchedRoleUserModel.builder()
                            .userID(member.getUser().getId())
                            .roleID(role.getId())
                            .roleName(role.getName())
                            .ordinal(ordinal.getAndIncrement())
                            .timestamp(System.currentTimeMillis() + 1000 * 60 * 5)
                            .value(member.getEffectiveName() + "\n"+member.getAsMention())
                            .build();
                    fetchedUsers.add(fetchedUser);
                });
                eBuilder.setColor(Color.YELLOW);
                CompletableFuture<Message> future = messageUtils.respondToUser(ctx, event, eBuilder);
                future.thenAccept(message -> {
                    MessageEditBuilder editBuilder = new MessageEditBuilder();
                    fetchedUsers.forEach(fetchedRoleUserModel -> {
                        fetchedRoleUserModel.setMessageID(message.getId());
                        fetchedRoleUserModel.setAllEntries(fetchedUsers.size());
                    });
                    fireStoreService.setFetchedRoleUserList(fetchedUsers);
                    eBuilder.setTitle("Fetched role *" + role.getName() + "* users");
                    eBuilder.setDescription("I've found **" + fetchedUsers.size() + "** users with role **" + role.getName() + "**");
                    eBuilder.appendDescription("\n\n**CACHED DATA WILL BE DELETED IN 5 MINUTES**\n\n");
                    List<FetchedRoleUserModel> temp = new ArrayList<>();
                    if(fetchedUsers.size() > MAX_USERS){
                        temp.addAll(fetchedUsers.subList(0, MAX_USERS));
                        eBuilder.setFooter("Showing page {**1/" + ((fetchedUsers.size() /MAX_USERS) + 1)   + "**} for [Fetch]");
                        Button button = Button.primary("nextPage", "Next page");
                        editBuilder.setActionRow(button);
                    }else{
                        temp.addAll(fetchedUsers);
                    }
                    temp.forEach(fetchedRoleUserModel -> {
                        eBuilder.addField(fetchedRoleUserModel.getUserID(), fetchedRoleUserModel.getValue(), true);
                    });
                    eBuilder.setColor(Color.GREEN);
                    editBuilder.setEmbeds(eBuilder.build());
                    messageUtils.editOryginalMessage(message, event, editBuilder.build());
                });
            });
        }else{
            eBuilder.setDescription("You should only provide single role Id or role mention");
            eBuilder.setColor(Color.RED);
            messageUtils.respondToUser(ctx, event, eBuilder);
        }
    }




}

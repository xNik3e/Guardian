package pl.xnik3e.Guardian.Utils;

import lombok.Getter;
import lombok.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.xnik3e.Guardian.Models.ConfigModel;
import pl.xnik3e.Guardian.Models.TempBanModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.components.Command.CommandContext;

import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Getter
@Service
public class MessageUtils {

    private final FireStoreService fireStoreService;
    private final ConfigModel configModel;

    @Autowired
    public MessageUtils(FireStoreService fireStoreService) {
        this.fireStoreService = fireStoreService;
        this.configModel = fireStoreService.getModel();
    }

    /**
     * Checks if member has any role included in rolesToDelete list.
     * If the user has granted special privileges, the method returns false.
     * <p>
     * Privileges are defined as merged list of excludedRoleIds and excludedUserIds.
     * <p>
     *
     * @param member Member to check
     * @return true if member has any role included in rolesToDelete list, false otherwise
     */
    public boolean checkRolesToDelete(Member member) {
        if (checkAuthority(member))
            return false;

        List<String> memberRoles = getMemberRoleList(member);
        return performRolesToDeleteCheck(memberRoles);
    }


    /**
     * Get list of member roles as String
     * <p></p>
     *
     * @param member Member to get roles from
     * @return List of member roles as String
     */
    @NotNull
    public static List<String> getMemberRoleList(Member member) {
        return member.getRoles()
                .stream()
                .map(Role::getIdLong)
                .map(String::valueOf).toList();
    }

    /**
     * Perform list search for rolesToDelete
     * <p></p>
     *
     * @param userRoles List of user roles
     * @return true if member has any role included in rolesToDelete list, false otherwise
     */
    private boolean performRolesToDeleteCheck(List<String> userRoles) {
        List<String> rolesToDelete = configModel
                .getRolesToDelete();
        return userRoles.stream().anyMatch(rolesToDelete::contains);
    }


    /**
     * Checks if member has any role included in excludedRoleIds list.
     * <p></p>
     *
     * @param userRoles List of user roles
     * @return true if member has any role included in excludedRoleIds list, false otherwise
     */
    private boolean performRolesToExcludeCheck(List<String> userRoles) {
        List<String> excludedRoles = configModel
                .getExcludedRoleIds();
        return userRoles.stream().anyMatch(excludedRoles::contains);
    }

    /**
     * Checks if member has any privileges.
     * <p></p>
     *
     * @param member Member to check
     * @return true if member has any role included in excludedRoleIds list or matching id in excludedUserIds list, false otherwise
     */
    private boolean checkAuthority(Member member) {
        List<String> memberRoles = getMemberRoleList(member);
        return performRolesToExcludeCheck(memberRoles) || configModel
                .getExcludedUserIds()
                .contains(member.getId());
    }

    /**
     * Checks if message should be processed by bot.
     * <p></p>
     *
     * @param event MessageReceivedEvent to check
     * @return true if message should be processed by bot, false otherwise
     */
    public boolean checkTrigger(MessageReceivedEvent event) {
        boolean respondByPrefix = configModel
                .isRespondByPrefix();

        if (respondByPrefix) {
            return checkPrefix(event);
        }
        return checkBotMention(event);
    }

    /**
     * Checks if bot was mentioned in message.
     * <p></p>
     *
     * @param event MessageReceivedEvent to check
     * @return true if bot was mentioned in message, false otherwise
     */
    private boolean checkBotMention(MessageReceivedEvent event) {
        return event.getMessage()
                .getMentions()
                .getMentions(Message.MentionType.USER)
                .stream()
                .anyMatch(user -> user.getIdLong() == event.getJDA().getSelfUser().getIdLong());
    }

    /**
     * Checks if message starts with prefix.
     * <p></p>
     *
     * @param event MessageReceivedEvent to check
     * @return true if message starts with prefix, false otherwise
     */
    private boolean checkPrefix(MessageReceivedEvent event) {
        String prefix = configModel
                .getPrefix();
        return event
                .getMessage()
                .getContentRaw()
                .startsWith(prefix);
    }

    /**
     * Sends message to user in private channel.
     * <p></p>
     *
     * @param user    Member to send private message to
     * @param message Message to send
     */
    public void openPrivateChannelAndMessageUser(User user, String message) {
        user
                .openPrivateChannel()
                .queue(privateChannel -> {
                    privateChannel.sendMessage(message).queue();
                });
    }

    /**
     * Sends message to user in private channel.
     * <p></p>
     *
     * @param user    Member to send private message to
     * @param message MessageEmbed to send
     */
    public void openPrivateChannelAndMessageUser(User user, MessageEmbed message) {
        user
                .openPrivateChannel()
                .queue(privateChannel -> {
                    privateChannel.sendMessageEmbeds(message).queue();
                });
    }

    /**
     * Sends message to user in private channel.
     * <p></p>
     *
     * @param user    Member to send private message to
     * @param message MessageCreateData to send
     */
    public void openPrivateChannelAndMessageUser(User user, MessageCreateData message) {
        user
                .openPrivateChannel()
                .queue(privateChannel -> {
                    privateChannel.sendMessage(message).queue();
                });
    }

    /**
     * Sends message to user in private channel or in channel where command was invoked.
     * <p></p>
     *
     * @param ctx     CommandContext to get member from
     * @param message Message to send
     */
    public void respondToUser(CommandContext ctx, String message) {
        if (configModel.isRespondInDirectMessage()) {
            openPrivateChannelAndMessageUser(ctx.getMember().getUser(), message);
        } else {
            ctx.getMessage().reply(message).queue();
        }
    }

    /**
     * Sends message to user in private channel or in channel where command was invoked.
     * <p></p>
     *
     * @param ctx     CommandContext to get member from
     * @param message MessageEmbed to send
     */
    public void respondToUser(CommandContext ctx, MessageEmbed message) {
        if (configModel.isRespondInDirectMessage()) {
            openPrivateChannelAndMessageUser(ctx.getMember().getUser(), message);
        } else {
            ctx.getMessage().replyEmbeds(message).queue();
        }
    }

    /**
     * Sends message to user in private channel or in channel where command was invoked.
     * <p></p>
     *
     * @param ctx     CommandContext to get member from
     * @param message MessageCreateData to send
     */
    public void respondToUser(CommandContext ctx, MessageCreateData message) {
        if (configModel.isRespondInDirectMessage()) {
            openPrivateChannelAndMessageUser(ctx.getMember().getUser(), message);
        } else {
            ctx.getMessage().reply(message).queue();
        }
    }


    /**
     * Get raw command content from MessageReceivedEvent when command is invoked.
     * <p></p>
     *
     * @param event MessageReceivedEvent to get command from
     * @return raw command content
     */
    public String rawCommandContent(MessageReceivedEvent event) {

        String command = event.getMessage().getContentRaw();
        if (fireStoreService.getModel().isRespondByPrefix()) {
            String prefix = fireStoreService.getModel()
                    .getPrefix();
            command = command.replace(prefix, "");
        } else {
            //delete bot mention
            command = command.replace("<@" + event.getJDA().getSelfUser().getId() + ">", "");
        }
        return command.trim();
    }

    /**
     * Bans users from guild.
     * <p></p>
     *
     * @param toBeBannedIds List of users to be banned
     * @param guild         Guild to ban users from
     */
    public void banUsers(List<String> toBeBannedIds, Guild guild) {
        MessageChannel channel = guild.getChannelById(MessageChannel.class, fireStoreService.getModel().getChannelIdToSendDeletedMessages());
        MessageChannel logChannel = guild.getChannelById(MessageChannel.class, fireStoreService.getModel().getChannelIdToSendLog());
        MessageChannel echoChannel = guild.getChannelById(MessageChannel.class, fireStoreService.getModel().getChannelIdToSendEchoLog());

        toBeBannedIds.forEach(id -> {
                    Member member = guild.getMemberById(id);
                    tempBanUser(member.getUser(), channel, guild, 1, TimeUnit.DAYS, "Niespełnianie wymagań wiekowych");

                    StringBuilder sb = new StringBuilder();
                    sb
                            .append("<@")
                            .append(id)
                            .append("> - ")
                            .append(member.getUser().getName())
                            .append(" - ")
                            .append("tempban rok")
                            .append(" - ").append("niespełnianie wymagań wiekowych");
                    if (logChannel != null)
                        logChannel.sendMessage(sb.toString()).queue();
                    if(echoChannel != null)
                        echoChannel.sendMessage(sb.toString()).queue();
                }
        );
    }

    /**
     * Checks if member should be excluded from bot actions.
     * <p></p>
     *
     * @param member Member to check
     * @return true if member should be excluded from bot actions, false otherwise
     */
    public boolean performMemberCheck(Member member) {
        List<String> excludedRoles = fireStoreService.getModel().getExcludedRoleIds();
        List<String> excludedMembers = fireStoreService.getModel().getExcludedUserIds();

        if (member.getUser().isBot())
            return true;
        if (member.getRoles().stream()
                .map(Role::getId)
                .anyMatch(excludedRoles::contains))
            return true;
        return excludedMembers.contains(member.getUser().getId());
    }

    private void tempBanUser(@NonNull User user, MessageChannel channel, Guild guild, long time, TimeUnit timeUnit, String reason){
        TempBanModel tempBanModel = new TempBanModel();
        tempBanModel.setUserId(user.getId());
        tempBanModel.setAvatarUrl(user.getAvatarUrl());
        tempBanModel.setUserName(user.getName());
        tempBanModel.setReason(reason);

        long timeInMillis = System.currentTimeMillis() + timeUnit.toMillis(time);
        tempBanModel.setBanTime(timeInMillis);

        //get time difference between current time and ban time in days
        Date date = new Date(timeInMillis);
        long days = (date.getTime() - System.currentTimeMillis()) / (1000 * 60 * 60 * 24);
        long weeks = days / 7;
        days = days % 7;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(
                tempBanModel.getUserName() + "has been banned for " + weeks + " weeks and " + days + " days",
                null,
                tempBanModel.getAvatarUrl());
        embedBuilder.setDescription("**Reason:** "+ tempBanModel.getReason());
        DateFormat f = new SimpleDateFormat("dd.MM.yyyy");
        embedBuilder.setFooter("Baned until | " + f.format(date));
        embedBuilder.setColor(0x5acff5);

        Button button = Button.danger("unban", "Unban");

        MessageCreateData message = new MessageCreateBuilder()
                .setEmbeds(embedBuilder.build())
                .setActionRow(button)
                .build();

        long finalDays = days;
        channel.sendMessage(message).queue(m -> {
            tempBanModel.setMessageId(m.getId());
            fireStoreService.setTempBanModel(tempBanModel);
            guild.ban(user, 0, TimeUnit.SECONDS)
                    .reason(reason)
                    .queue(s -> {
                        EmbedBuilder builder = new EmbedBuilder();
                        builder.setTitle("Banned");
                        builder.setDescription("You have been banned from " + guild.getName() + " for " + weeks + " weeks and " + finalDays + " days");
                        builder.addField("Reason", reason, false);
                        builder.setColor(Color.RED);
                        openPrivateChannelAndMessageUser(user, builder.build());
                    });
        });
    }
}

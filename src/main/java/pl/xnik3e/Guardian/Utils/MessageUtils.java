package pl.xnik3e.Guardian.Utils;

import lombok.Getter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.xnik3e.Guardian.Services.FireStoreService;

import java.util.List;

@Getter
@Service
public class MessageUtils {

    private final FireStoreService fireStoreService;

    @Autowired
    public MessageUtils(FireStoreService fireStoreService) {
        this.fireStoreService = fireStoreService;
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
        List<String> rolesToDelete = fireStoreService.getModel()
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
        List<String> excludedRoles = fireStoreService.getModel()
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
        return performRolesToExcludeCheck(memberRoles) || fireStoreService.getModel()
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
        boolean respondByPrefix = fireStoreService.getModel()
                .isRespondByPrefix();

        if (respondByPrefix) {
            return checkPrefix(event);
        }
        return checkBotMention(event);
    }

    /**
     * Checks if bot was mentioned in message.
     * <p></p>
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
     * @param event MessageReceivedEvent to check
     * @return true if message starts with prefix, false otherwise
     */
    private boolean checkPrefix(MessageReceivedEvent event) {
        String prefix = fireStoreService.getModel()
                .getPrefix();
        return event
                .getMessage()
                .getContentRaw()
                .startsWith(prefix);
    }

    /**
     * Sends message to user in private channel.
     * <p></p>
     * @param member Member to send private message to
     * @param message Message to send
     */
    public void openPrivateChannelAndMessageUser(Member member, String message) {
        member.getUser()
                .openPrivateChannel()
                .queue(privateChannel -> {
                    privateChannel.sendMessage(message).queue();
                });
    }

    public String rawCommandContent(MessageReceivedEvent event) {

        String command = event.getMessage().getContentRaw();
        if(fireStoreService.getModel().isRespondByPrefix()){
            String prefix = fireStoreService.getModel()
                    .getPrefix();
            command = command.replace(prefix, "");
        }else{
            //delete bot mention
            command = command.replace("<@!" + event.getJDA().getSelfUser().getId() + ">", "");
        }
        return command.trim();
    }
}

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

@Service
public class MessageUtils {

    @Getter
    private final FireStoreService fireStoreService;

    @Autowired
    public MessageUtils(FireStoreService fireStoreService) {
        this.fireStoreService = fireStoreService;
    }


    public boolean checkRolesToDelete(Member member) {
        /* check for specific role idk why, maybe for some fcking snowflake? */
        /*
        String uID = String.valueOf(member.getIdLong());
       if (uID.equals("SOME WEIRD ID"))
            return false;*/

        List<String> memberRoles = getMemberRoleList(member);

        if(checkAuthority(member))
            return false;

        return performRolesToDeleteCheck(memberRoles);
    }

    @NotNull
    private static List<String> getMemberRoleList(Member member) {
        return member.getRoles()
                .stream()
                .map(Role::getIdLong)
                .map(String::valueOf).toList();
    }

    private boolean performRolesToDeleteCheck(List<String> userRoles) {
        List<String> rolesToDelete = fireStoreService.getModel().getRolesToDelete();
        return userRoles.stream().anyMatch(rolesToDelete::contains);
    }


    private boolean performRolesToExcludeCheck(List<String> userRoles) {
        List<String> excludedRoles = fireStoreService.getModel().getExcludedRoleIds();
        return userRoles.stream().anyMatch(excludedRoles::contains);
    }

    private boolean checkAuthority(Member member){
        List<String> memberRoles = getMemberRoleList(member);
        return performRolesToExcludeCheck(memberRoles);
    }

    private boolean checkTrigger(MessageReceivedEvent event){
        boolean respondByPrefix = fireStoreService.getModel().isRespondByPrefix();
        if(respondByPrefix){
            return checkPrefix(event);
        }
        return checkBotMention(event);
    }

    private boolean checkBotMention(MessageReceivedEvent event){
        return event.getMessage()
                .getMentions()
                .getMentions(Message.MentionType.USER)
                .stream()
                .anyMatch(user -> user.getIdLong() == event.getJDA().getSelfUser().getIdLong());
    }

    private boolean checkPrefix(MessageReceivedEvent event){
        String prefix = fireStoreService.getModel().getPrefix();
        return event.getMessage().getContentRaw().startsWith(prefix);
    }

    public void openPrivateChannelAndMessageUser(Member member, String message) {
        member.getUser().openPrivateChannel().queue(privateChannel -> {
            privateChannel.sendMessage(message).queue();
        });
    }
}

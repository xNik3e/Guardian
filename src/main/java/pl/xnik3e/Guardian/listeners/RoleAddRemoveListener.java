package pl.xnik3e.Guardian.listeners;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;

import java.util.List;

public class RoleAddRemoveListener extends ListenerAdapter {

    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    private final List<String> userIds;

    @Autowired
    public RoleAddRemoveListener(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
        userIds = fireStoreService.getUserIds();
    }


    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        Member member = event.getMember();
        if (messageUtils.checkRolesToDelete(member)) {
            String userId = member.getId();
            if (!userIds.contains(userId)) {
                userIds.add(userId);
                fireStoreService.updateUserIds();
            }
        }
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        Member member = event.getMember();
        if (messageUtils.checkRolesToDelete(member)) {
            long userId = member.getIdLong();
            if (userIds.contains(userId)) {
                userIds.remove(userId);
                fireStoreService.updateUserIds();
            }
        }
    }
}

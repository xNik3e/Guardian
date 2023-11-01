package pl.xnik3e.Guardian.listeners;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MyEventListener extends ListenerAdapter {
    public final Firestore firestore;
    public final DocumentReference docRef;
    public static final long ROLE_ID = 1164645019769131029L;
    public final List<Long> userIds = new ArrayList<>();

    public MyEventListener(Firestore firestore) {
        this.firestore = firestore;
        this.docRef = firestore.collection("blacklist").document("toDelete");
        addSnapshotListener();
    }

    private void addSnapshotListener() {
        docRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                System.err.println("Listen failed: " + e);
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                userIds.clear();
                userIds.addAll((List<Long>) snapshot.get("userId"));
            } else {
                System.out.print("Current data: null");
            }
        });
    }

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        //super.onGuildMemberRoleAdd(event);
        List<Role> roles = event.getRoles();
        Member member = event.getMember();
        if (roles.stream().anyMatch(role -> role.getIdLong() == ROLE_ID)) {
            //openPrivateChannelAndNotifyUser(member, true);
            long userId = member.getIdLong();
            if (!userIds.contains(userId)) {
                userIds.add(userId);
                docRef.update("userId", userIds);
            }
        }
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        //super.onGuildMemberRoleRemove(event);
        List<Role> roles = event.getRoles();
        Member member = event.getMember();
        if (roles.stream().anyMatch(role -> role.getIdLong() == ROLE_ID)) {
            long userId = member.getIdLong();
            if (userIds.contains(userId)) {
                userIds.remove(userId);
                docRef.update("userId", userIds);
            }
        }
    }

    /**
     * TODO: TESTING PURPOSES ONLY
     */
    private void openPrivateChannelAndNotifyUser(Member member, boolean isRole) {
        member.getUser().openPrivateChannel().queue(privateChannel -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Witaj na serwerze ").append(member.getGuild().getName()).append("!\n");
            sb.append(isRole ? "Otrzymałeś rolę do wyjebania" : "Zostałeś pozbawiony roli do wyjebania");
            privateChannel.sendMessage(sb.toString()).queue();
        });
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        super.onMessageReceived(event);
        String message = event.getMessage().getContentRaw();
        if (message.equals("test")) {
            if (event.getAuthor().getIdLong() == 428233609342746634L) {
                for (Long id : userIds) {
                    event.getChannel().sendMessage("!tempban <@" + id + "> 365d niespełnianie wymagań wiekowych").queue();
                }
            } else {
                event.getChannel().sendMessage("Nie masz uprawnień do tego").queue();
            }
        }
    }
}

package pl.xnik3e.Guardian.components;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.springframework.scheduling.annotation.Scheduled;
import pl.xnik3e.Guardian.GuardianDiscordBot;

import java.util.ArrayList;
import java.util.List;

public class ScheduledTask {

    private final Firestore firestore;
    public final DocumentReference docRef;
    private final List<Long> userIds = new ArrayList<>();
    private final JDA jda;

    public ScheduledTask(Firestore firestore, GuardianDiscordBot bot) {
        this.firestore = firestore;
        this.docRef = firestore.collection("blacklist").document("toDelete");
        this.jda = bot.getJda();
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

    //schedule every 10 second
    //@Scheduled(fixedRate = 10 * 1000)
    //schedule every day at 22:46
    @Scheduled(cron = "0 46 22 * * *")
    public void notifyAllUsers(){
        Guild guild = jda.getGuildById(1112886826559078470L);
        for(Long id : userIds){
            Member member = guild.getMemberById(id);
            if(member != null){
                member.getUser().openPrivateChannel().queue(privateChannel -> {
                    privateChannel.sendMessage("Witaj, zostałeś zbanowany na serwerze " + guild.getName() + " przez bota Guardian. Jeśli chcesz się odwołać, napisz do administracji serwera.").queue();
                });
            }
        }
    }



}

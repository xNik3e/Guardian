package pl.xnik3e.Guardian.components;

import net.dv8tion.jda.api.JDA;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.xnik3e.Guardian.GuardianDiscordBot;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;

@Component
public class ScheduledTask {

    private final FireStoreService fireStoreService;
    private final MessageUtils messageUtils;
    private final JDA jda;


    @Autowired
    public ScheduledTask(MessageUtils messageUtils, GuardianDiscordBot bot) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
        this.jda = bot.getJda();
    }


    //schedule every 10 second
    //@Scheduled(fixedRate = 10 * 1000)
    //schedule every day at 22:46
    //@Scheduled(cron = "0 46 22 * * *")
   /* public void notifyAllUsers(){
        Guild guild = jda.getGuildById(1112886826559078470L);
        for(String id : userIds){
            User member = guild.getMemberById(id).getUser();
            if(member != null){
                messageUtils.openPrivateChannelAndMessageUser(member,
                        "Witaj, zostałeś zbanowany na serwerze " + guild.getName() + " przez bota Guardian. Jeśli chcesz się odwołać, napisz do administracji serwera.");
            }
        }
    }*/





}

package pl.xnik3e.Guardian.components;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import pl.xnik3e.Guardian.GuardianDiscordBot;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;

import java.util.List;


public class ScheduledTask {

    private final FireStoreService fireStoreService;
    private final MessageUtils messageUtils;
    private final JDA jda;
    private final List<String> userIds;

    @Autowired
    public ScheduledTask(MessageUtils messageUtils, GuardianDiscordBot bot) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
        this.jda = bot.getJda();
        userIds = this.fireStoreService.getUserIds();
    }


    //schedule every 10 second
    //@Scheduled(fixedRate = 10 * 1000)
    //schedule every day at 22:46
    //@Scheduled(cron = "0 46 22 * * *")
    public void notifyAllUsers(){
        Guild guild = jda.getGuildById(1112886826559078470L);
        for(String id : userIds){
            User member = guild.getMemberById(id).getUser();
            if(member != null){
                messageUtils.openPrivateChannelAndMessageUser(member,
                        "Witaj, zostałeś zbanowany na serwerze " + guild.getName() + " przez bota Guardian. Jeśli chcesz się odwołać, napisz do administracji serwera.");
            }
        }
    }



}

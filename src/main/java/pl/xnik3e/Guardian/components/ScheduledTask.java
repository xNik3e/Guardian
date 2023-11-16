package pl.xnik3e.Guardian.components;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.xnik3e.Guardian.GuardianDiscordBot;
import pl.xnik3e.Guardian.Models.TempBanModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;

import java.util.List;
@Component
public class ScheduledTask {

    private final FireStoreService fireStoreService;
    private final MessageUtils messageUtils;
    private final JDA jda;
    private final Dotenv config;


    @Autowired
    public ScheduledTask(MessageUtils messageUtils, GuardianDiscordBot bot) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
        this.jda = bot.getJda();
        this.config = Dotenv.configure().load();
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

    //scheduled every 1 minute
    @Scheduled(fixedRate = 60 * 1000)
    public void unbanUsers(){
        Guild guild = jda.getGuildById(config.get("GUILD_ID"));
        Thread thread = new Thread(() -> {
            List<TempBanModel> tempBanModels = fireStoreService.queryBans();
            tempBanModels.forEach(model -> {
                guild.unban(UserSnowflake.fromId(model.getUserId())).queue();
                fireStoreService.deleteBanModel(model.getMessageId());
                MessageChannel logChannel = guild.getChannelById(MessageChannel.class, fireStoreService.getModel().getChannelIdToSendDeletedMessages());
                Message message = logChannel.retrieveMessageById(model.getMessageId()).complete();
                message.delete().queue();
            });
        });
        thread.start();
    }




}

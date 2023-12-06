package pl.xnik3e.Guardian.Components;

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
import java.util.Objects;

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

    @Scheduled(fixedRate = 60 * 1000)
    public void unbanUsers() {
        Guild guild = jda.getGuildById(fireStoreService.getEnvironmentModel().getGUILD_ID());
        new Thread(() -> {
            fireStoreService.queryBans()
                    .forEach(model -> {
                        try {
                            guild.unban(UserSnowflake.fromId(model.getUserId())).queue();
                            fireStoreService.deleteBanModel(model.getMessageId());
                            Objects.requireNonNull(guild.getChannelById(MessageChannel.class,
                                            fireStoreService.getModel().getChannelIdToSendDeletedMessages()))
                                    .retrieveMessageById(model.getMessageId())
                                    .complete()
                                    .delete()
                                    .queue();
                        } catch (Exception e) {
                            System.out.println("Message was not found");
                        }
                    });
        }).start();
        new Thread(() -> fireStoreService.autoDeleteCache(jda, messageUtils)).start();
    }


}

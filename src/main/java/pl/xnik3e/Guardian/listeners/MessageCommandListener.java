package pl.xnik3e.Guardian.listeners;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;

import java.util.List;

@Component
public class MessageCommandListener extends ListenerAdapter {

    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    private final List<String> userIds;

    @Autowired
    public MessageCommandListener(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
        userIds = fireStoreService.getModel().getRolesToDelete();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(messageUtils.checkTrigger(event)){
            //TODO: Add command handling

        }
    }
}


/*
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
    }*/


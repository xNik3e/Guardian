package pl.xnik3e.Guardian.listeners;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.CommandManager;

@Component
public class MessageCommandListener extends ListenerAdapter {

    private final MessageUtils messageUtils;
    private final CommandManager commandManager;
    private final FireStoreService fireStoreService;


    @Autowired
    public MessageCommandListener(MessageUtils messageUtils, CommandManager commandManager) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
        this.commandManager = commandManager;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        User user = event.getAuthor();
        if (user.isBot() || event.isWebhookMessage()) {
            return;
        }

        if (messageUtils.checkTrigger(event)) {
            commandManager.handle(event);
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


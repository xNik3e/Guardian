package pl.xnik3e.Guardian.listeners;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.CommandManager;

import java.util.Objects;

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
            commandManager.handle(event, false);
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getButton().getId();
        switch (Objects.requireNonNull(buttonId)) {
            case "reset":
                fireStoreService.getModel().getDefaultConfig();
                fireStoreService.updateConfigModel();
                event.getMessage().delete().queue();
                messageUtils.openPrivateChannelAndMessageUser(event.getUser(), "Bot has been reset to factory settings");
                event.deferEdit().queue();
                break;
            case "another":
                break;
        }

    }
}




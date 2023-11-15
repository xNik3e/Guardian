package pl.xnik3e.Guardian.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
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

import java.awt.*;
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
            commandManager.handle(event);
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getButton().getId();
        switch (Objects.requireNonNull(buttonId)) {
            case "reset":
                fireStoreService.getModel().getDefaultConfig();
                fireStoreService.updateConfigModel();
                if (!event.getMessage().isEphemeral()) {
                    event.getMessage().delete().queue();
                    messageUtils.openPrivateChannelAndMessageUser(event.getUser(), getMessageEmbed());
                } else {
                    event.getHook().editOriginalComponents().queue();
                    event.getHook().editOriginalEmbeds(getMessageEmbed()).queue();
                }
                event.deferEdit().queue();
                break;
            case "another":
                break;
        }

    }

    @NotNull
    private static MessageEmbed getMessageEmbed() {
        return new EmbedBuilder()
                .setTitle("Reset complete!")
                .setDescription("Bot has been reset to factory settings")
                .setColor(Color.GREEN)
                .build();
    }
}




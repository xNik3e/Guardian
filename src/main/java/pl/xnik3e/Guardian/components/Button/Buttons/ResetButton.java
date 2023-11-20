package pl.xnik3e.Guardian.components.Button.Buttons;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Button.IButton;

import java.awt.*;

public class ResetButton implements IButton {

    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    public ResetButton(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
    }

    @Override
    public void handle(ButtonInteractionEvent event) {
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
    }

    @Override
    public String getValue() {
        return "reset";
    }

    @NotNull
    private  MessageEmbed getMessageEmbed() {
        return new EmbedBuilder()
                .setTitle("Reset complete!")
                .setDescription("Bot has been reset to factory settings")
                .setColor(Color.GREEN)
                .build();
    }
}

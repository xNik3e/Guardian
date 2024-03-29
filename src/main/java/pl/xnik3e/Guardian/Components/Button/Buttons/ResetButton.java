package pl.xnik3e.Guardian.Components.Button.Buttons;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.Components.Button.IButton;

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
        Member member = messageUtils.getMemberFromButtonEvent(event);
        if(!messageUtils.checkAuthority(member)) {
            return;
        }
        event.deferEdit().queue();

        fireStoreService.getModel().getDefaultConfig();
        fireStoreService.updateConfigModel();
        notifyUser(event);
    }

    private void notifyUser(ButtonInteractionEvent event) {
        if (!event.getMessage().isEphemeral()) {
            event.getMessage().delete().queue();
            messageUtils.openPrivateChannelAndMessageUser(event.getUser(), getMessageEmbed());
        } else {
            event.getHook().editOriginalComponents().queue();
            event.getHook().editOriginalEmbeds(getMessageEmbed()).queue();
        }
    }

    @Override
    public String getValue() {
        return "reset";
    }

    @NotNull
    private MessageEmbed getMessageEmbed() {
        return new EmbedBuilder()
                .setTitle("Reset complete!")
                .setDescription("Bot has been reset to factory settings")
                .setColor(Color.GREEN)
                .build();
    }
}

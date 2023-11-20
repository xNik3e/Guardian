package pl.xnik3e.Guardian.components.Button;

import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.springframework.stereotype.Component;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Button.Buttons.ResetButton;
import pl.xnik3e.Guardian.components.Button.Buttons.UnbanButton;
import pl.xnik3e.Guardian.components.Button.Buttons.UserAppealButton;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Component
public class ButtonManager {
    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;
    private final List<IButton> buttons = new ArrayList<>();

    public ButtonManager(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
        addButtons();
    }

    private void addButton(IButton button) {
        boolean nameFound = this.buttons.stream()
                .anyMatch(it -> it.getValue()
                        .equalsIgnoreCase(button.getValue())
                );
        if (nameFound) {
            throw new IllegalArgumentException("Button with this name is already present");
        }
        buttons.add(button);
    }

    public IButton getButton(String search){
        String searchLower = search.toLowerCase();
        for (IButton button : this.buttons) {
            if (button.getValue().equals(searchLower)) {
                return button;
            }
        }
        return null;
    }

    public void handle(ButtonInteractionEvent event, String value){
        String invoke = value.toLowerCase();
        IButton button = this.getButton(invoke);
        if(button == null){
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("Error");
            embed.setDescription("Button interaction failed");
            embed.setColor(Color.RED);
            event.deferReply(true).addEmbeds(embed.build()).queue();
            return;
        }
        new Thread(() -> button.handle(event)).start();
    }

    private void addButtons() {
        addButton(new ResetButton(messageUtils));
        addButton(new UnbanButton(messageUtils));
        addButton(new UserAppealButton(messageUtils));
    }
}

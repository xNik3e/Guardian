package pl.xnik3e.Guardian.Components.Button;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public interface IButton {
    void handle(ButtonInteractionEvent event);
    String getValue();
}

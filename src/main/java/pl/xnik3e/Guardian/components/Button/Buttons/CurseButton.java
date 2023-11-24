package pl.xnik3e.Guardian.components.Button.Buttons;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Button.IButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CurseButton implements IButton {

    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    public CurseButton(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
    }

    @Override
    public void handle(ButtonInteractionEvent event) {
        event.deferReply().queue();
        if(!messageUtils.checkAuthority(event.getMember()))
            return;
        List<String> ids = new ArrayList<>();
        event.getMessage().getEmbeds().get(0).getFields().forEach(field -> {
            ids.add(field.getName());
        });
        messageUtils.banUsers(ids, event.getGuild(), 0, TimeUnit.SECONDS, "Brak roli **kultysta**", false);
    }

    @Override
    public String getValue() {
        return "curse";
    }
}

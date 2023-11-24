package pl.xnik3e.Guardian.components.Button.Buttons;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.Nullable;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Button.IButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CurseButton implements IButton {

    private final MessageUtils messageUtils;

    public CurseButton(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
    }

    @Override
    public void handle(ButtonInteractionEvent event) {
        event.deferReply().queue();
        Member member = messageUtils.getMemberFromButtonEvent(event);
        if(!messageUtils.checkAuthority(member))
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

package pl.xnik3e.Guardian.Components.Button.Buttons;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import pl.xnik3e.Guardian.Models.CurseModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.Components.Button.IButton;

import java.awt.*;
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
        if(!messageUtils.checkAuthority(event.getMember()))
            return;
        event.deferEdit().queue();
        MessageEditBuilder builder = new MessageEditBuilder();
        EmbedBuilder eBuilder = new EmbedBuilder();
        List<String> ids = new ArrayList<>();
        fireStoreService.fetchAllCache(CurseModel.class).ifPresentOrElse(model -> {
            model.getMaps().forEach(map -> {
                ids.add(map.get("userID"));
            });
            eBuilder.setTitle("Curse");
            eBuilder.setDescription("All the evil spirits have been sanctified and the curse has been lifted");
            eBuilder.setColor(Color.GREEN);
            messageUtils.banUsers(ids, event.getGuild(), 0, TimeUnit.SECONDS, "Brak roli **kultysta**", false);
            fireStoreService.deleteCacheUntilNow(CurseModel.class);
            MessageCreateBuilder createBuilder = new MessageCreateBuilder();
            createBuilder.setEmbeds(eBuilder.build());
            builder.applyCreateData(createBuilder.build());
            event.getHook().editOriginal(builder.build()).queue();
        }, () -> {
            eBuilder.setTitle("Error");
            eBuilder.setDescription("Data was not found");
            eBuilder.setColor(Color.RED);
            builder.setEmbeds(eBuilder.build());
            event.getHook().editOriginal(builder.build()).queue();
        });
    }


    @Override
    public String getValue() {
        return "curse";
    }
}

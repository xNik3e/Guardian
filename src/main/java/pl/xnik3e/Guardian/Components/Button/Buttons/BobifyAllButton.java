package pl.xnik3e.Guardian.Components.Button.Buttons;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import pl.xnik3e.Guardian.Models.NickNameModel;
import pl.xnik3e.Guardian.Models.ToBobifyMembersModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.Components.Button.IButton;

import java.awt.*;

public class BobifyAllButton implements IButton {

    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    public BobifyAllButton(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
    }

    @Override
    public void handle(ButtonInteractionEvent event) {
        if(!messageUtils.checkAuthority(event.getMember()))
            return;
        event.deferEdit().queue();
        if (messageUtils.checkAuthority(event.getMember())) {
            fireStoreService.fetchAllCache(ToBobifyMembersModel.class).ifPresentOrElse(model -> {
                model.getMaps().forEach(map -> {
                    String UID = map.get("userID");
                    event.getGuild().retrieveMemberById(UID).queue(member -> {
                        if (messageUtils.checkAuthority(member))
                            return;
                        NickNameModel nickNameModel = fireStoreService.fetchNickNameModel(member.getId());
                        if (nickNameModel != null) {
                            nickNameModel.getNickName().remove(member.getEffectiveName());
                            fireStoreService.updateNickModel(nickNameModel);
                        }
                        messageUtils.bobify(member);
                    });
                });
                fireStoreService.deleteCacheUntilNow(ToBobifyMembersModel.class);
                event.getHook().editOriginalComponents().queue();
                event.getHook().editOriginalEmbeds(new EmbedBuilder()
                        .setTitle("Success")
                        .setDescription("Bobified all users")
                        .setColor(Color.GREEN)
                        .build()).queue();
            }, () -> {
                event.getHook().editOriginalComponents().queue();
                event.getHook().editOriginalEmbeds(new EmbedBuilder().setTitle("Error")
                        .setDescription("Data was not found")
                        .setColor(Color.RED).build()).queue();
            });
        }
    }

    @Override
    public String getValue() {
        return "bobifyall";
    }
}

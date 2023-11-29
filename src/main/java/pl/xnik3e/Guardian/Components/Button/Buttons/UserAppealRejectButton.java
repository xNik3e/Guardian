package pl.xnik3e.Guardian.Components.Button.Buttons;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import pl.xnik3e.Guardian.Models.NickNameModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.Components.Button.IButton;

import java.awt.*;

public class UserAppealRejectButton implements IButton {

    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    public UserAppealRejectButton(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
    }

    @Override
    public void handle(ButtonInteractionEvent event) {
        if(!messageUtils.checkAuthority(event.getMember()))
            return;
        event.deferEdit().queue();
        if(messageUtils.checkAuthority(event.getMember())){
            NickNameModel rejectedNickNameModel = new NickNameModel();
            Message message = event.getMessage();
            message.getEmbeds().get(0).getFields().forEach(field -> {
                if (field.getName().equals("Nick"))
                    rejectedNickNameModel.getNickName().add(field.getValue());
                else if (field.getName().equals("UserID"))
                    rejectedNickNameModel.setUserID(field.getValue());
            });
            event.getGuild().retrieveMemberById(rejectedNickNameModel.getUserID()).queue(member -> {
                event.getHook().editOriginalComponents().queue();
                event.getHook().editOriginalEmbeds(new EmbedBuilder()
                        .setTitle("Odwołanie odrzucone")
                        .setDescription("Odwołanie o zmianę nicku dla użytkownika " +
                                member.getAsMention() + " zostało odrzucone")
                        .addField("Poprzedni nick", rejectedNickNameModel.getNickName().get(0), false)
                        .setColor(Color.RED)
                        .build()).queue();

                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Odwołanie automatycznej zmiany nicku");
                embedBuilder.setDescription("Twoje odwołanie zostało odrzucone");
                embedBuilder.setColor(Color.RED);
                messageUtils.openPrivateChannelAndMessageUser(member.getUser(), embedBuilder.build());
            });
        }
    }

    @Override
    public String getValue() {
        return "rejectAppeal";
    }
}

package pl.xnik3e.Guardian.Components.Button.Buttons;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;
import pl.xnik3e.Guardian.Models.NickNameModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.Components.Button.IButton;

import java.awt.*;
import java.util.Objects;

public class UserAppealRejectButton implements IButton {

    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    public UserAppealRejectButton(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
    }

    @Override
    public void handle(ButtonInteractionEvent event) {
        if (!messageUtils.checkAuthority(event.getMember()))
            return;

        event.deferEdit().queue();
        NickNameModel rejectedNickNameModel = exctractUserFromEmbed(event);
        Objects.requireNonNull(event.getGuild())
                .retrieveMemberById(rejectedNickNameModel.getUserID())
                .queue(member -> {
                    editOriginalMessage(event, member, rejectedNickNameModel);
                    notifyUser(member);
                });
    }

    private void notifyUser(Member member) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Odwołanie automatycznej zmiany nicku");
        embedBuilder.setDescription("Twoje odwołanie zostało odrzucone");
        embedBuilder.setColor(Color.RED);
        messageUtils.openPrivateChannelAndMessageUser(member.getUser(), embedBuilder.build());
    }

    private static void editOriginalMessage(ButtonInteractionEvent event, Member member, NickNameModel rejectedNickNameModel) {
        event.getHook().editOriginalComponents().queue();
        event.getHook().editOriginalEmbeds(new EmbedBuilder()
                .setTitle("Odwołanie odrzucone")
                .setDescription("Odwołanie o zmianę nicku dla użytkownika " +
                        member.getAsMention() + " zostało odrzucone")
                .addField("Poprzedni nick", rejectedNickNameModel.getNickName().get(0), false)
                .setColor(Color.RED)
                .build()).queue();
    }

    @NotNull
    private static NickNameModel exctractUserFromEmbed(ButtonInteractionEvent event) {
        NickNameModel rejectedNickNameModel = new NickNameModel();
        Message message = event.getMessage();
        message.getEmbeds().get(0).getFields().forEach(field -> {
            if (field.getName().equals("Nick"))
                rejectedNickNameModel.getNickName().add(field.getValue());
            else if (field.getName().equals("UserID"))
                rejectedNickNameModel.setUserID(field.getValue());
        });
        return rejectedNickNameModel;
    }

    @Override
    public String getValue() {
        return "rejectAppeal";
    }
}

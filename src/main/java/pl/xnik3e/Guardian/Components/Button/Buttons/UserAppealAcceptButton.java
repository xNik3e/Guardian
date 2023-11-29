package pl.xnik3e.Guardian.Components.Button.Buttons;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import pl.xnik3e.Guardian.Models.NickNameModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.Components.Button.IButton;

import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

public class UserAppealAcceptButton implements IButton
{
    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    public UserAppealAcceptButton(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
    }

    @Override
    public void handle(ButtonInteractionEvent event) {
        if(!messageUtils.checkAuthority(event.getMember()))
            return;
        event.deferEdit().queue();
        if(messageUtils.checkAuthority(event.getMember())){
            NickNameModel nickNameModel = new NickNameModel();
            Message message = event.getMessage();
            message.getEmbeds().get(0).getFields().forEach(field -> {
                if (field.getName().equals("Nick"))
                    nickNameModel.getNickName().add(field.getValue());
                else if (field.getName().equals("UserID"))
                    nickNameModel.setUserID(field.getValue());
            });
            fireStoreService.updateNickModel(nickNameModel);
            event.getGuild().retrieveMemberById(nickNameModel.getUserID()).queue(member -> {
                member.modifyNickname(nickNameModel.getNickName().get(0)).queue();
                event.getHook().editOriginalComponents().queue();
                AtomicInteger index = new AtomicInteger(1);
                event.getHook().editOriginalEmbeds(new EmbedBuilder()
                        .setTitle("Odwołanie przyjęte")
                        .setDescription("Odwołanie o zmianę nicku dla użytkownika " +
                                member.getAsMention() + " zostało rozpatrzone pozytywnie\n" +
                                "Nick został przywrócony")
                        .addField("Whitelista użytkownika",
                                fireStoreService.getWhitelistedNicknames(nickNameModel.getUserID())
                                        .stream()
                                        .map(nick -> index.getAndIncrement() + ". " + nick + "\n")
                                        .reduce("", String::concat)
                                , false)
                        .addField("Usuń nick z whitelisty",
                                "Aby usunąć nick z whitelisty użyj komendy:\n**" + fireStoreService.getModel().getPrefix()
                                        + "blacklist <@Member> <Nick index>**", false)
                        .addField("Wyświetl nicki z whitelisty",
                                "Aby wyświetlić nicki z whitelisty użyj komendy:\n**" + fireStoreService.getModel().getPrefix()
                                        + "whitelist <@Member>**", false)
                        .setColor(Color.GREEN).build()).queue();

                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Odwołanie automatycznej zmiany nicku");
                embedBuilder.setDescription("Twój nick został przywrócony");
                embedBuilder.setColor(Color.GREEN);
                messageUtils.openPrivateChannelAndMessageUser(member.getUser(), embedBuilder.build());
            });
        }
    }

    @Override
    public String getValue() {
        return "acceptAppeal";
    }
}

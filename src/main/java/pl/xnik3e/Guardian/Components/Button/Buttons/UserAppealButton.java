package pl.xnik3e.Guardian.Components.Button.Buttons;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.Components.Button.IButton;

import java.awt.*;

public class UserAppealButton implements IButton {

    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    public UserAppealButton(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
    }

    @Override
    public void handle(ButtonInteractionEvent event) {
        event.deferEdit().queue();
        Message message = event.getMessage();
        String previousNick = message.getEmbeds().get(0).getFields().get(0).getValue();
        User user = event.getUser();
        event.getHook().editOriginalComponents().queue();
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Odwołanie");
        embedBuilder.addField("Nick", previousNick, false);
        embedBuilder.addField("UserID", user.getId(), false);
        embedBuilder.setDescription("Twoje odwołanie zostało wysłane do administracji. Odpowiedź otrzymasz w wiadomości prywatnej.\n" +
                "Jeżeli odwołanie zostanie przyjęte przywrócimy Twój nick.\n" +
                "Za utrudnienia przepraszamy.");
        embedBuilder.setColor(Color.PINK);
        event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();
        MessageChannel logChannel = event.getJDA().getGuildById(fireStoreService.getEnvironmentModel().getGUILD_ID())
                .getChannelById(MessageChannel.class, fireStoreService.getModel().getChannelIdToSendDeletedMessages());
        if (logChannel != null) {
            embedBuilder.setTitle("Odwołanie automatycznej zmiany nicku");
            embedBuilder.addField("User", user.getAsMention(), false);
            embedBuilder.setDescription("Przeprowadzono automatyczną edycję nicku i użytkownik się odwołał\n" +
                    "Wybierz opcję poniżej");
            embedBuilder.setColor(Color.BLUE);
            net.dv8tion.jda.api.interactions.components.buttons.Button buttonAccept = net.dv8tion.jda.api.interactions.components.buttons.Button.primary("acceptAppeal", "Akceptuj");
            net.dv8tion.jda.api.interactions.components.buttons.Button buttonReject = Button.danger("rejectAppeal", "Odrzuć");
            MessageCreateData data = new MessageCreateBuilder().setEmbeds(embedBuilder.build()).setActionRow(buttonAccept, buttonReject).build();
            logChannel.sendMessage(data).queue();
        }
    }

    @Override
    public String getValue() {
        return "appeal";
    }
}

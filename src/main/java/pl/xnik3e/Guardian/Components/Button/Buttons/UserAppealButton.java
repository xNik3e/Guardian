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
import java.util.Objects;

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
        editOriginalMessage(event, previousNick);

        MessageChannel logChannel = Objects.requireNonNull(event.getJDA().getGuildById(fireStoreService.getEnvironmentModel().getGUILD_ID()))
                .getChannelById(MessageChannel.class, fireStoreService.getModel().getChannelIdToSendDeletedMessages());
        if (logChannel != null) {
            sendMessageToMods(event, logChannel);
        }
    }

    private static void sendMessageToMods(ButtonInteractionEvent event, MessageChannel logChannel) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Odwołanie automatycznej zmiany nicku");
        embedBuilder.addField("User", event.getUser().getAsMention(), false);
        embedBuilder.setDescription("Przeprowadzono automatyczną edycję nicku i użytkownik się odwołał\n" +
                "Wybierz opcję poniżej");
        embedBuilder.setColor(Color.BLUE);
        Button buttonAccept = Button.primary("acceptAppeal", "Akceptuj");
        Button buttonReject = Button.danger("rejectAppeal", "Odrzuć");
        MessageCreateData data = new MessageCreateBuilder()
                .setEmbeds(embedBuilder.build())
                .setActionRow(buttonAccept, buttonReject)
                .build();
        logChannel.sendMessage(data).queue();
    }

    private static void editOriginalMessage(ButtonInteractionEvent event, String previousNick) {
        event.getHook().editOriginalComponents().queue();
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Odwołanie");
        embedBuilder.addField("Nick", previousNick, false);
        embedBuilder.addField("UserID", event.getUser().getId(), false);
        embedBuilder.setDescription("Twoje odwołanie zostało wysłane do administracji. Odpowiedź otrzymasz w wiadomości prywatnej.\n" +
                "Jeżeli odwołanie zostanie przyjęte przywrócimy Twój nick.\n" +
                "Za utrudnienia przepraszamy.");
        embedBuilder.setColor(Color.PINK);
        event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();
    }

    @Override
    public String getValue() {
        return "appeal";
    }
}

package pl.xnik3e.Guardian.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.xnik3e.Guardian.Models.NickNameModel;
import pl.xnik3e.Guardian.Models.TempBanModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.CommandManager;

import java.awt.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class MessageCommandListener extends ListenerAdapter {

    private final MessageUtils messageUtils;
    private final CommandManager commandManager;
    private final FireStoreService fireStoreService;


    @Autowired
    public MessageCommandListener(MessageUtils messageUtils, CommandManager commandManager) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
        this.commandManager = commandManager;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        User user = event.getAuthor();
        if (user.isBot() || event.isWebhookMessage()) {
            return;
        }

        if (messageUtils.checkTrigger(event)) {
            commandManager.handle(event);
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getButton().getId();
        switch (Objects.requireNonNull(buttonId)) {
            case "reset":
                fireStoreService.getModel().getDefaultConfig();
                fireStoreService.updateConfigModel();
                if (!event.getMessage().isEphemeral()) {
                    event.getMessage().delete().queue();
                    messageUtils.openPrivateChannelAndMessageUser(event.getUser(), getMessageEmbed());
                } else {
                    event.getHook().editOriginalComponents().queue();
                    event.getHook().editOriginalEmbeds(getMessageEmbed()).queue();
                }
                event.deferEdit().queue();
                break;
            case "unban":
                String messageId = event.getMessageId();
                Message message = event.getMessage();
                event.deferEdit().queue();
                Thread thread = new Thread(() -> {
                    TempBanModel tempBanModel = fireStoreService.fetchBanModel(messageId);
                    if (tempBanModel != null) {
                        event.getGuild().unban(UserSnowflake.fromId(tempBanModel.getUserId())).queue(s -> {
                            message.delete().queue();
                            fireStoreService.deleteBanModel(messageId);
                            MessageChannel logChannel = event.getGuild().getChannelById(MessageChannel.class, fireStoreService.getModel().getChannelIdToSendLog());
                            if (logChannel != null) {
                                logChannel.sendMessage("User " + tempBanModel.getUserId() + " has been unbanned").queue();
                            }
                        }, f -> {
                            messageUtils.openPrivateChannelAndMessageUser(event.getUser(), "Something went wrong");
                        });
                    }
                });
                thread.start();
                break;
            case "appeal":
                String messageId1 = event.getMessageId();
                Message message1 = event.getMessage();
                String previousNick = message1.getEmbeds().get(0).getFields().get(0).getValue();
                User user = event.getUser();
                event.deferEdit().queue();
                Thread thread1 = new Thread(() -> {
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
                            .getChannelById(MessageChannel.class, fireStoreService.getModel().getChannelIdToSendLog());
                    if (logChannel != null) {
                        embedBuilder.setTitle("Odwołanie automatycznej zmiany nicku");
                        embedBuilder.addField("User", user.getAsMention(), false);
                        embedBuilder.setDescription("Przeprowadzono automatyczną edycję nicku i użytkownik się odwołał\n" +
                                "Wybierz opcję poniżej");
                        embedBuilder.setColor(Color.BLUE);
                        Button buttonAccept = Button.primary("acceptAppeal", "Akceptuj");
                        Button buttonReject = Button.danger("rejectAppeal", "Odrzuć");
                        MessageCreateData data = new MessageCreateBuilder().setEmbeds(embedBuilder.build()).setActionRow(buttonAccept, buttonReject).build();
                        logChannel.sendMessage(data).queue();
                    }
                });
                thread1.start();
                break;
            case "acceptAppeal":
                NickNameModel nickNameModel = new NickNameModel();
                event.deferEdit().queue();
                Message message2 = event.getMessage();
                message2.getEmbeds().get(0).getFields().forEach(field -> {
                    if (field.getName().equals("Nick"))
                        nickNameModel.getNickName().add(field.getValue());
                    else if (field.getName().equals("UserID"))
                        nickNameModel.setUserID(field.getValue());
                });
                fireStoreService.updateNickModel(nickNameModel);
                event.getGuild().retrieveMemberById(nickNameModel.getUserID()).queue(member -> {
                    member.modifyNickname(nickNameModel.getNickName().get(0)).queue();
                    event.getHook().editOriginalComponents().queue();
                    event.getHook().editOriginalEmbeds(new EmbedBuilder()
                            .setTitle("Odwołanie przyjęte")
                            .setDescription("Odwołanie o zmianę nicku dla użytkownika " +
                                    member.getAsMention() + " zostało rozpatrzone pozytywnie\n" +
                                    "Nick został przywrócony")
                            .setColor(Color.GREEN)
                            .build()).queue();

                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.setTitle("Odwołanie automatycznej zmiany nicku");
                    embedBuilder.setDescription("Twój nick został przywrócony");
                    embedBuilder.setColor(Color.GREEN);
                    messageUtils.openPrivateChannelAndMessageUser(member.getUser(), embedBuilder.build());
                });
                break;
            case "rejectAppeal":
                NickNameModel rejectedNickNameModel = new NickNameModel();
                event.deferEdit().queue();
                Message message3 = event.getMessage();
                message3.getEmbeds().get(0).getFields().forEach(field -> {
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
                break;
        }

    }

    @NotNull
    private static MessageEmbed getMessageEmbed() {
        return new EmbedBuilder()
                .setTitle("Reset complete!")
                .setDescription("Bot has been reset to factory settings")
                .setColor(Color.GREEN)
                .build();
    }
}




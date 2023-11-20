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
import pl.xnik3e.Guardian.components.Button.ButtonManager;
import pl.xnik3e.Guardian.components.Command.CommandManager;

import java.util.List;
import java.awt.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MessageCommandListener extends ListenerAdapter {

    private final MessageUtils messageUtils;
    private final CommandManager commandManager;
    private final FireStoreService fireStoreService;
    private final ButtonManager buttonManager;


    @Autowired
    public MessageCommandListener(MessageUtils messageUtils, CommandManager commandManager, ButtonManager buttonManager) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
        this.commandManager = commandManager;
        this.buttonManager = buttonManager;
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
        buttonManager.handle(event, Objects.requireNonNull(buttonId));
        switch (Objects.requireNonNull(buttonId)) {
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
                    AtomicInteger index = new AtomicInteger(1);
                    List<String> whitelistedNicknames = fireStoreService.getWhitelistedNicknames(nickNameModel.getUserID());
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
            case "bobifyall":
                event.deferEdit().queue();
                Message message4 = event.getMessage();
                MessageEmbed embed = message4.getEmbeds().get(0);
                embed.getFields().forEach(field -> {
                    String UID = field.getValue();
                    event.getGuild().retrieveMemberById(UID).queue(member ->{
                        NickNameModel model = fireStoreService.getNickNameModel(member.getId());
                        if(model != null){
                            model.getNickName().remove(member.getEffectiveName());
                            fireStoreService.updateNickModel(model);
                        }
                        messageUtils.bobify(member);
                    });
                });
                event.getHook().editOriginalComponents().queue();
                event.getHook().editOriginalEmbeds(new EmbedBuilder()
                        .setTitle("Success")
                        .setDescription("Bobified all users")
                        .setColor(Color.GREEN)
                        .build()).queue();
            break;
        }

    }
}




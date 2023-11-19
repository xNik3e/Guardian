package pl.xnik3e.Guardian.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.member.GenericGuildMemberEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;

import java.awt.*;


@Component
public class BobNicknameChangeListener extends ListenerAdapter {
    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    @Autowired
    public BobNicknameChangeListener(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
    }

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        checkMention(event);
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        checkMention(event);
    }

    @Override
    public void onGuildMemberUpdateNickname(@NotNull GuildMemberUpdateNicknameEvent event) {
        checkMention(event);
    }

    private void checkMention(@NotNull GenericGuildMemberEvent event) {
        Member member = event.getMember();
        boolean hasMentionableNick = messageUtils.hasMentionableNickName(member);
        if(!hasMentionableNick){
            changeNickName(member);
        }
    }

    private void changeNickName(Member member){
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Nieoznaczalny nick");
        embedBuilder.addField("Nieoznaczalny nick", member.getEffectiveName(), false);
        embedBuilder.setDescription("No cześć! Zdaje mi się, że Twój nick - **" + member.getEffectiveName() +"** - nie jest oznaczalny.\n" +
                "Według punktu 5. regulaminu serwera, musisz zmienić swój nick.\n" +
                "Na ten moment nazywasz się **BOB**. Jeżeli Ci to pasuje - zajebiście, będziemy się tak do Ciebie zwracać.\n"
                + "Jeżeli jednak nie chcesz zostać do końca swojego życia Bobem, możesz w każdej chwili zmienić swój nick.\n");
        embedBuilder.setColor(Color.PINK);
        Button button = Button.primary("appeal", "Odwołaj się");
        MessageCreateData data = new MessageCreateBuilder().setEmbeds(embedBuilder.build()).setActionRow(button).build();
        try{
            member.modifyNickname("Bob").queue();
            messageUtils.openPrivateChannelAndMessageUser(member.getUser(), data);
        }catch(Exception e){
            System.err.println("User is higher in hierarchy than bot");
        }
    }
}

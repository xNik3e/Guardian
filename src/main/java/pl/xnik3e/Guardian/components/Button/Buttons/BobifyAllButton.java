package pl.xnik3e.Guardian.components.Button.Buttons;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import pl.xnik3e.Guardian.Models.NickNameModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Button.IButton;

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
        event.deferEdit().queue();
        Message message = event.getMessage();
        MessageEmbed embed = message.getEmbeds().get(0);
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
    }

    @Override
    public String getValue() {
        return "bobifyall";
    }
}

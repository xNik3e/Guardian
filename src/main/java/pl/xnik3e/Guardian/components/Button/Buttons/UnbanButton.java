package pl.xnik3e.Guardian.components.Button.Buttons;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import pl.xnik3e.Guardian.Models.TempBanModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Button.IButton;

public class UnbanButton implements IButton {

    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    public UnbanButton(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
    }

    @Override
    public void handle(ButtonInteractionEvent event) {
        String messageId = event.getMessageId();
        Message message = event.getMessage();
        event.deferEdit().queue();
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
    }

    @Override
    public String getValue() {
        return "unban";
    }
}

package pl.xnik3e.Guardian.components.Command.Commands;

import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.CommandContext;
import pl.xnik3e.Guardian.components.Command.ICommand;

import java.util.List;

public class ToggleMentionCommand implements ICommand {

    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    public ToggleMentionCommand(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
    }

    @Override
    public void handle(CommandContext ctx) {
        ctx.getMessage().delete().queue();
        fireStoreService.getModel().setRespondByPrefix(false);
        fireStoreService.updateConfigModel();
        messageUtils.openPrivateChannelAndMessageUser(ctx.getMember().getUser(),
                "Bot is now responding by: **mention**");
    }

    @Override
    public String getName() {
        return "togglemention";
    }

    @Override
    public String getHelp() {
        return "Set bot to respond by mention";
    }

    @Override
    public boolean isAfterInit() {
        return false;
    }

    @Override
    public List<String> getAliases() {
        return List.of("mention", "m", "setmention", "changemention");
    }
}

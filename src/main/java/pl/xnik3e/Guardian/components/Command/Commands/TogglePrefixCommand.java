package pl.xnik3e.Guardian.components.Command.Commands;

import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.CommandContext;
import pl.xnik3e.Guardian.components.Command.ICommand;

import java.util.List;

public class TogglePrefixCommand implements ICommand {

    public final FireStoreService fireStoreService;
    public final MessageUtils messageUtils;

    public TogglePrefixCommand(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
    }

    @Override
    public void handle(CommandContext ctx) {
        ctx.getMessage().delete().queue();
        fireStoreService.getModel().setRespondByPrefix(true);
        fireStoreService.updateConfigModel();
        messageUtils.openPrivateChannelAndMessageUser(ctx.getMember().getUser(),
                "Bot is now responding by: **prefix**");

    }

    @Override
    public String getName() {
        return "toggleprefix";
    }

    @Override
    public String getHelp() {
        return "Set bot to respond by prefix";
    }

    @Override
    public boolean isAfterInit() {
        return false;
    }

    @Override
    public List<String> getAliases() {
        return List.of("prefix", "p", "setprefix", "changeprefix");
    }
}

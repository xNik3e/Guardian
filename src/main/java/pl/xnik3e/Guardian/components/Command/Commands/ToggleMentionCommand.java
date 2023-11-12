package pl.xnik3e.Guardian.components.Command.Commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
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
    public MessageEmbed getHelp() {
        EmbedBuilder eBuilder = new EmbedBuilder();
        eBuilder.setTitle("Toggle mention");
        eBuilder.setDescription("Set bot to respond by mention\n");
        eBuilder.addField("Usage", "{prefix} togglemention", false);
        eBuilder.addField("Example usage", fireStoreService.getModel().getPrefix() + "togglemention", false);
        eBuilder.addField("Available aliases", "mention, m, setmention, changemention", false);
        return eBuilder.build();
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

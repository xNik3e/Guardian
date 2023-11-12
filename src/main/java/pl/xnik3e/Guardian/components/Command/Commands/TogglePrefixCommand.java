package pl.xnik3e.Guardian.components.Command.Commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
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
        List<String> args = ctx.getArgs();
        if(!args.isEmpty()){
            String prefix = args.get(0);
            fireStoreService.getModel().setPrefix(prefix);
        }

        fireStoreService.updateConfigModel();
        messageUtils.openPrivateChannelAndMessageUser(ctx.getMember().getUser(),
                "Bot is now responding by: **prefix**\n" +
                        "Current value for prefix: "+ "`" + fireStoreService.getModel().getPrefix() + "`");

    }

    @Override
    public String getName() {
        return "toggleprefix";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder eBuilder = new EmbedBuilder();
        eBuilder.setTitle("Toggle prefix");
        eBuilder.setDescription("Set bot to respond by prefix\n");
        eBuilder.addField("Usage", "{prefix} toggleprefix {optional <prefix>}", false);
        eBuilder.addField("Example usage", fireStoreService.getModel().getPrefix() + "toggleprefix !", false);
        eBuilder.addField("Available aliases", "prefix, p, setprefix, changeprefix", false);
        return eBuilder.build();
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

package pl.xnik3e.Guardian.components.Command.Commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.CommandContext;
import pl.xnik3e.Guardian.components.Command.ICommand;

import java.awt.*;
import java.util.List;

public class DeleteTriggerCommand implements ICommand {

    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    public DeleteTriggerCommand(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
    }

    @Override
    public void handle(CommandContext ctx) {
        boolean deleteTriggerMessage = fireStoreService.getModel().isDeleteTriggerMessage();
        if(!deleteTriggerMessage)
            ctx.getMessage().delete().queue();

        fireStoreService.getModel().setDeleteTriggerMessage(!deleteTriggerMessage);
        fireStoreService.updateConfigModel();

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Hello there!");
        embedBuilder.setDescription("From now, I " + (!deleteTriggerMessage ? "will " : "won't " ) + "delete trigger messages");
        embedBuilder.setColor(Color.GREEN);
        messageUtils.respondToUser(ctx, embedBuilder.build());

    }

    @Override
    public String getName() {
        return "deletetrigger";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(getTitle());
        embedBuilder.setDescription(getDescription());
        embedBuilder.addField("Usage", "`{prefix or mention} deletetrigger`", false);
        embedBuilder.addField("Example usage", "`" + fireStoreService.getModel().getPrefix()  + "deletetrigger`", false);
        embedBuilder.addField("Available aliases", "`dt`, `removetrigger`, `rt`", false);
        embedBuilder.setColor((int)(Math.random() * 0x1000000));
        return embedBuilder.build();
    }

    @Override
    public String getDescription() {
        return "Change if the bot should delete trigger command or not";
    }

    @Override
    public String getTitle() {
        return "Delete trigger";
    }

    @Override
    public boolean isAfterInit() {
        return false;
    }

    @Override
    public List<String> getAliases() {
        return List.of("dt", "removetrigger", "rt");
    }
}

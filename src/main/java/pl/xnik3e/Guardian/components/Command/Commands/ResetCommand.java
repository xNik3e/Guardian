package pl.xnik3e.Guardian.components.Command.Commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.CommandContext;
import pl.xnik3e.Guardian.components.Command.ICommand;

import java.util.List;

public class ResetCommand implements ICommand {

    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    public ResetCommand(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
    }

    @Override
    public void handle(CommandContext ctx) {
        ctx.getMessage().delete().queue();
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Hello there!");
        embedBuilder.setDescription("You're about to reset the bot to factory settings. Are you sure?");
        embedBuilder.addField("Warning", "This action is **irreversible!**", false);
        Button button = Button.danger("reset", "Reset");

        MessageCreateData message = new MessageCreateBuilder().setEmbeds(embedBuilder.build()).setActionRow(button).build();
        messageUtils.respondToUser(ctx, message);



    }

    @Override
    public String getName() {
        return "reset";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(getTitle());
        embedBuilder.setDescription(getDescription());
        embedBuilder.addField("Usage", "`{prefix or mention} reset`", false);
        embedBuilder.addField("Example usage", "`" + fireStoreService.getModel().getPrefix() + "reset`", false);
        embedBuilder.addField("Available aliases", "`resetbot`, `usedefaults`, `factoryreset`, `rollback`, `r`", false);
        return embedBuilder.build();
    }

    @Override
    public String getDescription() {
        return "Command used to return to factory settings. It will delete all data from the database and reset the bot";
    }

    @Override
    public String getTitle() {
        return "Reset bot settings";
    }

    @Override
    public boolean isAfterInit() {
        return false;
    }

    @Override
    public List<String> getAliases() {
        return List.of("resetbot", "usedefaults", "factoryreset", "rollback", "r");
    }
}

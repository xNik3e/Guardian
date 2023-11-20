package pl.xnik3e.Guardian.components.Command.Commands.ConfigCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.CommandContext;
import pl.xnik3e.Guardian.components.Command.ICommand;

import java.awt.*;
import java.util.List;

public class ToggleBotResponseCommand implements ICommand {

    private final MessageUtils messageUtils;
    private final FireStoreService firestoreService;

    public ToggleBotResponseCommand(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.firestoreService = messageUtils.getFireStoreService();
    }

    @Override
    public void handle(CommandContext ctx) {
        boolean deleteTriggerMessage = firestoreService.getModel().isDeleteTriggerMessage();
        if(deleteTriggerMessage)
            ctx.getMessage().delete().queue();
        EmbedBuilder embedBuilder = getEmbedBuilder();
        messageUtils.respondToUser(ctx, embedBuilder.build());
    }

    @Override
    public void handleSlash(SlashCommandInteractionEvent event, List<String> args) {
        EmbedBuilder embedBuilder = getEmbedBuilder();
        event.getHook().sendMessageEmbeds(embedBuilder.build()).setEphemeral(true).queue();
    }

    @Override
    public String getName() {
        return "togglebotresponse";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(getTitle());
        embedBuilder.setDescription(getDescription());
        embedBuilder.addField("Usage", "`{prefix or mention} togglebotresponse`", false);
        embedBuilder.addField("Example usage", "`" + firestoreService.getModel().getPrefix() + "togglebotresponse`", false);
        embedBuilder.addField("Available aliases", messageUtils.createAliasString(getAliases()), false);
        Color color = new Color((int)(Math.random() * 0x1000000));
        embedBuilder.setColor(color);
        return embedBuilder.build();
    }

    @Override
    public String getDescription() {
        return "Switch between bot responding to commands by private message or by replying to the command message.";
    }

    @Override
    public String getTitle() {
        return "Toggle Bot Response";
    }

    @Override
    public boolean isAfterInit() {
        return false;
    }

    @Override
    public List<String> getAliases() {
        return List.of("tbr", "toggleresponse", "changeresponse", "changereply", "togglereply");
    }

    @NotNull
    private EmbedBuilder getEmbedBuilder() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        boolean respondInDirect = !firestoreService.getModel().isRespondInDirectMessage();
        firestoreService.getModel().setRespondInDirectMessage(respondInDirect);
        firestoreService.updateConfigModel();
        embedBuilder.setTitle("Hello there!");
        embedBuilder.setDescription("I will now respond back to you by: **" + (respondInDirect ? "direct message" : "message reply") + "**");
        embedBuilder.setColor(Color.GREEN);
        return embedBuilder;
    }
}

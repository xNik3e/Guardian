package pl.xnik3e.Guardian.Components.Command.Commands.ConfigCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.Components.Command.CommandContext;
import pl.xnik3e.Guardian.Components.Command.ICommand;

import java.awt.*;
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
        messageUtils.deleteTrigger(ctx);
        messageUtils.respondToUser(ctx, getEmbedBuilder().build());
    }

    @Override
    public void handleSlash(SlashCommandInteractionEvent event, List<String> args) {
        event.getHook()
                .sendMessageEmbeds(getEmbedBuilder().build())
                .setEphemeral(true)
                .queue();
    }

    @Override
    public String getName() {
        return "togglemention";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder eBuilder = new EmbedBuilder();
        eBuilder.setTitle(getTitle());
        eBuilder.setDescription(getDescription());
        eBuilder.addField("Usage", "`{prefix} togglemention`", false);
        eBuilder.addField("Example usage", "`" +fireStoreService.getModel().getPrefix() + "togglemention`", false);
        eBuilder.addField("Available aliases", messageUtils.createAliasString(getAliases()), false);
        Color color = new Color((int)(Math.random() * 0x1000000));
        eBuilder.setColor(color);
        return eBuilder.build();
    }

    @Override
    public String getDescription() {
        return "Set bot to respond by mention";
    }

    @Override
    public String getTitle() {
        return "Toggle mention";
    }

    @Override
    public boolean isAfterInit() {
        return false;
    }

    @Override
    public List<String> getAliases() {
        return List.of("mention", "m", "setmention", "changemention");
    }

    @NotNull
    private EmbedBuilder getEmbedBuilder() {
        fireStoreService.getModel().setRespondByPrefix(false);
        fireStoreService.updateConfigModel();
        EmbedBuilder eBuilder = new EmbedBuilder();
        eBuilder.setTitle("Respond by mention");
        eBuilder.setDescription("Bot is now responding by: **mention**");
        return eBuilder;
    }
}

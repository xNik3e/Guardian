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

public class TogglePrefixCommand implements ICommand {

    public final FireStoreService fireStoreService;
    public final MessageUtils messageUtils;

    public TogglePrefixCommand(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
    }

    @Override
    public void handle(CommandContext ctx) {
        messageUtils.deleteTrigger(ctx);
        messageUtils.respondToUser(ctx,
                getEmbedBuilder(ctx.getArgs())
                        .build());
    }

    @Override
    public void handleSlash(SlashCommandInteractionEvent event, List<String> args) {
        EmbedBuilder eBuilder = getEmbedBuilder(args);
        event.getHook()
                .sendMessageEmbeds(eBuilder.build())
                .setEphemeral(true)
                .queue();
    }

    @Override
    public String getName() {
        return "toggleprefix";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder eBuilder = new EmbedBuilder();
        eBuilder.setTitle(getTitle());
        eBuilder.setDescription(getDescription());
        eBuilder.addField("Usage", "`{prefix} toggleprefix {optional <prefix>}`", false);
        eBuilder.addField("Example usage", "`" + fireStoreService.getModel().getPrefix() + "toggleprefix !`", false);
        eBuilder.addField("Available aliases", messageUtils.createAliasString(getAliases()), false);
        Color color = new Color((int) (Math.random() * 0x1000000));
        eBuilder.setColor(color);
        return eBuilder.build();
    }

    @Override
    public String getDescription() {
        return "Set bot to respond by prefix";
    }

    @Override
    public String getTitle() {
        return "Toggle prefix";
    }

    @Override
    public boolean isAfterInit() {
        return false;
    }

    @Override
    public List<String> getAliases() {
        return List.of("prefix", "p", "setprefix", "changeprefix");
    }

    @NotNull
    private EmbedBuilder getEmbedBuilder(List<String> args) {
        fireStoreService.getModel().setRespondByPrefix(true);

        EmbedBuilder eBuilder = new EmbedBuilder();
        eBuilder.setTitle("Respond by prefix");
        if (!args.isEmpty()) {
            eBuilder.setTitle("Prefix changed");
            fireStoreService.getModel().setPrefix(args.get(0));
        }
        fireStoreService.updateConfigModel();
        eBuilder.setDescription("Bot is now responding by: **prefix**\n" +
                "Current value for prefix: " + "`" + fireStoreService.getModel().getPrefix() + "`");
        return eBuilder;
    }
}

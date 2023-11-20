package pl.xnik3e.Guardian.components.Command.Commands.BobCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.stereotype.Component;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.CommandContext;
import pl.xnik3e.Guardian.components.Command.ICommand;

import java.awt.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class WhitelistCommand implements ICommand {

    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    public WhitelistCommand(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
    }

    @Override
    public void handle(CommandContext ctx) {
        boolean deleteTriggerMessage = fireStoreService.getModel().isDeleteTriggerMessage();
        if (deleteTriggerMessage)
            ctx.getMessage().delete().queue();
        List<String> args = ctx.getArgs();
        getWhitelist(ctx, null, args, ctx.getGuild());
    }

    @Override
    public void handleSlash(SlashCommandInteractionEvent event, List<String> args) {
        getWhitelist(null, event, args, event.getGuild());
    }

    @Override
    public String getName() {
        return "whitelist";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(getTitle());
        embedBuilder.setDescription(getDescription());
        embedBuilder.addField("Usage",
                "`{prefix or mention} whitelist <user id>`",
                false);
        embedBuilder.addField("Example usage",
                "`" + fireStoreService.getModel().getPrefix() + "whitelist 1164645019769131029`\n"
                        + "`" + fireStoreService.getModel().getPrefix() + "whitelist @xnik3e`",
                false);
        embedBuilder.addField("Available aliases", messageUtils.createAliasString(getAliases()), false);
        Color color = new Color((int) (Math.random() * 0x1000000));
        embedBuilder.setColor(color);
        return embedBuilder.build();
    }

    @Override
    public String getDescription() {
        return "Get whitelisted nicknames for provided user";
    }

    @Override
    public String getTitle() {
        return "Whitelist";
    }

    @Override
    public boolean isAfterInit() {
        return true;
    }

    @Override
    public List<String> getAliases() {
        return List.of("wl");
    }

    public void getWhitelist(CommandContext ctx, SlashCommandInteractionEvent event, List<String> args, Guild guild){
        EmbedBuilder eBuilder = new EmbedBuilder();
        if (args.isEmpty()) {
            eBuilder.setTitle("An error occurred");
            eBuilder.setDescription("Please provide valid user id or mention");
            eBuilder.setColor(Color.RED);
            respondToUser(ctx, event, eBuilder);
            return;
        }
        Matcher matcher = Pattern.compile("\\d+")
                .matcher(args.get(0));
        if (!matcher.find()) {
            respondToUser(ctx, event, eBuilder);
            return;
        }
        String userId = matcher.group();
        guild.retrieveMemberById(userId).queue(member -> {
            String id = member.getId();
            StringBuilder stringBuilder = new StringBuilder();
            AtomicInteger index = new AtomicInteger(1);
            fireStoreService.getWhitelistedNicknames(id)
                    .stream()
                    .map(nick -> index.getAndIncrement() + ". " + nick + "\n")
                    .forEachOrdered(stringBuilder::append);
            eBuilder.setTitle("Whitelisted nicknames for user " + member.getEffectiveName());
            eBuilder.setDescription(stringBuilder.toString().isEmpty() ? "No whitelisted nicknames found" : stringBuilder.toString());
            eBuilder.setColor(Color.GREEN);
            respondToUser(ctx, event, eBuilder);
        }, error -> {
            eBuilder.setTitle("An error occurred");
            eBuilder.setDescription("Please provide valid user id or mention");
            eBuilder.setColor(Color.RED);
            respondToUser(ctx, event, eBuilder);
        });
    }

    private void respondToUser(CommandContext ctx, SlashCommandInteractionEvent event, EmbedBuilder eBuilder) {
        if (ctx != null)
            messageUtils.respondToUser(ctx, eBuilder.build());
        else
            event.getHook().sendMessageEmbeds(eBuilder.build()).setEphemeral(true).queue();
    }
}

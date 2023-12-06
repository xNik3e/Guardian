package pl.xnik3e.Guardian.Components.Command.Commands.BobCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import pl.xnik3e.Guardian.Models.ContextModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.Components.Command.CommandContext;
import pl.xnik3e.Guardian.Components.Command.ICommand;

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
        messageUtils.deleteTrigger(ctx);
        getWhitelist(new ContextModel(ctx));
    }

    @Override
    public void handleSlash(SlashCommandInteractionEvent event, List<String> args) {
        getWhitelist(new ContextModel(event, args));
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

    public void getWhitelist(ContextModel context){
        if (context.args.isEmpty()) {
            sendErrorEmptyArgument(context);
            return;
        }

        Matcher matcher = Pattern.compile("\\d+")
                .matcher(context.args.get(0));
        if (!matcher.find()) {
            sendErrorEmptyArgument(context);
            return;
        }
        String userId = matcher.group();
        context.guild.retrieveMemberById(userId).queue(member -> {
            messageUtils.respondToUser(context.ctx, context.event, getWhitelistedNicknamesEmbedBuilder(member));
        }, error -> {
            sendErrorEmptyArgument(context);
        });
    }

    @NotNull
    private EmbedBuilder getWhitelistedNicknamesEmbedBuilder(Member member) {
        StringBuilder stringBuilder = getMemberWhitelistedNicknames(member);

        return new EmbedBuilder()
                .setTitle("Whitelisted nicknames for user " + member.getEffectiveName())
                .setDescription(stringBuilder.toString().isEmpty() ? "No whitelisted nicknames found" : stringBuilder.toString())
                .setColor(stringBuilder.toString().isEmpty() ? Color.YELLOW : Color.GREEN);
    }

    @NotNull
    private StringBuilder getMemberWhitelistedNicknames(Member member) {
        StringBuilder stringBuilder = new StringBuilder();
        AtomicInteger index = new AtomicInteger(1);
        fireStoreService.getWhitelistedNicknames(member.getId())
                .stream()
                .map(nick -> index.getAndIncrement() + ". " + nick + "\n")
                .forEachOrdered(stringBuilder::append);
        return stringBuilder;
    }

    private void sendErrorEmptyArgument(ContextModel context) {
        EmbedBuilder eBuilder = new EmbedBuilder();
        eBuilder.setTitle("An error occurred");
        eBuilder.setDescription("Please provide valid user id or mention");
        eBuilder.setColor(Color.RED);
        messageUtils.respondToUser(context.ctx, context.event, eBuilder);
    }


}

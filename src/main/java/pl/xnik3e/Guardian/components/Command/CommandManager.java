package pl.xnik3e.Guardian.components.Command;

import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.Commands.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Getter
@Component
public class CommandManager {
    private final MessageUtils messageUtils;
    private final List<ICommand> commands = new ArrayList<>();

    @Autowired
    public CommandManager(MessageUtils messageUtils){
        this.messageUtils = messageUtils;
        addCommand(new FetchUsersWithRoleCommand(messageUtils));
        addCommand(new InitCommand(messageUtils));
        addCommand(new TogglePrefixCommand(messageUtils));
        addCommand(new ToggleMentionCommand(messageUtils));
        addCommand(new BanUsersWithRoleCommand(messageUtils));
        addCommand(new HelpCommand(this, messageUtils));
        addCommand(new ToggleBotResponseCommand(messageUtils));
        addCommand(new ResetCommand(messageUtils));
        addCommand(new DeleteTriggerCommand(messageUtils));
        addCommand(new WhitelistCommand(messageUtils));
    }

    private void addCommand(ICommand cmd) {
        boolean nameFound = this.commands.stream()
                .anyMatch(it -> it.getName()
                        .equalsIgnoreCase(cmd.getName())
                );
        if (nameFound) {
            throw new IllegalArgumentException("Command with this name is already present");
        }
        commands.add(cmd);
    }


    @Nullable
    public ICommand getCommand(String search) {
        String searchLower = search.toLowerCase();
        for (ICommand cmd : this.commands) {
            if (cmd.getName().equals(searchLower) || cmd.getAliases().contains(searchLower)) {
                return cmd;
            }
        }
        return null;
    }

    public void handle(MessageReceivedEvent event) {
        String[] split = messageUtils.rawCommandContent(event)
                .split("\\s+");
        String invoke = split[0].toLowerCase();
        ICommand cmd = this.getCommand(invoke);

        if(cmd == null){
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Command not found");
            embedBuilder.setDescription("Use `" + messageUtils.getFireStoreService().getModel().getPrefix() + "help` to see all available commands");
            embedBuilder.setColor(Color.RED);
            messageUtils.respondToUser(new CommandContext(event, List.of("")), embedBuilder.build());
            return;
        }

        if (checkInit(cmd)) {
            List<String> args = Arrays.asList(split).subList(1, split.length);
            CommandContext ctx = new CommandContext(event, args);
            cmd.handle(ctx);
        }else{
            User user = event.getAuthor();
            event.getMessage().delete().queue();
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Bot not initialized");
            embedBuilder.setDescription("You should run the *init* command first");
            embedBuilder.setColor(Color.RED);
            messageUtils.respondToUser(new CommandContext(event, List.of("")), embedBuilder.build());
        }
    }

    public void handle(SlashCommandInteractionEvent event, String command){
        String[] split = command.split("\\s+");
        String invoke = split[0].toLowerCase();
        ICommand cmd = this.getCommand(invoke);

        if(cmd == null){
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Command not found");
            embedBuilder.setDescription("Use `" + messageUtils.getFireStoreService().getModel().getPrefix() + "help` to see all available commands");
            embedBuilder.setColor(Color.RED);
            event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
            return;
        }

        if (checkInit(cmd)) {
            List<String> args = Arrays.asList(split).subList(1, split.length);
            cmd.handleSlash(event, args);
        }else{
            User user = event.getUser();
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Bot not initialized");
            embedBuilder.setDescription("You should run the *init* command first");
            embedBuilder.setColor(Color.RED);
            event.getHook().sendMessageEmbeds(embedBuilder.build()).setEphemeral(true).queue();
        }
    }

    public boolean checkInit(@Nonnull ICommand cmd){
        boolean a = cmd.isAfterInit();
        boolean b = messageUtils.getFireStoreService().getModel().isInit();

        //bot is init and command require init return true
        //bot is not init and command require init return false
        //command not require init return true
        return !a || b;
    }

}

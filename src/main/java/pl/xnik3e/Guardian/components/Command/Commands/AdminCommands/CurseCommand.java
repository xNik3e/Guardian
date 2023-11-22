package pl.xnik3e.Guardian.components.Command.Commands.AdminCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.concurrent.Task;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.CommandContext;
import pl.xnik3e.Guardian.components.Command.ICommand;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class CurseCommand implements ICommand {

    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    public CurseCommand(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
    }

    @Override
    public void handle(CommandContext ctx) {
        boolean deleteTriggerMessage = fireStoreService.getModel().isDeleteTriggerMessage();
        if(deleteTriggerMessage)
            ctx.getMessage().delete().queue();
        curse(ctx, null, ctx.getArgs(), ctx.getGuild());
    }

    @Override
    public void handleSlash(SlashCommandInteractionEvent event, List<String> args) {
        curse(null, event, args, event.getGuild());
    }

    @Override
    public String getName() {
        return "curse";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(getTitle());
        embedBuilder.setDescription(getDescription());
        embedBuilder.addField("Usage", "`{prefix or mention} curse`", false);
        embedBuilder.addField("Available aliases", messageUtils.createAliasString(getAliases()), false);
        Color color = new Color((int) (Math.random() *  0x1000000));
        embedBuilder.setColor(color);
        return embedBuilder.build();
    }

    @Override
    public String getDescription() {
        return "Ban users that don't have **kultysta** role from the existence";
    }

    @Override
    public String getTitle() {
        return "Banish the unholy spirits!";
    }

    @Override
    public boolean isAfterInit() {
        return true;
    }

    @Override
    public List<String> getAliases() {
        return List.of("curse");
    }

    private void curse(CommandContext ctx, SlashCommandInteractionEvent event, List<String> args, Guild guild) {
        EmbedBuilder eBuilder = new EmbedBuilder();
        if(!args.isEmpty()){
            eBuilder.setTitle("Error");
            eBuilder.setDescription("This command doesn't take any arguments");
            eBuilder.setColor(Color.RED);
            messageUtils.respondToUser(ctx, event, eBuilder);
            return;
        }
        String defaultRoleId = fireStoreService.getModel().getDefaultRoleId();
        eBuilder.setTitle("Curse");
        eBuilder.setDescription("Providing you with the list of unholy spirits...\n*Please wait...*");
        eBuilder.setColor(Color.YELLOW);
        CompletableFuture<Message> future = messageUtils.respondToUser(ctx, event, eBuilder);
        Task<List<Member>> excludedMembers = guild.findMembers(member -> member.getRoles()
                .stream()
                .map(Role::getId)
                .noneMatch(defaultRoleId::equals));
        excludedMembers.onSuccess(members -> {
            Message message = future.join();
            eBuilder.setTitle("Evil spirits");
            eBuilder.setDescription("The following members are not blessed with the **kultysta** role");
            members.forEach(member -> eBuilder.addField(member.getId(), member.getEffectiveName(), true));
            eBuilder.setColor(Color.GREEN);
            Button button = Button.danger("curse", "Curse them!");
            MessageEditData messageCreateData = new MessageEditBuilder().setEmbeds(eBuilder.build()).setActionRow(button).build();
            message.editMessage(messageCreateData).queue();
        }).onError(throwable -> {
            eBuilder.setTitle("Error");
            eBuilder.setDescription("Something went wrong while fetching members");
            eBuilder.setColor(Color.RED);
            messageUtils.respondToUser(ctx, event, eBuilder);
        });
    }
}

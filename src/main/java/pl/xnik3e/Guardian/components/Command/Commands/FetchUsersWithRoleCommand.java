package pl.xnik3e.Guardian.components.Command.Commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.concurrent.Task;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.CommandContext;
import pl.xnik3e.Guardian.components.Command.ICommand;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FetchUsersWithRoleCommand implements ICommand {

    private final MessageUtils messageUtils;

    public FetchUsersWithRoleCommand(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
    }

    @Override
    public void handle(CommandContext ctx) {
        boolean deleteTriggerMessage = messageUtils.getFireStoreService().getModel().isDeleteTriggerMessage();
        if(deleteTriggerMessage)
            ctx.getMessage().delete().queue();
        fetchUsers(ctx, null, ctx.getArgs(), ctx.getGuild());
    }

    @Override
    public void handleSlash(SlashCommandInteractionEvent event, List<String> args) {
        fetchUsers(null, event, args, event.getGuild());
    }

    @Override
    public String getName() {
        return "getuserswithrole";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(getTitle());
        builder.setDescription(getDescription());
        builder.addField("Usage", "`{prefix or mention} getuserswithrole <role id>`", false);
        builder.addField("Available aliases", "`fetchusers`, `getusers`, `findbyrole`, `fetch`", false);
        //get random color
        Color color = new Color((int)(Math.random() * 0x1000000));
        builder.setColor(color);
        return builder.build();
    }

    @Override
    public String getDescription() {
        return "Returns list of users with specified role";
    }

    @Override
    public String getTitle() {
        return "Get users with role command";
    }


    @Override
    public List<String> getAliases() {
        return List.of("fetchusers", "getusers", "findbyrole", "fetch");
    }

    private void fetchUsers(@Nullable CommandContext ctx, @Nullable SlashCommandInteractionEvent event, List<String> args, Guild guild) {
        EmbedBuilder eBuilder = new EmbedBuilder();
        if(args.size() == 1){
            //regular expression to check whether role was mentioned or not
            Matcher matcher = Pattern.compile("\\d+")
                    .matcher(args.get(0));

            eBuilder.setTitle("An error has occurred");
            if(!matcher.find()){
                eBuilder.setDescription("Please provide valid role id or mention");
                eBuilder.setColor(Color.RED);
                respondToUser(ctx, event, eBuilder);
                return;
            }
            String roleId = matcher.group(0);
            Role role = guild.getRoleById(roleId);
            if(role == null){
                eBuilder.setDescription("Please provide valid role id or mention");
                eBuilder.setColor(Color.RED);
                respondToUser(ctx, event, eBuilder);
                return;
            }
            Task<List<Member>> task = guild.findMembersWithRoles(role);
            task.onSuccess(members -> {
                eBuilder.setTitle("Fetching complete");
                eBuilder.setDescription("Users with role: " + role.getName());
                members.forEach(member -> {
                    eBuilder.addField(member.getUser().getName(), member.getUser().getId(), true);
                });
                eBuilder.setColor(Color.GREEN);
                respondToUser(ctx, event, eBuilder);
            });
        }else{
            eBuilder.setDescription("You should only provide single role Id or role mention");
            eBuilder.setColor(Color.RED);
            respondToUser(ctx, event, eBuilder);
        }
    }

    private void respondToUser(@Nullable CommandContext ctx, @Nullable SlashCommandInteractionEvent event, EmbedBuilder eBuilder) {
        if(ctx != null)
            messageUtils.respondToUser(ctx, eBuilder.build());
        else
            Objects.requireNonNull(event).getHook().sendMessageEmbeds(eBuilder.build()).setEphemeral(true).queue();
    }
}

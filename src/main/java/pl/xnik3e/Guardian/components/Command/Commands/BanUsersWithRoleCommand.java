package pl.xnik3e.Guardian.components.Command.Commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.utils.concurrent.Task;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.CommandContext;
import pl.xnik3e.Guardian.components.Command.ICommand;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BanUsersWithRoleCommand implements ICommand {

    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    public BanUsersWithRoleCommand(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
    }

    @Override
    public void handle(CommandContext ctx) {
        ctx.getMessage().delete().queue();
        Guild guild = ctx.getGuild();
        List<String> args = ctx.getArgs();
        EmbedBuilder eBuilder = new EmbedBuilder();
        if (!args.isEmpty()) {
            //regular expression to check whether role was mentioned or not
            Matcher matcher = Pattern.compile("\\d+")
                    .matcher(args.get(0));
            eBuilder.setTitle("An error occurred");
            eBuilder.setDescription("Please provide valid role id or mention");
            eBuilder.setColor(Color.RED);

            if (!matcher.find()) {
                messageUtils.respondToUser(ctx, eBuilder.build());
                return;
            }
            String roleId = matcher.group(0);
            Role role = guild.getRoleById(roleId);
            if (role == null) {
                messageUtils.respondToUser(ctx, eBuilder.build());
                return;
            }
            Task<List<Member>> task = guild.findMembersWithRoles(role);

            task.onSuccess(members -> {
                List<String> toBeBannedIds = new ArrayList<>();
                members.forEach(member -> toBeBannedIds.add(member.getUser().getId()));
                messageUtils.banUsers(toBeBannedIds, guild);
            });
        }else{
            eBuilder.setTitle("Empty argument");
            eBuilder.setDescription("Please provide valid role id or mention");
            eBuilder.setColor(Color.RED);
            messageUtils.respondToUser(ctx, eBuilder.build());
        }

    }

    @Override
    public String getName() {
        return "banuserswithrole";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(getTitle());
        builder.setDescription(getDescription());
        builder.addField("Usage",
                "`{prefix or mention} banuserswithrole <role id>`",
                false);
        builder.addField("Example usage",
                "`" + fireStoreService.getModel().getPrefix() + "banuserswithrole 1164645019769131029`",
                false);
        builder.addField("Available aliases",
                "`banusers`, `banbyrole`, `purge`, `banall`, `banrole`",
                false);
        Color color = new Color((int)(Math.random() * 0x1000000));
        builder.setColor(color);
        return builder.build();
    }

    @Override
    public String getDescription() {
        return "Bans all users with specified role\n**Available only after init command**";
    }

    @Override
    public String getTitle() {
        return "Ban users with role command";
    }

    @Override
    public boolean isAfterInit() {
        return true;
    }

    @Override
    public List<String> getAliases() {
        return List.of("banusers", "banbyrole", "purge", "banall", "banrole");
    }
}

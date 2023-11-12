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
        if (args.size() == 1) {
            //regular expression to check whether role was mentioned or not
            Matcher matcher = Pattern.compile("\\d+")
                    .matcher(args.get(0));

            if (!matcher.find()) {
                messageUtils.openPrivateChannelAndMessageUser(ctx.getMember().getUser(), "Invalid role id");
                return;
            }
            String roleId = matcher.group(0);
            Role role = guild.getRoleById(roleId);
            if (role == null) {
                messageUtils.openPrivateChannelAndMessageUser(ctx.getMember().getUser(), "Invalid role id");
                return;
            }
            Task<List<Member>> task = guild.findMembersWithRoles(role);

            task.onSuccess(members -> {
                List<String> toBeBannedIds = new ArrayList<>();
                members.forEach(member -> toBeBannedIds.add(member.getUser().getId()));
                messageUtils.banUsers(toBeBannedIds, guild);
            });
        }

    }

    @Override
    public String getName() {
        return "banuserswithrole";
    }

    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Ban users with role command");
        builder.setDescription("Bans all users with specified role\n");
        builder.addField("Usage",
                "```{prefix or mention} banuserswithrole <role id>```",
                false);
        builder.addField("Example usage",
                "```" + fireStoreService.getModel().getPrefix() + "banuserswithrole 1164645019769131029```",
                false);
        builder.appendDescription("**Available only after init command**");
        builder.addField("Available aliases",
                "`banusers`, `banbyrole`, `purge`, `banall`, `banrole`",
                false);
        return builder.build();
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

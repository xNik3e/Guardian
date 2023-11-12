package pl.xnik3e.Guardian.components.Command.Commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.utils.concurrent.Task;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.CommandContext;
import pl.xnik3e.Guardian.components.Command.ICommand;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FetchUsersWithRoleCommand implements ICommand {

    private final MessageUtils messageUtils;

    public FetchUsersWithRoleCommand(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
    }

    @Override
    public void handle(CommandContext ctx) {
        ctx.getMessage().delete().queue();
        Guild guild = ctx.getGuild();
        List<String> args = ctx.getArgs();
        if(args.size() == 1){
            //regular expression to check whether role was mentioned or not
            Matcher matcher = Pattern.compile("\\d+")
                    .matcher(args.get(0));

            if(!matcher.find()){
                messageUtils.openPrivateChannelAndMessageUser(ctx.getMember().getUser(), "Invalid role id");
                return;
            }
            String roleId = matcher.group(0);
            Role role = guild.getRoleById(roleId);
            if(role == null){
                messageUtils.openPrivateChannelAndMessageUser(ctx.getMember().getUser(), "Invalid role id");
                return;
            }
            Task<List<Member>> task = guild.findMembersWithRoles(role);
            task.onSuccess(members -> {
                StringBuilder builder = new StringBuilder();
                builder.append("Users with role: ").append(role.getName()).append("\n");
                members.forEach(member -> builder.append(member.getUser().getName()).append("\t").append(member.getUser().getId()).append("\n"));
                messageUtils.openPrivateChannelAndMessageUser(ctx.getMember().getUser(), builder.toString());
            });
        }else{
            messageUtils.openPrivateChannelAndMessageUser(ctx.getMember().getUser(), "You should only provide single role Id or role mention");
        }
    }

    @Override
    public String getName() {
        return "getuserswithrole";
    }

    @Override
    public String getHelp() {
        return "Returns list of users with specified role\n" +
                "Usage: ```{prefix or mention}getuserswithrole <role id>```\n"+
                "Available aliases: `fetchusers`, `getusers`, `findbyrole`";
    }



    @Override
    public List<String> getAliases() {
        return List.of("fetchusers", "getusers", "findbyrole");
    }
}

package pl.xnik3e.Guardian.components.Command.Commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.utils.concurrent.Task;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Command.CommandContext;
import pl.xnik3e.Guardian.components.Command.ICommand;

import java.util.List;
public class FetchUsersWithRole implements ICommand {

    private final MessageUtils messageUtils;

    public FetchUsersWithRole(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
    }

    @Override
    public void handle(CommandContext ctx) {
        ctx.getMessage().delete().queue();
        Guild guild = ctx.getGuild();
        List<String> args = ctx.getArgs();
        if(args.size() == 1){
            Role role = guild.getRoleById(args.get(0));
            if(role == null){
                messageUtils.openPrivateChannelAndMessageUser(ctx.getMember(), "Invalid role id");
                return;
            }
            Task<List<Member>> task = guild.findMembersWithRoles(role);
            System.out.println(task.isStarted() + "");
            task.onSuccess(members -> {
                StringBuilder builder = new StringBuilder();
                builder.append("Users with role: ").append(role.getName()).append("\n");
                members.forEach(member -> builder.append(member.getUser().getName()).append("\t").append(member.getUser().getId()).append("\n"));
                messageUtils.openPrivateChannelAndMessageUser(ctx.getMember(), builder.toString());
            });
        }
    }

    @Override
    public String getName() {
        return "getuserswithrole";
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return List.of("fetchusers", "getusers", "findbyrole");
    }
}

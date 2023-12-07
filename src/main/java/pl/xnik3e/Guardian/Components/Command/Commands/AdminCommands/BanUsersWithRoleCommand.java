package pl.xnik3e.Guardian.Components.Command.Commands.AdminCommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.concurrent.Task;
import pl.xnik3e.Guardian.Models.ContextModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.Components.Command.CommandContext;
import pl.xnik3e.Guardian.Components.Command.ICommand;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
        messageUtils.deleteTrigger(ctx);
        purgeUsers(new ContextModel(ctx));

    }

    @Override
    public void handleSlash(SlashCommandInteractionEvent event, List<String> args) {
        purgeUsers(new ContextModel(event, args));
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
                messageUtils.createAliasString(getAliases()),
                false);
        Color color = new Color((int) (Math.random() * 0x1000000));
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

    private void purgeUsers(ContextModel context) {
        if (!context.args.isEmpty()) {
            //regular expression to check whether role was mentioned or not
            Matcher matcher = Pattern.compile("\\d+")
                    .matcher(context.args.get(0));

            if (!matcher.find()) {
                sendErrorMessageInvalidRole(context);
                return;
            }

            String roleId = matcher.group(0);
            Role role = context.guild.getRoleById(roleId);
            if (role == null || role.isPublicRole()) {
                sendErrorMessageInvalidRole(context);
                return;
            }

            User user = context.from == ContextModel.From.EVENT ? context.event.getUser() : context.ctx.getAuthor();

            if (!fireStoreService.getModel().getExcludedUserIds().contains(user.getId())) {
                sendErrorMessageMissingPermissions(context);
                return;
            }
            banExactRole(context, role);
        } else {
            String roleToDelete = fireStoreService.getModel().getRolesToDelete().get(0);
            Role role = context.guild.getRoleById(roleToDelete);
            if (role == null) {
                sendErrorMessageInvalidRole(context);
                return;
            }
            banExactRole(context, role);
        }
    }

    private void sendErrorMessageMissingPermissions(ContextModel context) {
        EmbedBuilder eBuilder = new EmbedBuilder();
        eBuilder.setTitle("Missing permissions");
        eBuilder.setDescription("Hey! Only users with granted permissions can choose different role to ban\n" +
                "Try using the command without an argument to purge default role");
        eBuilder.setColor(Color.RED);
        messageUtils.respondToUser(context.ctx, context.event, eBuilder);
    }

    private void sendErrorMessageInvalidRole(ContextModel context) {
        EmbedBuilder eBuilder = new EmbedBuilder();
        eBuilder.setTitle("An error occurred");
        eBuilder.setDescription("Please provide valid role id or mention");
        eBuilder.setColor(Color.RED);
        messageUtils.respondToUser(context.ctx, context.event, eBuilder);
    }

    private void banExactRole(ContextModel context, Role role) {
        context.guild.findMembersWithRoles(role)
                .onSuccess(members -> {
                   /* members.forEach(member -> {
                        if (messageUtils.performMemberCheck(member)) return;
                        toBeBannedIds.add(member.getUser().getId());
                    });*/
                    List<String> toBeBannedIds = members.stream()
                            .filter(member -> !messageUtils.performMemberCheck(member))
                            .map(member -> member.getUser().getId())
                            .toList();

                    messageUtils.respondToUser(context.ctx, context.event,
                            new EmbedBuilder().setTitle("Banning users")
                            .setDescription("Banning users with role: **" + role.getName() + "**")
                            .setColor(Color.GREEN));

                    messageUtils.banUsers(toBeBannedIds, context.guild, 365, TimeUnit.DAYS, "Niespełnianie wymagań wiekowych", true);
                });
    }

}

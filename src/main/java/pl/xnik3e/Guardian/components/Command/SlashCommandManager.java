package pl.xnik3e.Guardian.components.Command;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.stereotype.Component;

@Component
public class SlashCommandManager {

    public void updateSlashCommand(Guild guild) {
        guild.updateCommands().addCommands(
                Commands.slash("help", "Show help")
                        .addOptions(
                                new OptionData(OptionType.STRING, "command", "Command name", false)
                                        .addChoice("Help", "help")
                                        .addChoice("Init", "init")
                                        .addChoice("Toggle prefix", "prefix")
                                        .addChoice("Toggle mention", "mention")
                                        .addChoice("Get users with role", "fetch")
                                        .addChoice("Ban users with role", "purge")
                                        .addChoice("Toggle bot response", "tbr")
                                        .addChoice("Delete message trigger", "dt")
                                        .addChoice("Reset bot", "reset")
                        ),
                Commands.slash("init", "Init the bot")
                        .addOptions(
                                new OptionData(OptionType.STRING, "option", "Option", false)
                                        .addChoice("Ban", "ban")
                                        .addChoice("Log", "log")
                        ),
                Commands.slash("prefix", "Toggle prefix")
                        .addOptions(
                                new OptionData(OptionType.STRING, "prefix", "Optional new prefix", false)
                        ),
                Commands.slash("mention", "Toggle mention"),
                Commands.slash("fetch", "Get users with role")
                        .addOption(OptionType.ROLE, "role", "Role to fetch", true),
                Commands.slash("purge", "Ban users with role")
                        .addOption(OptionType.ROLE, "role", "Role to ban", true),
                Commands.slash("tbr", "Toggle bot response"),
                Commands.slash("dt", "Delete message trigger"),
                Commands.slash("reset", "Reset bot")
        ).queue();
    }
}

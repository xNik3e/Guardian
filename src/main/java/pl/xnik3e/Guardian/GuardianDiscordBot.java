package pl.xnik3e.Guardian;

import com.google.cloud.firestore.Firestore;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import pl.xnik3e.Guardian.components.Command.SlashCommandManager;
import pl.xnik3e.Guardian.listeners.MessageCommandListener;
import pl.xnik3e.Guardian.listeners.RoleAddRemoveListener;
import pl.xnik3e.Guardian.listeners.SlashCommandInteractionListener;

@Component
@Scope("singleton")
public class GuardianDiscordBot {

    private final JDA jda;
    private final Dotenv config;
    private final RoleAddRemoveListener roleAddRemoveListener;
    private final MessageCommandListener messageCommandListener;
    private final SlashCommandInteractionListener slashCommandInteractionListener;
    private final SlashCommandManager slashCommandManager;

    @Autowired
    private GuardianDiscordBot(Firestore firestore, RoleAddRemoveListener roleAddRemoveListener, MessageCommandListener messageCommandListener, SlashCommandInteractionListener slashCommandInteractionListener, SlashCommandManager slashCommandManager) {
        this.roleAddRemoveListener = roleAddRemoveListener;
        this.messageCommandListener = messageCommandListener;
        this.slashCommandInteractionListener = slashCommandInteractionListener;
        this.slashCommandManager = slashCommandManager;
        config = Dotenv.configure().load();
        try {
            JDABuilder builder = JDABuilder
                    .createDefault(config.get("TOKEN"))
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES);

            builder.addEventListeners(roleAddRemoveListener); //Listener for adding and removing roles
            builder.addEventListeners(messageCommandListener); //Listener for user commands
            builder.addEventListeners(slashCommandInteractionListener); //Listener for slash commands

            jda = builder.build().awaitReady();

            Guild guild = jda.getGuildById(config.get("GUILD_ID"));
            if (guild != null)
                slashCommandManager.updateSlashCommand(guild);

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to login to discord");
        }
    }

    @Bean
    public JDA getJda() {
        return jda;
    }
}

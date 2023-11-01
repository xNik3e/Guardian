package pl.xnik3e.Guardian;

import com.google.cloud.firestore.Firestore;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import pl.xnik3e.Guardian.listeners.MessageCommandListener;
import pl.xnik3e.Guardian.listeners.RoleAddRemoveListener;

@Component
@Scope("singleton")
public class GuardianDiscordBot {

    @Getter
    private final JDA jda;
    private final Dotenv config;
    private final RoleAddRemoveListener roleAddRemoveListener;
    private final MessageCommandListener messageCommandListener;

    @Autowired
    private GuardianDiscordBot(Firestore firestore, RoleAddRemoveListener roleAddRemoveListener, MessageCommandListener messageCommandListener){
        this.roleAddRemoveListener = roleAddRemoveListener;
        this.messageCommandListener = messageCommandListener;
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

            jda = builder.build().awaitReady();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Failed to login to discord");
        }
    }
}

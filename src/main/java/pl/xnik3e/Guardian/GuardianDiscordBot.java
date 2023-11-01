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
import pl.xnik3e.Guardian.listeners.MyEventListener;

@Component
@Scope("singleton")
public class GuardianDiscordBot {

    @Getter
    private final JDA jda;
    private final Dotenv config;
    private final MyEventListener myEventListener;

    @Autowired
    private GuardianDiscordBot(Firestore firestore, MyEventListener myEventListener){
        this.myEventListener = myEventListener;
        config = Dotenv.configure().load();
        try {
            JDABuilder builder = JDABuilder
                    .createDefault(config.get("TOKEN"))
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES);

            builder.addEventListeners(myEventListener);
            jda = builder.build().awaitReady();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to login to discord");
        }
    }
}

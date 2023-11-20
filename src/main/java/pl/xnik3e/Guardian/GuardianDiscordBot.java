package pl.xnik3e.Guardian;

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
import pl.xnik3e.Guardian.Models.EnvironmentModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.components.Command.SlashCommandUpdater;
import pl.xnik3e.Guardian.listeners.BobNicknameChangeListener;
import pl.xnik3e.Guardian.listeners.MessageCommandListener;
import pl.xnik3e.Guardian.listeners.SlashCommandInteractionListener;

@Component
@Scope("singleton")
public class GuardianDiscordBot {

    private final JDA jda;
    private Dotenv config;
    private final MessageCommandListener messageCommandListener;
    private final SlashCommandInteractionListener slashCommandInteractionListener;
    private final BobNicknameChangeListener bobNicknameChangeListener;
    private final SlashCommandUpdater slashCommandUpdater;
    private final FireStoreService fireStoreService;


    @Autowired
    private GuardianDiscordBot(MessageCommandListener messageCommandListener, SlashCommandInteractionListener slashCommandInteractionListener, BobNicknameChangeListener bobNicknameChangeListener, SlashCommandUpdater slashCommandUpdater, FireStoreService fireStoreService) {
        this.messageCommandListener = messageCommandListener;
        this.slashCommandInteractionListener = slashCommandInteractionListener;
        this.bobNicknameChangeListener = bobNicknameChangeListener;
        this.slashCommandUpdater = slashCommandUpdater;
        this.fireStoreService = fireStoreService;
        try {
            EnvironmentModel eModel = fireStoreService.getEnvironmentModel();
            JDABuilder builder = JDABuilder
                    .createDefault(eModel.getTOKEN())
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES);

            builder.addEventListeners(messageCommandListener); //Listener for user commands
            builder.addEventListeners(slashCommandInteractionListener); //Listener for slash commands
            builder.addEventListeners(bobNicknameChangeListener); //Listener for bob nickname change

            jda = builder.build().awaitReady();

            Guild guild = jda.getGuildById(eModel.getGUILD_ID());
            if (guild != null)
                slashCommandUpdater.updateSlashCommand(guild);

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

package pl.xnik3e.Guardian.Components.Button.Buttons;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.jetbrains.annotations.NotNull;
import pl.xnik3e.Guardian.Models.BasicCacheModel;
import pl.xnik3e.Guardian.Models.CurseModel;
import pl.xnik3e.Guardian.Models.FetchedRoleModel;
import pl.xnik3e.Guardian.Models.ToBobifyMembersModel;
import pl.xnik3e.Guardian.Services.FireStoreService;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class BasicPagingButtonUtils {

    protected final static String PREVIOUS_PAGE = "previousPage";
    protected final static String PAGES = "pages";
    protected final static String FUNCTION = "function";
    protected final int MAX_USERS;
    protected final MessageCreateBuilder createBuilder;
    protected final EmbedBuilder eBuilder;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final FireStoreService fireStoreService;

    public BasicPagingButtonUtils(FireStoreService fireStoreService) {
        this.fireStoreService = fireStoreService;
        this.createBuilder = new MessageCreateBuilder();
        this.eBuilder = new EmbedBuilder();
        this.MAX_USERS = fireStoreService.getModel().getMaxElementsInEmbed();
    }

    protected void setFetchError(ButtonInteractionEvent event) {
        eBuilder.clear();
        createBuilder.clear();
        eBuilder.setTitle("Error");
        eBuilder.setDescription("Failed to fetch users");
        eBuilder.setColor(Color.RED);
        createBuilder.setEmbeds(eBuilder.build());
        event.getHook().editOriginalEmbeds().queue();
        event.getHook().editOriginal(getMessageEditBuilder().build()).queue();
    }

    @NotNull
    private MessageEditBuilder getMessageEditBuilder() {
        return new MessageEditBuilder().applyCreateData(createBuilder.build());
    }


    protected <T extends BasicCacheModel> void createMessage(ButtonInteractionEvent event, T model, Map<String, Integer> pageMap, Predicate<Integer> predicate, Direction direction) throws Exception{
        Button buttonNext = Button.primary("nextPage", "Next page");
        Button buttonPrevious = Button.primary("previousPage", "Previous page");
        int currentPage = pageMap.get(PREVIOUS_PAGE);
        int pages = pageMap.get(PAGES);

        List<Map<String, String>> maps = model.getMaps();

        String time = new SimpleDateFormat("HH:mm:ss").format(new Date(model.getTimestamp()));

        if(predicate.test(currentPage)){
            createBuilder.setActionRow(direction == Direction.PREVIOUS ? buttonNext : buttonPrevious);
        }else{
            createBuilder.setActionRow(buttonPrevious, buttonNext);
        }

        Class<?> clazz = model.getClass();
        if(clazz.isAssignableFrom(FetchedRoleModel.class))
            createFetchUserWithRoleMessage((FetchedRoleModel) model, time, currentPage, pages, maps);
        else if(clazz.isAssignableFrom(ToBobifyMembersModel.class))
            createGetBobMessage((ToBobifyMembersModel) model, time, currentPage, pages, maps);
        else if(clazz.isAssignableFrom(CurseModel.class))
            createCurseMessage((CurseModel) model, time, currentPage, pages, maps);

        event.getHook().editOriginalEmbeds().queue();
        event.getHook().editOriginal(getMessageEditBuilder().build()).queue();
    }

    private void createCurseMessage(CurseModel model, String time, int currentPage, int pages, List<Map<String, String>> maps) {
        eBuilder.clear();
        eBuilder.setTitle("Evil spirits");
        eBuilder.setDescription("The following " + model.getAllEntries() + " members are not blessed with the **kultysta** role");
        eBuilder.appendDescription("\n\n**CACHED DATA WILL BE ISSUED FOR DELETION AFTER: **" + time + "\n*ANY REQUESTS AFTER THAT TIME CAN RESULT IN FAILURE*\n");
        eBuilder.setFooter("Showing page {**" + currentPage + "/" + pages + "**} for [Curse]");
        eBuilder.setColor(Color.GREEN);
        maps.forEach(map -> {
            eBuilder.addField(map.get("effectiveName"), map.get("mention") + "\nJoined: " +
                    new SimpleDateFormat("yyyy.MM.dd [HH:mm]").format(new Date(Long.parseLong(map.get("timeJoined")))), true);
        });
        createBuilder.addActionRow(Button.danger("curse", "Curse them!"));
        createBuilder.setEmbeds(eBuilder.build());
    }

    private void createGetBobMessage(ToBobifyMembersModel model, String time, int currentPage, int pages, List<Map<String, String>> maps) {
        eBuilder.clear();
        eBuilder.setTitle("Bob list");
        eBuilder.setDescription("List of users which username are marked as not being mentionable");
        eBuilder.appendDescription("\nTo Bobify specific user use `" + fireStoreService.getModel().getPrefix() + "bobify <userID>` command");
        eBuilder.appendDescription("\n\n**CACHED DATA WILL BE ISSUED FOR DELETION AFTER: **" + time + "\n*ANY REQUESTS AFTER THAT TIME CAN RESULT IN FAILURE*\n");
        eBuilder.setFooter("Showing page {**" + currentPage + "/" + pages + "**} for [GetBob]");
        eBuilder.setColor(Color.GREEN);
        maps.forEach(map -> {
            eBuilder.addField(map.get("effectiveName"), map.get("mention") + "\n[" + map.get("userID")+"]", true);
        });
        createBuilder.addActionRow(Button.danger("bobifyall", "Bobify all"));
        createBuilder.setEmbeds(eBuilder.build());
    }

    private void createFetchUserWithRoleMessage(FetchedRoleModel model, String time, int currentPage, int pages, List<Map<String, String>> maps) {
        eBuilder.clear();
        eBuilder.setTitle("Fetched role *" + model.getRoleName() + "* users");
        eBuilder.setDescription("I've found **" +
                model.getAllEntries() +
                "** users with role **" +
                model.getRoleName() + "**");
        eBuilder.appendDescription("\n\n**CACHED DATA WILL BE ISSUED FOR DELETION AFTER: **" + time + "\n*ANY REQUESTS AFTER THAT TIME CAN RESULT IN FAILURE*\n");
        eBuilder.setFooter("Showing page {**" + currentPage + "/" + pages + "**} for [Fetch]");
        eBuilder.setColor(Color.GREEN);
        maps.forEach(map -> {
            eBuilder.addField(map.get("userID"), map.get("value"), true);
        });
        createBuilder.setEmbeds(eBuilder.build());
    }

    protected void setLoading(ButtonInteractionEvent event, int page) {
        eBuilder.clear();
        eBuilder.setTitle("Loading page " + page + "...");
        eBuilder.setDescription("Please wait");
        eBuilder.setColor(Color.YELLOW);
        createBuilder.setEmbeds(eBuilder.build());
        event.getHook().editOriginal(getMessageEditBuilder().build()).queue();
    }

    /**
     * Extrude data from footer.
     * <p></p>
     *
     * @param footer Message footer
     * @return Map<String,String> map with data
     */
    protected Map<String, String> extrudeFooterData(String footer) {
        String regexXy = "\\{\\*\\*(.*?)\\*\\*\\}";
        String regexZ = "\\[(.*?)\\]";
        Pattern patternXy = Pattern.compile(regexXy);
        Pattern patternZ = Pattern.compile(regexZ);
        Matcher matcherZ = patternZ.matcher(footer);
        Matcher matcherXy = patternXy.matcher(footer);

        if (matcherXy.find() && matcherZ.find()) {
            String[] xyValue = matcherXy.group(1).split("/");
            String zValue = matcherZ.group(1);

            Map<String, String> map = new HashMap<>();
            map.put("previousPage", xyValue[0]);
            map.put("pages", xyValue[1]);
            map.put("function", zValue);
            return map;
        }
        return null;
    }

    protected enum Direction{
        NEXT,
        PREVIOUS
    }
}

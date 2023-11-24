package pl.xnik3e.Guardian.components.Button.Buttons;

import lombok.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import pl.xnik3e.Guardian.Models.FetchedRoleModel;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class BasicRoleFetchButtonUtils {

    protected final static String PREVIOUS_PAGE = "previousPage";
    protected final static String PAGES = "pages";
    protected final static String FUNCTION = "function";
    protected final static int MAX_USERS = 25;
    protected final MessageEditBuilder editBuilder;
    protected final EmbedBuilder eBuilder;

    public BasicRoleFetchButtonUtils() {
        this.editBuilder = new MessageEditBuilder();
        this.eBuilder = new EmbedBuilder();
    }

    protected void setFetchError(ButtonInteractionEvent event) {
        eBuilder.clear();
        editBuilder.clear();
        eBuilder.setTitle("Error");
        eBuilder.setDescription("Failed to fetch users");
        eBuilder.setColor(Color.RED);
        editBuilder.setEmbeds(eBuilder.build());
        event.getHook().editOriginalEmbeds().queue();
        event.getHook().editOriginal(editBuilder.build()).queue();
    }

    protected void createMessage(ButtonInteractionEvent event, FetchedRoleModel model, Map<String, Integer> pageMap, Predicate<Integer> predicate, Direction direction) throws Exception{
        Button buttonNext = Button.primary("nextPage", "Next page");
        Button buttonPrevious = Button.primary("previousPage", "Previous page");
        int currentPage = pageMap.get(PREVIOUS_PAGE);
        int pages = pageMap.get(PAGES);

        List<Map<String, String>> maps = model.getUsers();

        if(predicate.test(currentPage)){
            editBuilder.setActionRow(direction == Direction.PREVIOUS ? buttonNext : buttonPrevious);
        }else{
            editBuilder.setActionRow(buttonPrevious, buttonNext);
        }
        eBuilder.clear();
        eBuilder.setTitle("Fetched role *" + model.getRoleName() + "* users");
        eBuilder.setDescription("I've found **" +
                model.getAllEntries() +
                "** users with role **" +
                model.getRoleName() + "**");
        eBuilder.setFooter("Showing page {**" + currentPage + "/" + pages + "**} for [Fetch]");
        eBuilder.setColor(Color.GREEN);
        maps.forEach(map -> {
            eBuilder.addField(map.get("userID"), map.get("value"), true);
        });
        editBuilder.setEmbeds(eBuilder.build());
        event.getHook().editOriginalEmbeds().queue();
        event.getHook().editOriginal(editBuilder.build()).queue();
    }

    protected void setLoading(ButtonInteractionEvent event, int page) {
        eBuilder.clear();
        eBuilder.setTitle("Loading page " + page + "...");
        eBuilder.setDescription("Please wait");
        eBuilder.setColor(Color.YELLOW);
        editBuilder.setEmbeds(eBuilder.build());
        event.getHook().editOriginal(editBuilder.build()).queue();
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

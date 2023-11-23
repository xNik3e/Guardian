package pl.xnik3e.Guardian.components.Button.Buttons;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import pl.xnik3e.Guardian.Models.FetchedRoleUserModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Button.IButton;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;

public class NextPageButton implements IButton {

    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;
    private final static String PREVIOUS_PAGE = "previousPage";
    private final static String PAGES = "pages";
    private final static String FUNCTION = "function";
    private final static int MAX_USERS= 5;

    public NextPageButton(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
    }

    @Override
    public void handle(ButtonInteractionEvent event) {
        Message message = event.getMessage();
        String footer = message.getEmbeds().get(0).getFooter().getText();
        Map<String, String> paramMap = getData(footer);

        MessageEditBuilder editBuilder = new MessageEditBuilder();
        EmbedBuilder eBuilder = new EmbedBuilder();
        //Handle first interaction with button -> show loading
        int nextPage = Integer.parseInt(paramMap.get(PREVIOUS_PAGE)) + 1;
        eBuilder.setTitle("Loading page " + nextPage + "...");
        eBuilder.setDescription("Please wait");
        eBuilder.setColor(Color.YELLOW);
        editBuilder.setEmbeds(eBuilder.build());
        event.getHook().editOriginal(editBuilder.build()).queue();

        switch(paramMap.get(FUNCTION)){
            case "Fetch":
                fetchUsersWithRole(event, paramMap, editBuilder, eBuilder);
                return;
            default:
                return;
        }
    }

    private void fetchUsersWithRole(ButtonInteractionEvent event, Map<String, String> paramMap, MessageEditBuilder editBuilder, EmbedBuilder eBuilder) {
        try{
            int currentPage = Integer.parseInt(paramMap.get(PREVIOUS_PAGE)) + 1;
            int pages = Integer.parseInt(paramMap.get(PAGES));
            Optional<List<FetchedRoleUserModel>> fetchedRoleUserModels = fireStoreService.fetchFetchedRoleUserList(currentPage, MAX_USERS);
            if(fetchedRoleUserModels.isEmpty()){
                eBuilder.setTitle("Error");
                eBuilder.setDescription("Failed to fetch users");
                eBuilder.setColor(Color.RED);
                editBuilder.clear();
                editBuilder.setEmbeds(eBuilder.build());
                event.getHook().editOriginalEmbeds().queue();
                event.getHook().editOriginal(editBuilder.build()).queue();
                return;
            }
            List<FetchedRoleUserModel> modelList = fetchedRoleUserModels.get();
            Button buttonNext = Button.primary("nextPage", "Next page");
            Button buttonPrevious = Button.primary("previousPage", "Previous page");
            if(currentPage == pages){
                editBuilder.setActionRow(buttonPrevious);
            }else{
                editBuilder.setActionRow(buttonPrevious, buttonNext);
            }
            eBuilder.clear();
            eBuilder.setTitle("Fetched role *" + modelList.get(0).getRoleName() + "* users");
            eBuilder.setDescription("I've found **" + modelList.get(0).getAllEntries() + "** users with role **" + modelList.get(0).getRoleName() + "**");
            eBuilder.setFooter("Showing page {**" + currentPage + "/" + pages + "**} for [Fetch]");
            modelList.forEach(fetchedRoleUserModel -> {
                eBuilder.addField(fetchedRoleUserModel.getUserID(), fetchedRoleUserModel.getValue(), true);
            });
            eBuilder.setColor(Color.GREEN);
            editBuilder.setEmbeds(eBuilder.build());
            event.getHook().editOriginal(editBuilder.build()).queue();
        }catch(Exception e){
            eBuilder.setTitle("Error");
            eBuilder.setDescription("Failed to fetch users");
            eBuilder.setColor(Color.RED);
            editBuilder.clear();
            editBuilder.setEmbeds(eBuilder.build());
            event.getHook().editOriginalEmbeds().queue();
            event.getHook().editOriginal(editBuilder.build()).queue();
        }
    }

    private Map<String, String> getData(String footer) {
        String regexXy = "\\{\\*\\*(.*?)\\*\\*\\}";
        String regexZ = "\\[(.*?)\\]";
        Pattern patternXy = Pattern.compile(regexXy);
        Pattern patternZ = Pattern.compile(regexZ);
        Matcher matcherZ = patternZ.matcher(footer);
        Matcher matcherXy = patternXy.matcher(footer);

        if(matcherXy.find() && matcherZ.find()){
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

    @Override
    public String getValue() {
        return "nextPage";
    }
}

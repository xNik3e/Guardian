package pl.xnik3e.Guardian.components.Button.Buttons;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import pl.xnik3e.Guardian.Models.BasicCacheModel;
import pl.xnik3e.Guardian.Models.FetchedRoleModel;
import pl.xnik3e.Guardian.Models.ToBobifyMembersModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Button.IButton;

import java.util.*;

public class NextPageButton extends BasicPagingButtonUtils implements IButton {

    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    public NextPageButton(MessageUtils messageUtils) {
        super(messageUtils.getFireStoreService());
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
    }

    @Override
    public void handle(ButtonInteractionEvent event) {
        event.deferEdit().queue();
        try {
            Message message = event.getMessage();
            String footer = Objects.requireNonNull(message.getEmbeds().get(0)
                            .getFooter())
                    .getText();
            Map<String, String> paramMap = extrudeFooterData(footer);

            //Handle first interaction with button -> show loading
            int nextPage = Integer.parseInt(paramMap.get(PREVIOUS_PAGE)) + 1;
            setLoading(event, nextPage);

            switch (paramMap.get(FUNCTION)) {
                case "Fetch":
                    fetchCache(event, paramMap, FetchedRoleModel.class);
                    return;
                case "GetBob":
                    fetchCache(event, paramMap, ToBobifyMembersModel.class);
                    return;
                default:
                    return;
            }
        } catch (Exception e) {
            setFetchError(event);
        }
    }

    @Override
    public String getValue() {
        return "nextPage";
    }

    private <T extends BasicCacheModel> void fetchCache(ButtonInteractionEvent event, Map<String, String> paramMap, Class<T> clazz) {
        try{
            int currentPage = Integer.parseInt(paramMap.get(PREVIOUS_PAGE)) + 1;
            int allPages = Integer.parseInt(paramMap.get(PAGES));
            Map<String, Integer> pageMap = Map.of(PREVIOUS_PAGE, currentPage, PAGES, allPages);

            Optional<T> membersModel
                    = fireStoreService.fetchCache(currentPage, MAX_USERS, clazz);
            if(membersModel.isEmpty()) {
                setFetchError(event);
                return;
            }
            createMessage(event, membersModel.get(), pageMap, page -> page == allPages, Direction.NEXT);
        }catch(Exception e){
            setFetchError(event);
        }
    }
}

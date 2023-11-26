package pl.xnik3e.Guardian.components.Button.Buttons;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import pl.xnik3e.Guardian.Models.BasicCacheModel;
import pl.xnik3e.Guardian.Models.CurseModel;
import pl.xnik3e.Guardian.Models.FetchedRoleModel;
import pl.xnik3e.Guardian.Models.ToBobifyMembersModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Button.IButton;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class PreviousPageButton extends BasicPagingButtonUtils implements IButton {

    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    public PreviousPageButton(MessageUtils messageUtils) {
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
            int previousPage = Integer.parseInt(paramMap.get(PREVIOUS_PAGE)) - 1;
            setLoading(event, previousPage);
            switch (paramMap.get(FUNCTION)) {
                case "Fetch":
                    fetchCache(event, paramMap, FetchedRoleModel.class);
                    return;
                case "GetBob":
                    fetchCache(event, paramMap, ToBobifyMembersModel.class);
                    return;
                case "Curse":
                    fetchCache(event, paramMap, CurseModel.class);
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
        return "previousPage";
    }

    private <T extends BasicCacheModel> void fetchCache(ButtonInteractionEvent event, Map<String, String> paramMap, Class<T> clazz) {
        try {
            int currentPage = Integer.parseInt(paramMap.get(PREVIOUS_PAGE)) - 1;
            int allPages = Integer.parseInt(paramMap.get(PAGES));
            Map<String, Integer> pageMap = Map.of(PREVIOUS_PAGE, currentPage, PAGES, allPages);

            Optional<T> membersModel
                    = fireStoreService.fetchCache(currentPage, MAX_USERS, clazz);
            if (membersModel.isEmpty()) {
                setFetchError(event);
                return;
            }
            createMessage(event, membersModel.get(), pageMap, page -> page == allPages, Direction.PREVIOUS);
        } catch (Exception e) {
            setFetchError(event);
        }
    }

}

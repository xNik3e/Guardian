package pl.xnik3e.Guardian.components.Button.Buttons;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import pl.xnik3e.Guardian.Models.FetchedRoleModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Utils.MessageUtils;
import pl.xnik3e.Guardian.components.Button.IButton;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public class PreviousPageButton extends BasicRoleFetchButtonUtils implements IButton {

    private final MessageUtils messageUtils;
    private final FireStoreService fireStoreService;

    public PreviousPageButton(MessageUtils messageUtils) {
        super();
        this.messageUtils = messageUtils;
        this.fireStoreService = messageUtils.getFireStoreService();
    }


    @Override
    public void handle(ButtonInteractionEvent event) {
        event.deferEdit().queue();
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
                fetchUsersWithRole(event, paramMap);
                return;
            default:
                return;
        }
    }

    @Override
    public String getValue() {
        return "previousPage";
    }

    private void fetchUsersWithRole(ButtonInteractionEvent event, Map<String, String> paramMap) {
        try {
            int currentPage = Integer.parseInt(paramMap.get(PREVIOUS_PAGE)) - 1;
            int allPages = Integer.parseInt(paramMap.get(PAGES));
            Map<String, Integer> pageMap = Map.of(PREVIOUS_PAGE, currentPage, PAGES, allPages);

            Optional<FetchedRoleModel> fetchedRoleModel =
                    fireStoreService.fetchFetchedRoleUserList(currentPage, MAX_USERS);
            if (fetchedRoleModel.isEmpty()) {
                setFetchError(event);
                return;
            }

            createMessage(event, fetchedRoleModel.get(), pageMap, page -> page == 1, Direction.PREVIOUS);
        } catch (Exception e) {
            setFetchError(event);
        }
    }


}

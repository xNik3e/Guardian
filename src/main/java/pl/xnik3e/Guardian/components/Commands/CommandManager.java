package pl.xnik3e.Guardian.components.Commands;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
public class CommandManager {
    private final List<ICommand> commands = new ArrayList<>();
}

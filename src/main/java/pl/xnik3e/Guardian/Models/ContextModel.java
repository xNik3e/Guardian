package pl.xnik3e.Guardian.Models;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import pl.xnik3e.Guardian.Components.Command.CommandContext;

import java.util.List;

public class ContextModel {
    public Guild guild;
    public Channel channel;
    public List<String> args;
    ContextModel.From from;
    public CommandContext ctx;
    public SlashCommandInteractionEvent event;

    public ContextModel(CommandContext ctx) {
        this.guild = ctx.getGuild();
        this.channel = ctx.getChannel();
        this.args = ctx.getArgs();
        this.from = ContextModel.From.CONTEXT;
        this.ctx = ctx;
    }

    public ContextModel(SlashCommandInteractionEvent event, List<String> args) {
        this.guild = event.getGuild();
        this.channel = event.getChannel();
        this.args = args;
        this.from = ContextModel.From.EVENT;
        this.event = event;
    }

    protected enum From {
        EVENT,
        CONTEXT
    }
}

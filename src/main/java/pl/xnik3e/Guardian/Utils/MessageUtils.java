package pl.xnik3e.Guardian.Utils;

import lombok.Getter;
import lombok.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.xnik3e.Guardian.Components.Command.Commands.ConfigCommands.InitCommand;
import pl.xnik3e.Guardian.Models.BasicCacheModel;
import pl.xnik3e.Guardian.Models.ConfigModel;
import pl.xnik3e.Guardian.Models.TempBanModel;
import pl.xnik3e.Guardian.Services.FireStoreService;
import pl.xnik3e.Guardian.Components.Command.CommandContext;

import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Service
public class MessageUtils {

    private final FireStoreService fireStoreService;
    private final ConfigModel configModel;
    private final List<String> mentionableCharacters = List.of("a", "ą", "b", "c", "ć", "d", "e", "ę", "f", "g", "h", "i", "j", "k",
            "l", "ł", "m", "n", "o", "ó", "p", "q", "r", "s", "ś", "t", "u", "v", "w", "x", "y", "z", "ź", "ż", "A", "Ą", "B", "C", "Ć", "D",
            "E", "Ę", "F", "G", "H", "I", "J", "K", "L", "Ł", "M", "N", "O", "Ó", "P", "Q", "R", "S", "Ś", "T", "U", "V", "W", "X", "Y", "Z",
            "Ź", "Ż", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", " ", "-", "_", "!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "+",
            "=", "[", "]", ".", "<", ">", "{", "}");


    @Autowired
    public MessageUtils(FireStoreService fireStoreService) {
        this.fireStoreService = fireStoreService;
        this.configModel = fireStoreService.getModel();
    }

    /**
     * Get list of member roles as String
     * <p></p>
     *
     * @param member Member to get roles from
     * @return List of member roles as String
     */
    @NotNull
    public static List<String> getMemberRoleList(Member member) {
        return member.getRoles()
                .stream()
                .map(Role::getIdLong)
                .map(String::valueOf).toList();
    }


    /**
     * Checks if member has any role included in excludedRoleIds list.
     * <p></p>
     *
     * @param userRoles List of user roles
     * @return true if member has any role included in excludedRoleIds list, false otherwise
     */
    private boolean performRolesToExcludeCheck(List<String> userRoles) {
        List<String> excludedRoles = configModel
                .getExcludedRoleIds();
        return userRoles.stream().anyMatch(excludedRoles::contains);
    }

    /**
     * Checks if member has any privileges.
     * <p></p>
     *
     * @param member Member to check
     * @return true if member has any role included in excludedRoleIds list or matching id in excludedUserIds list, false otherwise
     */
    public boolean checkAuthority(Member member) {
        List<String> memberRoles = getMemberRoleList(member);
        return performRolesToExcludeCheck(memberRoles) || configModel
                .getExcludedUserIds()
                .contains(member.getId());
    }

    /**
     * Checks if message should be processed by bot.
     * <p></p>
     *
     * @param event MessageReceivedEvent to check
     * @return true if message should be processed by bot, false otherwise
     */
    public boolean checkTrigger(MessageReceivedEvent event) {
        return configModel.isRespondByPrefix() ? checkPrefix(event) : checkBotMention(event);
    }

    /**
     * Checks if bot was mentioned in message.
     * <p></p>
     *
     * @param event MessageReceivedEvent to check
     * @return true if bot was mentioned in message, false otherwise
     */
    private boolean checkBotMention(MessageReceivedEvent event) {
        return event.getMessage()
                .getMentions()
                .getMentions(Message.MentionType.USER)
                .stream()
                .anyMatch(user -> user.getIdLong() == event.getJDA().getSelfUser().getIdLong());
    }

    /**
     * Checks if message starts with prefix.
     * <p></p>
     *
     * @param event MessageReceivedEvent to check
     * @return true if message starts with prefix, false otherwise
     */
    private boolean checkPrefix(MessageReceivedEvent event) {
        return event
                .getMessage()
                .getContentRaw()
                .startsWith(configModel
                        .getPrefix());
    }

    /**
     * Sends message to user in private channel.
     * <p></p>
     *
     * @param user    Member to send private message to
     * @param message Message to send
     * @return CompletableFuture<Message> of sent message
     */
    public CompletableFuture<Message> openPrivateChannelAndMessageUser(User user, String message) {
        return user
                .openPrivateChannel()
                .flatMap(privateChannel -> privateChannel.sendMessage(message))
                .submit();
    }

    /**
     * Sends message to user in private channel.
     * <p></p>
     *
     * @param user    Member to send private message to
     * @param message MessageEmbed to send
     * @return CompletableFuture<Message> of sent message
     */
    public CompletableFuture<Message> openPrivateChannelAndMessageUser(User user, MessageEmbed message) {
        return user
                .openPrivateChannel()
                .flatMap(privateChannel -> privateChannel.sendMessageEmbeds(message))
                .submit();
    }

    /**
     * Sends message to user in private channel.
     * <p></p>
     *
     * @param user    Member to send private message to
     * @param message MessageCreateData to send
     * @return CompletableFuture<Message> of sent message
     */
    public CompletableFuture<Message> openPrivateChannelAndMessageUser(User user, MessageCreateData message) {
        return user
                .openPrivateChannel()
                .flatMap(privateChannel -> privateChannel.sendMessage(message))
                .submit();
    }

    /**
     * Sends message to user in private channel or in channel where command was invoked.
     * <p></p>
     *
     * @param ctx     CommandContext to get member from
     * @param message Message to send
     * @return CompletableFuture<Message> of sent message
     */
    public CompletableFuture<Message> respondToUser(CommandContext ctx, String message) {
        return configModel.isRespondInDirectMessage() ?
                openPrivateChannelAndMessageUser(ctx.getMember().getUser(), message) :
                ctx.getMessage().reply(message).submit();
    }

    /**
     * Sends message to user in private channel or in channel where command was invoked.
     * <p></p>
     *
     * @param ctx     CommandContext to get member from
     * @param message MessageEmbed to send
     * @return CompletableFuture<Message> of sent message
     */
    public CompletableFuture<Message> respondToUser(CommandContext ctx, MessageEmbed message) {
        return configModel.isRespondInDirectMessage() ?
                openPrivateChannelAndMessageUser(ctx.getMember().getUser(), message) :
                ctx.getMessage().replyEmbeds(message).submit();
    }

    /**
     * Sends message to user in private channel or in channel where command was invoked.
     * <p></p>
     *
     * @param ctx     CommandContext to get member from
     * @param message MessageCreateData to send
     * @return CompletableFuture<Message> of sent message
     */
    public CompletableFuture<Message> respondToUser(CommandContext ctx, MessageCreateData message) {
        return configModel.isRespondInDirectMessage() ?
                openPrivateChannelAndMessageUser(ctx.getMember().getUser(), message) :
                ctx.getMessage().reply(message).submit();
    }

    /**
     * Sends message to user in private channel or in channel where command was invoked.
     * <p></p>
     *
     * @param ctx      CommandContext to get member from
     * @param event    SlashCommandInteractionEvent to get hook from
     * @param eBuilder Message to send in form of EmbedBuilder
     * @return CompletableFuture<Message> of sent message
     */
    public CompletableFuture<Message> respondToUser(CommandContext ctx, SlashCommandInteractionEvent event, EmbedBuilder eBuilder) {
        return ctx != null ?
                respondToUser(ctx, eBuilder.build()) :
                event.getHook().sendMessageEmbeds(eBuilder.build()).setEphemeral(true).submit();
    }


    /**
     * Sends message to user in private channel or in channel where command was invoked.
     * <p></p>
     *
     * @param ctx     CommandContext to get member from
     * @param event   SlashCommandInteractionEvent to get hook from
     * @param message Message to send in form of MessageCreateData
     * @return CompletableFuture<Message> of sent message
     */
    public CompletableFuture<Message> respondToUser(CommandContext ctx, SlashCommandInteractionEvent event, MessageCreateData message) {
        return ctx != null ?
                respondToUser(ctx, message) :
                event.getHook().sendMessage(message).setEphemeral(true).submit();
    }

    /**
     * Edits original message in private channel or in channel where command was invoked.
     * <p></p>
     *
     * @param originalMessage Message to edit
     * @param event           SlashCommandInteractionEvent to get hook from
     * @param message         Message to send in form of MessageCreateData
     */
    public CompletableFuture<Message> editOryginalMessage(Message originalMessage, SlashCommandInteractionEvent event, MessageEditData message) {
        return event == null ?
                editUserMessage(originalMessage, message) :
                event.getHook().editOriginal(message).submit();
    }

    private CompletableFuture<Message> editUserMessage(Message originalMessage, MessageEditData message) {
        try {
            return originalMessage.editMessage(message).submit();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Get raw command content from MessageReceivedEvent when command is invoked.
     * <p></p>
     *
     * @param event MessageReceivedEvent to get command from
     * @return raw command content
     */
    public String rawCommandContent(MessageReceivedEvent event) {
        String command = event.getMessage().getContentRaw();
        if (fireStoreService.getModel().isRespondByPrefix()) {
            command = command.replace(fireStoreService.getModel().getPrefix(), "");
        } else {
            //delete bot mention
            command = command.replace("<@" + event.getJDA().getSelfUser().getId() + ">", "");
        }
        return command.trim();
    }

    /**
     * Bans users from guild.
     * <p></p>
     *
     * @param toBeBannedIds List of users to be banned
     * @param guild         Guild to ban users from
     */
    public void banUsers(List<String> toBeBannedIds, Guild guild, int time, TimeUnit timeUnit, String reason, boolean tempBan) {
        ConfigModel configModel = fireStoreService.getModel();
        MessageChannel logChannel;
        MessageChannel echoChannel;
        MessageChannel channel = guild.getChannelById(MessageChannel.class, configModel.getChannelIdToSendDeletedMessages());

        if (!configModel.getChannelIdToSendLog().isEmpty())
            logChannel = guild.getChannelById(MessageChannel.class, configModel.getChannelIdToSendLog());
        else {
            logChannel = null;
        }
        if (!configModel.getChannelIdToSendEchoLog().isEmpty())
            echoChannel = guild.getChannelById(MessageChannel.class, configModel.getChannelIdToSendEchoLog());
        else {
            echoChannel = null;
        }

        toBeBannedIds.forEach(id -> {
                    guild.retrieveMemberById(id).queue(member -> {
                        if (tempBan)
                            tempBanUser(member.getUser(), channel, guild, time, timeUnit, reason.isEmpty() ? "" : reason);
                        else
                            banUser(member.getUser(), channel, guild, reason.isEmpty() ? "" : reason);
                        StringBuilder sb = new StringBuilder();
                        sb
                                .append("<@")
                                .append(id)
                                .append("> - ")
                                .append(member.getUser().getName())
                                .append(" - ")
                                .append(tempBan ? "tempban" : "ban");
                        if (time != 0)
                            sb.append(" ")
                                    .append(time)
                                    .append(" ")
                                    .append(timeUnit.toString());
                        sb.append(" - ").append(reason.isEmpty() ? "No reason" : reason);
                        if (logChannel != null)
                            logChannel.sendMessage(sb.toString()).queue();
                        if (echoChannel != null)
                            echoChannel.sendMessage(sb.toString()).queue();
                    });
                }
        );
    }

    public void kickUsers(List<String> toBeKicked, Guild guild, String reason) {
        ConfigModel configModel = fireStoreService.getModel();
        MessageChannel logChannel;
        MessageChannel echoChannel;
        MessageChannel channel = guild.getChannelById(MessageChannel.class, configModel.getChannelIdToSendDeletedMessages());
        assert channel != null;

        if (!configModel.getChannelIdToSendLog().isEmpty())
            logChannel = guild.getChannelById(MessageChannel.class, configModel.getChannelIdToSendLog());
        else {
            logChannel = null;
        }
        if (!configModel.getChannelIdToSendEchoLog().isEmpty())
            echoChannel = guild.getChannelById(MessageChannel.class, configModel.getChannelIdToSendEchoLog());
        else {
            echoChannel = null;
        }
        toBeKicked.forEach(
                id -> guild.retrieveMemberById(id).queue(member -> {
                    kickUser(member.getUser(), channel, guild, reason);
                    StringBuilder sb = new StringBuilder();
                    sb
                            .append("<@")
                            .append(id)
                            .append("> - ")
                            .append(member.getUser().getName())
                            .append(" - ")
                            .append("kick")
                            .append(" ")
                            .append(" - ")
                            .append(reason.isEmpty() ? "No reason" : reason);
                    if (logChannel != null)
                        logChannel.sendMessage(sb.toString()).queue();
                    if (echoChannel != null)
                        echoChannel.sendMessage(sb.toString()).queue();
                })
        );
    }

    /**
     * Checks if member should be excluded from bot actions.
     * <p></p>
     *
     * @param member Member to check
     * @return true if member should be excluded from bot actions, false otherwise
     */
    public boolean performMemberCheck(Member member) {
        List<String> excludedRoles = fireStoreService.getModel().getExcludedRoleIds();
        List<String> excludedMembers = fireStoreService.getModel().getExcludedUserIds();

        if (member.getUser().isBot())
            return true;
        if (member.getRoles().stream()
                .map(Role::getId)
                .anyMatch(excludedRoles::contains))
            return true;
        return excludedMembers.contains(member.getUser().getId());
    }

    /**
     * Kick member from guild.
     * <p></p>
     *
     * @param user User to check
     * @param channel channel to send message to
     * @param guild guild to kick user from
     * @param reason reason of kick
     */
    private void kickUser(@NonNull User user, MessageChannel channel, Guild guild, String reason){
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(
                user.getName() + " has been kicked",
                null,
                user.getAvatarUrl());
        embedBuilder.setDescription("**Reason:** " + reason);
        embedBuilder.setColor(0x5acff5);

        MessageCreateData message = new MessageCreateBuilder()
                .setEmbeds(embedBuilder.build())
                .build();

        channel.sendMessage(message).queue(m -> {
            guild.kick(user).queue(s -> {
                EmbedBuilder builder = new EmbedBuilder();
                builder.setTitle("Kicked");
                builder.setDescription("You have been kicked from " + guild.getName());
                builder.addField("Reason", reason, false);
                builder.setColor(Color.RED);
                openPrivateChannelAndMessageUser(user, builder.build()).exceptionally(throwable -> {
                    System.out.println("Throwable: " + throwable.getMessage());
                    return null;
                });
            });
        });
    }


    /**
     * Temp ban member from guild.
     * <p></p>
     *
     * @param user User to check
     * @param channel channel to send message to
     * @param guild guild to ban user from
     * @param time time of ban
     * @param timeUnit time unit of ban
     * @param reason reason of ban
     */
    private void tempBanUser(@NonNull User user, MessageChannel channel, Guild guild, long time, TimeUnit timeUnit, String reason) {
        TempBanModel tempBanModel = new TempBanModel();
        long timeInMillis = System.currentTimeMillis() + timeUnit.toMillis(time);

        //get time difference between current time and ban time in days
        Date date = new Date(timeInMillis);
        long days = (date.getTime() - System.currentTimeMillis()) / (1000 * 60 * 60 * 24);
        long weeks = days / 7;
        days = days % 7;

        tempBanModel.setUserId(user.getId());
        tempBanModel.setAvatarUrl(user.getAvatarUrl());
        tempBanModel.setUserName(user.getName());
        tempBanModel.setReason(reason);
        tempBanModel.setBanTime(timeInMillis);


        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(
                tempBanModel.getUserName() + " has been banned for " + weeks + " weeks and " + days + " days",
                null,
                tempBanModel.getAvatarUrl());
        embedBuilder.setDescription("**Reason:** " + tempBanModel.getReason());
        DateFormat f = new SimpleDateFormat("dd.MM.yyyy");
        embedBuilder.setFooter("Baned until | " + f.format(date));
        embedBuilder.setColor(0x5acff5);

        Button button = Button.danger("unban", "Unban");

        MessageCreateData message = new MessageCreateBuilder()
                .setEmbeds(embedBuilder.build())
                .setActionRow(button)
                .build();

        long finalDays = days;
        channel.sendMessage(message).queue(m -> {
            tempBanModel.setMessageId(m.getId());
            fireStoreService.setTempBanModel(tempBanModel);
            guild.ban(user, 0, TimeUnit.SECONDS)
                    .reason(reason)
                    .queue(s -> {
                        EmbedBuilder builder = new EmbedBuilder();
                        builder.setTitle("Banned");
                        builder.setDescription("You have been banned from " + guild.getName() + " for " + weeks + " weeks and " + finalDays + " days");
                        builder.addField("Reason", reason, false);
                        builder.setColor(Color.RED);
                        openPrivateChannelAndMessageUser(user, builder.build());
                    });
        });
    }

    private void banUser(@NonNull User user, MessageChannel channel, Guild guild, String reason) {
        TempBanModel tempBanModel = new TempBanModel();
        tempBanModel.setUserId(user.getId());
        tempBanModel.setAvatarUrl(user.getAvatarUrl());
        tempBanModel.setUserName(user.getName());
        tempBanModel.setReason(reason);
        Calendar c = Calendar.getInstance();
        c.setTime(new Date(System.currentTimeMillis()));
        c.add(Calendar.YEAR, 69);
        tempBanModel.setBanTime(c.getTimeInMillis());

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(
                user.getName() + " has been banned",
                null,
                user.getAvatarUrl());
        embedBuilder.setDescription("**Reason:** " + tempBanModel.getReason());
        embedBuilder.setColor(0x5acff5);

        Button button = Button.danger("unban", "Unban");

        MessageCreateData message = new MessageCreateBuilder()
                .setEmbeds(embedBuilder.build())
                .setActionRow(button)
                .build();

        channel.sendMessage(message).queue(m -> {
            tempBanModel.setMessageId(m.getId());
            fireStoreService.setTempBanModel(tempBanModel);
            guild.ban(user, 0, TimeUnit.SECONDS)
                    .reason(reason)
                    .queue(s -> {
                        EmbedBuilder builder = new EmbedBuilder();
                        builder.setTitle("Banned");
                        builder.setDescription("You have been banned from " + guild.getName());
                        builder.addField("Reason", reason, false);
                        builder.setColor(Color.RED);
                        openPrivateChannelAndMessageUser(user, builder.build()).exceptionally(throwable -> {
                            System.out.println("Throwable: " + throwable.getMessage());
                            return null;
                        });
                    }, throwable -> {
                        System.out.println("Throwable: " + throwable.getMessage());
                    });
        });
    }

    /**
     * Checks if member has mentionable nickname.
     * <p></p>
     *
     * @param member Member to check
     * @return true if member has mentionable nickname, false otherwise
     */
    public boolean hasMentionableNickName(Member member) {
        String nick = member.getEffectiveName();
        if (checkAuthority(member))
            return true;

        if (fireStoreService.checkIfWhitelisted(member.getUser().getId(), nick))
            return true;

        float mentionableCharsPercent = fireStoreService.getModel().getMentionableCharsPercent();
        float mentionableSegmentRatio = fireStoreService.getModel().getMentionableSegmentRatio();
        StringBuilder filteredUsername = new StringBuilder();
        AtomicInteger mentionable = new AtomicInteger();
        nick.chars().forEach(c -> {
            if (mentionableCharacters.contains(String.valueOf((char) c))) {
                filteredUsername.append((char) c);
                mentionable.getAndIncrement();
            } else {
                filteredUsername.append(" ");
            }
        });
        float percent = (float) (mentionable.get() * 100) / nick.length();
        if (percent < mentionableCharsPercent) return false;

        int maxSubNickLength = Arrays.stream(
                        filteredUsername
                                .toString()
                                .strip()
                                .split(" "))
                .filter(s -> !s.isEmpty())
                .map(String::length)
                .max(Integer::compareTo)
                .orElse(0);

        percent = (float) (maxSubNickLength * 100) / mentionable.get();
        return percent >= mentionableSegmentRatio;
    }


    /**
     * Bobify the given member.
     * <p></p>
     *
     * @param member Member to bobify
     */
    public void bobify(Member member) {
        try {
            MessageCreateData data = getBobifyMessageSentToUser(member);
            member.modifyNickname("Bob").queue();
            openPrivateChannelAndMessageUser(member.getUser(), data);
        } catch (Exception e) {
            System.err.println("User is higher in hierarchy than bot");
        }
    }

    @NotNull
    private static MessageCreateData getBobifyMessageSentToUser(Member member) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Nieoznaczalny nick");
        embedBuilder.addField("Nieoznaczalny nick", member.getEffectiveName(), false);
        embedBuilder.setDescription("No cześć! Zdaje mi się, że Twój nick - **" + member.getEffectiveName() + "** - nie jest oznaczalny.\n" +
                "Według punktu 5. regulaminu serwera, musisz zmienić swój nick.\n" +
                "Na ten moment nazywasz się **BOB**. Jeżeli Ci to pasuje - zajebiście, będziemy się tak do Ciebie zwracać.\n"
                + "Jeżeli jednak nie chcesz zostać do końca swojego życia Bobem, możesz w każdej chwili zmienić swój nick.\n");
        embedBuilder.setColor(Color.PINK);
        Button button = Button.primary("appeal", "Odwołaj się");
        return new MessageCreateBuilder().setEmbeds(embedBuilder.build()).setActionRow(button).build();
    }


    /**
     * Get list of aliases as String.
     * <p></p>
     *
     * @param aliases List of aliases to get String from
     * @return String of aliases separated by comma
     */
    public String createAliasString(List<String> aliases) {
        return aliases
                .stream()
                .map(s -> "`" + s + "`")
                .reduce((s, s2) -> s + ", " + s2)
                .orElse("No aliases");

    }

    /**
     * Get member from ButtonInteractionEvent.
     * If event is not from guild, get member from user id.
     * <p></p>
     *
     * @param event ButtonInteractionEvent to get member from
     * @return String of aliases separated by comma
     */
    @Nullable
    public Member getMemberFromButtonEvent(ButtonInteractionEvent event) {
        return event.getGuild() == null ?
                getMemberFromPrivateChannel(event) :
                event.getMember();
    }

    private Member getMemberFromPrivateChannel(ButtonInteractionEvent event) {
        try {
            return Objects.requireNonNull(
                            event.getJDA()
                                    .getGuildById(fireStoreService.getEnvironmentModel().getGUILD_ID()))
                    .retrieveMemberById(
                            event.getInteraction()
                                    .getUser()
                                    .getId())
                    .complete();
        } catch (Exception e) {
            return null;
        }
    }


    public <T extends BasicCacheModel> void deleteMessage(JDA jda, T model) {
        new Thread(() -> {
            if (model.isPrivateChannel()) {
                editMessageInPrivateChannel(jda, model);
            } else {
                editMessageInGuildTextChannel(jda, model);
            }
        }).start();
    }

    private static <T extends BasicCacheModel> void editMessageInGuildTextChannel(JDA jda, T model) {
        try {
            TextChannel textChannel = jda.getTextChannelById(model.getChannelId());
            Objects.requireNonNull(textChannel).retrieveMessageById(model.getMessageID()).queue(message -> {
                message.editMessage(getDeletedMessageEditBuilder().build()).queue();
            });
        } catch (Exception e) {
            System.out.println("Channel is null");
        }


    }

    private static <T extends BasicCacheModel> void editMessageInPrivateChannel(JDA jda, T model) {
        try {
            PrivateChannel privateChannel = jda.getPrivateChannelById(model.getChannelId());
            Objects.requireNonNull(privateChannel).retrieveMessageById(model.getMessageID()).queue(message -> {
                message.editMessage(getDeletedMessageEditBuilder().build()).queue();
            });
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @NotNull
    private static MessageEditBuilder getDeletedMessageEditBuilder() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("This message has been deleted");
        builder.setColor(Color.BLUE);
        return new MessageEditBuilder().setEmbeds(builder.build());
    }

    public void deleteTrigger(CommandContext ctx) {
        boolean deleteTriggerMessage = fireStoreService.getModel().isDeleteTriggerMessage();
        if (deleteTriggerMessage)
            ctx.getMessage().delete().queue();
    }


}

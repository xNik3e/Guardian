package pl.xnik3e.Guardian.Models;

import lombok.Data;

@Data
public class LogModel {
    private String rawMessage;
    private String messageId;
    private String channelName;
    private String channelId;
    private String guildName;
    private String GuildId;
    private String authorName;
    private String authorId;
    private String action;
    private long time;
}

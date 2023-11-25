package pl.xnik3e.Guardian.Models;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BasicCacheModel {
    protected boolean isPrivateChannel;
    protected String userID;
    protected String channelId;
    protected String messageID;
    protected int allEntries;
    protected long timestamp;
    protected List<Map<String, String>> maps;
}

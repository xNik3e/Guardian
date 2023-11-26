package pl.xnik3e.Guardian.Models;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BasicCacheModel {
    protected boolean isPrivateChannel;         // true: private channel, false: public channel
    protected String userID;                    // author id
    protected String channelId;                 // channel id
    protected String messageID;                 // message id
    protected int allEntries;                   // all map entries
    protected long timestamp;                   // when to delete message
    protected List<Map<String, String>> maps;   // data
}

package pl.xnik3e.Guardian.Models;

import lombok.Data;

@Data
public class BasicCacheModel {
    protected String messageID;
    protected int allEntries;
    protected long timestamp;
}

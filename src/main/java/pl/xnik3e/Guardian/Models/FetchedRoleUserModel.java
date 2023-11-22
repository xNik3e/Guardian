package pl.xnik3e.Guardian.Models;

import lombok.Data;

@Data
public class FetchedRoleUserModel {
    private String messageID;
    private String userID;
    private Integer ordinal;
    private long timestamp;
    private String value;
}

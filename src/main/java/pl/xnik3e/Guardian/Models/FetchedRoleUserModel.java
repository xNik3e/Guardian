package pl.xnik3e.Guardian.Models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FetchedRoleUserModel {
    private String messageID;
    private String userID;
    private Integer ordinal;
    private long timestamp;
    private String value;
    private Integer allEntries;
    private String roleID;
    private String roleName;

}

package pl.xnik3e.Guardian.Models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FetchedRoleModel {
    private String messageID;
    private List<Map<String, String>> users;
    private int allEntries;
    private long timestamp;
    private String roleID;
    private String roleName;

}

package pl.xnik3e.Guardian.Models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FetchedRoleModel extends BasicCacheModel{
    private List<Map<String, String>> users;
    private String roleID;
    private String roleName;

}

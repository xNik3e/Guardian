package pl.xnik3e.Guardian.Models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.List;

@Data
public class FetchedRoleModel extends BasicCacheModel{
    private String roleID;
    private String roleName;


}

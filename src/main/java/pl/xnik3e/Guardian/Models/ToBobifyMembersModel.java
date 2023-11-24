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
public class ToBobifyMembersModel extends BasicCacheModel{
    private List<Map<String, String>> users;
}

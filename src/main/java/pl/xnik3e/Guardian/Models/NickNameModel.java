package pl.xnik3e.Guardian.Models;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class NickNameModel {
    private List<String> nickName;
    private String userID;

    public NickNameModel() {
        nickName = new ArrayList<>();
    }

}

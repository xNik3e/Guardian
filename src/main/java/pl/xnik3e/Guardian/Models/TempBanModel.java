package pl.xnik3e.Guardian.Models;


import lombok.Data;

@Data
public class TempBanModel {
    String messageId;
    String userId;
    String avatarUrl;
    String userName;
    String reason;
    Long banTime;

}

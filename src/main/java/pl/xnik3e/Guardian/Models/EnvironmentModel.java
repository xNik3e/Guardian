package pl.xnik3e.Guardian.Models;

import lombok.Data;

@Data
public class EnvironmentModel {
    private String TOKEN;
    private String GUILD_ID;

    public void updateEnvironmentModel(EnvironmentModel updatedModel) {
        this.TOKEN = updatedModel.TOKEN;
        this.GUILD_ID = updatedModel.GUILD_ID;
    }
}

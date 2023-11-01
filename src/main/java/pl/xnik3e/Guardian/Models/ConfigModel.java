package pl.xnik3e.Guardian.Models;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ConfigModel {
    //config model
    private boolean isInit;
    private boolean afterStartupInit;
    private boolean respondByPrefix;
    private String prefix = "";
    private List<String> excludedRoleIds;
    private List<String> excludedChannelIds;
    private List<String> excludedUserIds;
    private List<String> rolesToDelete;

    private String channelIdToSendLog;
    private String channelIdToSendDeletedMessages;

    public ConfigModel() {
        this.excludedChannelIds = new ArrayList<>(); //Channels to exclude from operating
        this.excludedRoleIds = new ArrayList<>(); //Roles that have granted special privileges
        this.excludedUserIds = new ArrayList<>(); //Users that have granted special privileges
        this.rolesToDelete = new ArrayList<>(); //Included user roles that will result in member being banned
        getDefaultConfig();
    }

    public void updateConfigModel(ConfigModel model){
        this.isInit = model.isInit;
        this.afterStartupInit = model.afterStartupInit;
        this.respondByPrefix = model.respondByPrefix;
        this.prefix = model.prefix;

        this.excludedRoleIds.clear();
        this.excludedRoleIds.addAll(model.excludedRoleIds);

        this.excludedChannelIds.clear();
        this.excludedChannelIds.addAll(model.excludedChannelIds);

        this.excludedUserIds.clear();
        this.excludedUserIds.addAll(model.excludedUserIds);

        this.rolesToDelete.clear();
        this.rolesToDelete.addAll(model.rolesToDelete);
    }

    public void getDefaultConfig(){
        this.isInit = false;
        this.afterStartupInit = false;
        this.respondByPrefix = true;
        this.prefix = "&";
        this.channelIdToSendLog = "1123245083798552657"; //TODO: TEMP CHANGE LATER
        this.channelIdToSendDeletedMessages = "1123245083798552657"; //TODO: TEMP CHANGE LATER


        this.excludedChannelIds.clear();
        this.excludedRoleIds.clear();
        this.rolesToDelete.clear();
        this.excludedUserIds.clear();
        this.rolesToDelete.add("1164645019769131029"); //<16

        //excluded roles
        this.excludedRoleIds.add("372811196627156993"); //Moderator
        this.excludedRoleIds.add("672490701769932801"); //Pomocnik Administracji
        this.excludedRoleIds.add("379296005385879553"); //Administrator
        this.excludedRoleIds.add("451069025607352320"); //PIO
    }
}

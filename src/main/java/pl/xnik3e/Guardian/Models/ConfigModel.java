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
    private boolean respondInDirectMessage;
    private boolean deleteTriggerMessage;
    private String prefix = "";
    private List<String> excludedRoleIds;
    private List<String> excludedChannelIds;
    private List<String> excludedUserIds;
    private List<String> rolesToDelete;

    private String channelIdToSendLog;
    private String channelIdToSendEchoLog;
    private String channelIdToSendDeletedMessages;

    private String defaultRoleId;

    private float mentionableCharsPercent = 65f;
    private float mentionableSegmentRatio = 33f;

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
        this.respondInDirectMessage = model.respondInDirectMessage;
        this.deleteTriggerMessage = model.deleteTriggerMessage;
        this.prefix = model.prefix;

        this.channelIdToSendLog = model.channelIdToSendLog;
        this.channelIdToSendEchoLog = model.channelIdToSendEchoLog;
        this.channelIdToSendDeletedMessages = model.channelIdToSendDeletedMessages;

        this.defaultRoleId = model.defaultRoleId;

        this.mentionableCharsPercent = model.mentionableCharsPercent;
        this.mentionableSegmentRatio = model.mentionableSegmentRatio;

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
        this.respondInDirectMessage = true;
        this.deleteTriggerMessage = true;
        this.prefix = "&";
        this.channelIdToSendLog = ""; //TODO: TEMP CHANGE LATER
        this.channelIdToSendDeletedMessages = ""; //TODO: TEMP CHANGE LATER
        this.channelIdToSendEchoLog = ""; //TODO: TEMP CHANGE LATER

        this.defaultRoleId = "1174464585391157410"; //TODO: TEMP CHANGE LATER TO 1059877981776003233

        this.mentionableCharsPercent = 65f;
        this.mentionableSegmentRatio = 33f;

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
        this.excludedRoleIds.add("1174464585391157410"); //TODO: TEMP ROLE FOR DEBUGGING

        //excluded users
        this.excludedUserIds.add("428233609342746634"); //TODO: DEBUGGING
    }
}

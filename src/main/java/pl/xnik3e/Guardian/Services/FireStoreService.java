package pl.xnik3e.Guardian.Services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.xnik3e.Guardian.Models.*;

import javax.print.Doc;
import java.rmi.server.UID;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Getter
@Service
public class FireStoreService {

    private final Firestore firestore;
    private final ConfigModel model;
    private final EnvironmentModel environmentModel;

    @Autowired
    public FireStoreService(Firestore firestore) {
        this.firestore = firestore;
        this.model = new ConfigModel();
        this.environmentModel = new EnvironmentModel();
        fetchConfigModel();
        fetchEnvironmentModel();
        attachListeners();
    }


    private void attachListeners() {
        //Add listener to config to get updates
        firestore.collection("config").document("config").addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                System.err.println("Listen failed: " + e);
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                ConfigModel tempModel = snapshot.toObject(ConfigModel.class);
                assert tempModel != null;
                model.updateConfigModel(tempModel);
            } else {
                //Insert default config
                System.out.print("Current data: null");
                this.model.getDefaultConfig();
                updateConfigModel();
            }
        });


        firestore.collection("global_config").document("environment").addSnapshotListener((snapshot, e) -> {
            if(e != null) {
                System.err.println("Listen failed: " + e);
                return;
            }
            if(snapshot != null && snapshot.exists()) {
                EnvironmentModel tempModel = snapshot.toObject(EnvironmentModel.class);
                assert tempModel != null;
                environmentModel.updateEnvironmentModel(tempModel);
            } else {
                System.out.print("Current data: null");
            }
        });
    }

    private synchronized void fetchConfigModel() {
        ApiFuture<DocumentSnapshot> future = firestore.collection("config").document("config").get();
        Thread thread = new Thread(() -> {
            try {
                DocumentSnapshot document = future.get();
                ConfigModel updatedModel = document.toObject(ConfigModel.class);
                assert updatedModel != null : "Config model is null";
                model.updateConfigModel(updatedModel);
            } catch (Exception e) {
                System.out.println("Error fetching model from firestore");
            }
        });
        thread.start();
    }

    private void fetchEnvironmentModel() {
        ApiFuture<DocumentSnapshot> future = firestore.collection("global_config").document("environment").get();
        try {
            DocumentSnapshot document = future.get();
            EnvironmentModel updatedModel = document.toObject(EnvironmentModel.class);
            assert updatedModel != null : "Environment model is null";
            environmentModel.updateEnvironmentModel(updatedModel);
        } catch (Exception e) {
            System.out.println("Error fetching model from firestore");
        }
    }


    public void updateConfigModel() {
        ApiFuture<WriteResult> future = firestore.collection("config").document("config").set(model);
        if (future.isDone()) {
            System.out.println("Updated config");
        }
    }


    public void setTempBanModel(TempBanModel banModel){
        ApiFuture<WriteResult> future = firestore.collection("tempbans").document(banModel.getMessageId()).set(banModel);
        if(future.isDone()){
            System.out.println("Added tempban model");
        }
    }

    public TempBanModel fetchBanModel(String messageId){
        ApiFuture<DocumentSnapshot> future = firestore.collection("tempbans").document(messageId).get();
        try {
            DocumentSnapshot document = future.get(5, TimeUnit.SECONDS);
            return document.toObject(TempBanModel.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void deleteBanModel(String messageId){
        ApiFuture<WriteResult> future = firestore.collection("tempbans").document(messageId).delete();
        if(future.isDone()){
            System.out.println("Deleted tempban model");
        }
    }


    public List<TempBanModel> queryBans(){
        long now = System.currentTimeMillis();
        List<TempBanModel> tempBanModels = new ArrayList<>();
        ApiFuture<QuerySnapshot> future = firestore.collection("tempbans").whereLessThan("banTime", now).get();
        try{
            future.get(10, TimeUnit.SECONDS).getDocuments().forEach(document -> {
                TempBanModel tempBanModel = document.toObject(TempBanModel.class);
                if(tempBanModel != null){
                    tempBanModels.add(tempBanModel);
                }
            });
            return tempBanModels;
        }catch (Exception e){
            return null;
        }
    }

    public NickNameModel getNickNameModel(String userID){
        ApiFuture<DocumentSnapshot> future = firestore.collection("whitelist").document(userID).get();
        try{
           return future.get(5, TimeUnit.SECONDS).toObject(NickNameModel.class);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean checkIfWhitelisted(String UID, String nickName){
        ApiFuture<DocumentSnapshot> future = firestore.collection("whitelist").document(UID).get();
        try{
            DocumentSnapshot snapshot = future.get(5, TimeUnit.SECONDS);
            return snapshot.toObject(NickNameModel.class).getNickName().contains(nickName);
        } catch (Exception e) {
            return false;
        }
    }

    public void deleteNickNameModel(String userID){
        ApiFuture<WriteResult> future = firestore.collection("whitelist").document(userID).delete();
        if(future.isDone()){
            System.out.println("Deleted nickname model");
        }
    }

    public void addNickModel(NickNameModel model) {
        ApiFuture<WriteResult> future = firestore.collection("whitelist").document(model.getUserID()).set(model);
        if(future.isDone()){
            System.out.println("Added nickname model");
        }
    }

    public void updateNickModel(NickNameModel model){
        ApiFuture<DocumentSnapshot> future = firestore.collection("whitelist").document(model.getUserID()).get();
        try {
            NickNameModel nickModel = future.get(5, TimeUnit.SECONDS).toObject(NickNameModel.class);
            if(nickModel != null && !nickModel.getNickName().contains(model.getNickName().get(0)))
                nickModel.getNickName().add(model.getNickName().get(0));

            addNickModel(nickModel);
        } catch (Exception e) {
            System.err.println("Error fetching model from firestore. Adding new model");
            addNickModel(model);
        }
    }

    public List<String> getWhitelistedNicknames(String userID){
        ApiFuture<DocumentSnapshot> future = firestore.collection("whitelist").document(userID).get();
        try{
            return future.get(5, TimeUnit.SECONDS).toObject(NickNameModel.class).getNickName();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setFetchedRoleUserList(List<FetchedRoleUserModel> fetchedRoleUserList){
        new Thread(() -> {
            int ordinal = 0;
            for(FetchedRoleUserModel model : fetchedRoleUserList){
                model.setOrdinal(++ordinal);
                ApiFuture<DocumentReference> future = firestore.collection("cache")
                        .document("fetchedRoleUserList")
                        .collection("fetchedIds")
                        .add(model);
            }
            System.out.println("Added fetched IDs");
        }).start();
    }

    public Optional<List<FetchedRoleUserModel>> fetchFetchedRoleUserList(int page){
       List<FetchedRoleUserModel> fetchedRoleUserModels = new ArrayList<>();
            ApiFuture<QuerySnapshot> future = firestore.collection("cache")
                    .document("fetchedRoleUserList")
                    .collection("fetchedIds")
                    .orderBy("ordinal")
                    .startAt((page-1) * 25)
                    .endAt(page * 25)
                    .get();
            try{
                future.get(5, TimeUnit.SECONDS).getDocuments().forEach(document -> {
                    FetchedRoleUserModel model = document.toObject(FetchedRoleUserModel.class);
                    if(model != null){
                        fetchedRoleUserModels.add(model);
                    }
                });
                fetchedRoleUserModels.sort(Comparator.comparing(FetchedRoleUserModel::getOrdinal));
                return Optional.of(fetchedRoleUserModels);
            }catch (Exception e){
                return Optional.empty();
            }
    }

    public void deleteFetchedRoleUserList(String messageID){
        new Thread(() -> {
            ApiFuture<QuerySnapshot> future = firestore.collection("cache")
                    .document("fetchedRoleUserList")
                    .collection("fetchedIds")
                    .whereEqualTo("messageID", messageID)
                    .get();
            try {
                future.get(5, TimeUnit.SECONDS).getDocuments().forEach(document -> {
                    document.getReference().delete();
                });
            } catch (Exception e) {
                System.err.println("Error deleting fetchedRoleUserList");
            }
        }).start();
    }
}

package pl.xnik3e.Guardian.Services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.xnik3e.Guardian.Models.ConfigModel;
import pl.xnik3e.Guardian.Models.EnvironmentModel;
import pl.xnik3e.Guardian.Models.NickNameModel;
import pl.xnik3e.Guardian.Models.TempBanModel;

import java.util.ArrayList;
import java.util.List;
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
        NickNameModel model;
        ApiFuture<DocumentSnapshot> future = firestore.collection("whitelist").document(userID).get();
        try{
           return future.get(5, TimeUnit.SECONDS).toObject(NickNameModel.class);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean checkIfWhitelisted(String nickName){
        ApiFuture<QuerySnapshot> future = firestore.collection("whitelist").whereEqualTo("nickName", nickName).get();
        try{
            return !future.get(5, TimeUnit.SECONDS).getDocuments().isEmpty();
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
}

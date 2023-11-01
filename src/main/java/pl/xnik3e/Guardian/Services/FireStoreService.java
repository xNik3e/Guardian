package pl.xnik3e.Guardian.Services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.xnik3e.Guardian.Models.ConfigModel;

import java.util.ArrayList;
import java.util.List;

@Getter
@Service
public class FireStoreService {

    private final Firestore firestore;
    private final ConfigModel model;
    private final List<String> userIds = new ArrayList<>();

    @Autowired
    public FireStoreService(Firestore firestore) {
        this.firestore = firestore;
        this.model = new ConfigModel();
        fetchConfigModel();
        fetchUserIds();
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

        //Add listener to blacklist to get updates
        firestore.collection("blacklist").document("toDelete").addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                System.err.println("Listen failed: " + e);
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                userIds.clear();
                userIds.addAll((List<String>) snapshot.get("userId"));
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

    private synchronized void fetchUserIds() {
        ApiFuture<DocumentSnapshot> future = firestore.collection("blacklist").document("toDelete").get();
        Thread thread = new Thread(() -> {
            try {
                DocumentSnapshot document = future.get();
                userIds.clear();
                userIds.addAll((List<String>) document.get("userId"));
            } catch (Exception e) {
                System.out.println("Error fetching model from firestore");
            }
        });
        thread.start();
    }

    private void updateConfigModel() {
        ApiFuture<WriteResult> future = firestore.collection("config").document("config").set(model);
        if (future.isDone()) {
            System.out.println("Updated config");
        }
    }

    public void updateUserIds(){
        ApiFuture<WriteResult> future = firestore.collection("blacklist").document("toDelete").update("userId", userIds);
        if (future.isDone()) {
            System.out.println("Updated userIds");
        }
    }

    public void switchRespondByPrefix(){
        model.setRespondByPrefix(!model.isRespondByPrefix());
        System.out.println("Respond by prefix: " + (model.isRespondByPrefix() ? "enabled" : "disabled"));
        updateConfigModel();
    }

}

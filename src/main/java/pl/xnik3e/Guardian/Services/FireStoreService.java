package pl.xnik3e.Guardian.Services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import lombok.Getter;
import lombok.NonNull;
import net.dv8tion.jda.api.JDA;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.xnik3e.Guardian.Models.*;
import pl.xnik3e.Guardian.Utils.MessageUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@Getter
@Service
public class FireStoreService {

    private final Firestore firestore;
    private final ConfigModel model;
    private final EnvironmentModel environmentModel;
    private final CollectionReference COLLECTION_CACHE_FETCH;
    private final CollectionReference COLLECTION_CACHE_BOB;
    private final CollectionReference COLLECTION_CACHE_CURSE;
    private final DocumentReference DOCUMENT_CACHE_FETCH;
    private final DocumentReference DOCUMENT_CACHE_BOB;
    private final DocumentReference DOCUMENT_CACHE_CURSE;

    @Autowired
    public FireStoreService(Firestore firestore) {
        this.firestore = firestore;
        this.model = new ConfigModel();
        this.environmentModel = new EnvironmentModel();
        fetchConfigModel();
        fetchEnvironmentModel();
        attachListeners();

        COLLECTION_CACHE_BOB = firestore.collection("cache")
                .document("bobifyUsersCommand")
                .collection("bobifyUsers");
        COLLECTION_CACHE_FETCH = firestore.collection("cache")
                .document("fetchUsersWithRoleCommand")
                .collection("fetchUsers");
        COLLECTION_CACHE_CURSE = firestore.collection("cache")
                .document("curseCommand")
                .collection("curseUsers");
        DOCUMENT_CACHE_BOB = COLLECTION_CACHE_BOB
                .document("toBobifyMembersModel");
        DOCUMENT_CACHE_FETCH = COLLECTION_CACHE_FETCH
                .document("roleModel");
        DOCUMENT_CACHE_CURSE = COLLECTION_CACHE_CURSE
                .document("curseModel");
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
            if (e != null) {
                System.err.println("Listen failed: " + e);
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                EnvironmentModel tempModel = snapshot.toObject(EnvironmentModel.class);
                assert tempModel != null;
                environmentModel.updateEnvironmentModel(tempModel);
            } else {
                System.out.print("Current data: null");
            }
        });
    }

    private synchronized void fetchConfigModel() {
        new Thread(() -> {
            try {
                ConfigModel updatedModel = Objects.requireNonNull(firestore.collection("config")
                        .document("config")
                        .get()
                        .get()
                        .toObject(ConfigModel.class));
                model.updateConfigModel(updatedModel);
            } catch (Exception e) {
                System.out.println("Error fetching model from firestore");
            }
        }).start();
    }

    private void fetchEnvironmentModel() {
        try {
            EnvironmentModel updatedModel = Objects.requireNonNull(firestore.collection("global_config")
                    .document("environment")
                    .get()
                    .get()
                    .toObject(EnvironmentModel.class));
            environmentModel.updateEnvironmentModel(updatedModel);
        } catch (Exception e) {
            System.out.println("Error fetching model from firestore");
        }
    }


    public void updateConfigModel() {
        firestore.collection("config")
                .document("config")
                .set(model);

    }


    public void setTempBanModel(TempBanModel banModel) {
        firestore.collection("tempbans")
                .document(banModel.getMessageId())
                .set(banModel);
    }

    public TempBanModel fetchBanModel(String messageId) {
        try {
            return firestore.collection("tempbans")
                    .document(messageId)
                    .get()
                    .get(5, TimeUnit.SECONDS)
                    .toObject(TempBanModel.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void deleteBanModel(String messageId) {
        firestore.collection("tempbans")
                .document(messageId)
                .delete();
    }


    public List<TempBanModel> queryBans() {
        try {
            return firestore.collection("tempbans")
                    .whereLessThan("banTime", System.currentTimeMillis())
                    .get()
                    .get(10, TimeUnit.SECONDS)
                    .getDocuments().stream()
                    .map(documentSnapshot -> documentSnapshot.toObject(TempBanModel.class))
                    .toList();
        } catch (Exception e) {
            return null;
        }
    }

    public NickNameModel fetchNickNameModel(String userID) {
        try {
            return firestore.collection("whitelist")
                    .document(userID)
                    .get()
                    .get(5, TimeUnit.SECONDS)
                    .toObject(NickNameModel.class);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean checkIfWhitelisted(String UID, String nickName) {
        try {
            return Objects.requireNonNull(firestore.collection("whitelist")
                            .document(UID)
                            .get()
                            .get(5, TimeUnit.SECONDS)
                            .toObject(NickNameModel.class))
                    .getNickName()
                    .contains(nickName);
        } catch (Exception e) {
            return false;
        }
    }

    public void deleteNickNameModel(String userID) {
        firestore.collection("whitelist")
                .document(userID)
                .delete();
    }

    public void setNickModel(NickNameModel model) {
        firestore.collection("whitelist")
                .document(model.getUserID())
                .set(model);
    }

    public void updateNickModel(NickNameModel model) {
        try {
            NickNameModel nickModel = Objects.requireNonNull(firestore.collection("whitelist")
                    .document(model.getUserID())
                    .get()
                    .get(5, TimeUnit.SECONDS)
                    .toObject(NickNameModel.class));

            if (!nickModel.getNickName().contains(model.getNickName().get(0)))
                nickModel.getNickName().add(model.getNickName().get(0));
            setNickModel(nickModel);
        } catch (Exception e) {
            System.err.println("Error fetching model from firestore. Adding new model");
            setNickModel(model);
        }
    }

    public List<String> getWhitelistedNicknames(String userID) {
        try {
            return Objects.requireNonNull(firestore.collection("whitelist")
                            .document(userID)
                            .get()
                            .get(5, TimeUnit.SECONDS)
                            .toObject(NickNameModel.class))
                    .getNickName();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public <T extends BasicCacheModel> void setCacheModel(T model) {
        new Thread(() -> {
            try {
                Objects.requireNonNull(
                                getGenericDocumentReference(model.getClass()))
                        .set(model);
            } catch (Exception e) {
                System.err.println("Error while setting cache model");
            }
        }).start();
    }

    @NotNull
    private <T extends BasicCacheModel> ApiFuture<DocumentSnapshot> getDocumentSnapshotApiFuture(@NotNull Class<T> modelClass) {
        return Objects.requireNonNull(getGenericDocumentReference(modelClass))
                .get();
    }

    private <T extends BasicCacheModel> DocumentReference getGenericDocumentReference(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(FetchedRoleModel.class)) {
            return DOCUMENT_CACHE_FETCH;
        } else if (modelClass.isAssignableFrom(ToBobifyMembersModel.class)) {
            return DOCUMENT_CACHE_BOB;
        } else if (modelClass.isAssignableFrom(CurseModel.class)) {
            return DOCUMENT_CACHE_CURSE;
        }
        return null;
    }

    public <T extends BasicCacheModel> Optional<T> fetchAllCache(@NonNull Class<T> modelClass) {
        try {
            return Optional.ofNullable(Objects.requireNonNull(getGenericDocumentReference(modelClass))
                    .get()
                    .get(10, TimeUnit.SECONDS)
                    .toObject(modelClass));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public <T extends BasicCacheModel> Optional<T> fetchCache(int page, int maxUsers, @NonNull Class<T> modelClass) {
        try {
            T model = Objects.requireNonNull(getDocumentSnapshotApiFuture(modelClass))
                    .get(10, TimeUnit.SECONDS)
                    .toObject(modelClass);

            assert model != null;
            model.setMaps(getFilteredMaps(model,
                    integer -> integer >= (page - 1) * maxUsers && integer < page * maxUsers));

            return Optional.of(model);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @NotNull
    private static <T extends BasicCacheModel> List<Map<String, String>> getFilteredMaps(T model, Predicate<Integer> pagePredicate) {
        return model
                .getMaps()
                .stream()
                .sorted(Comparator
                        .comparingInt(o -> Integer.parseInt(o
                                .get("ordinal")))
                ).filter(map -> pagePredicate.test(Integer.parseInt(map.get("ordinal"))))
                .toList();
    }

    public <T extends BasicCacheModel> T deleteCacheUntilNow(Class<T> clazz) {
        try {
            T deletedModel = Objects.requireNonNull(getGenericCollectionReference(clazz))
                    .whereLessThan("timestamp", (System.currentTimeMillis() + 1000 * 60 * 5))
                    .get()
                    .get(5, TimeUnit.SECONDS)
                    .getDocuments()
                    .get(0)
                    .toObject(clazz);
            System.out.println("Deleted cache");
            return deletedModel;
        } catch (Exception e) {
            System.err.println("Error deleting cache");
            return null;
        }
    }

    private <T extends BasicCacheModel> CollectionReference getGenericCollectionReference(Class<T> clazz) {
        if (clazz.isAssignableFrom(FetchedRoleModel.class)) {
            return COLLECTION_CACHE_FETCH;
        } else if (clazz.isAssignableFrom(ToBobifyMembersModel.class)) {
            return COLLECTION_CACHE_BOB;
        } else if (clazz.isAssignableFrom(CurseModel.class)) {
           return COLLECTION_CACHE_CURSE;
        }
        return null;
    }

    public void autoDeleteCache(JDA jda, MessageUtils messageUtils) {
        deleteFetchCache(jda, messageUtils);
        deleteBobCache(jda, messageUtils);
        deleteCurseCache(jda, messageUtils);
    }

    private void deleteBobCache(JDA jda, MessageUtils messageUtils) {
        try {
            DocumentSnapshot documentBob = COLLECTION_CACHE_BOB
                    .whereLessThan("timestamp", (System.currentTimeMillis()))
                    .get()
                    .get()
                    .getDocuments()
                    .get(0);
            ToBobifyMembersModel modelBob = documentBob.toObject(ToBobifyMembersModel.class);
            messageUtils.deleteMessage(jda, modelBob);
            documentBob.getReference().delete();
            System.out.println("Deleted bob cache");
        } catch (Exception e) {

        }
    }

    private void deleteCurseCache(JDA jda, MessageUtils messageUtils) {
        try {
            DocumentSnapshot documentCurse = COLLECTION_CACHE_CURSE
                    .whereLessThan("timestamp", (System.currentTimeMillis()))
                    .get()
                    .get()
                    .getDocuments()
                    .get(0);
            CurseModel model = documentCurse.toObject(CurseModel.class);
            messageUtils.deleteMessage(jda, model);
            documentCurse.getReference().delete();
            System.out.println("Deleted curse cache");
        } catch (Exception e) {

        }
    }

    private void deleteFetchCache(JDA jda, MessageUtils messageUtils) {
        try {
            DocumentSnapshot documentFetch = COLLECTION_CACHE_FETCH
                    .whereLessThan("timestamp", (System.currentTimeMillis()))
                    .get()
                    .get()
                    .getDocuments()
                    .get(0);
            FetchedRoleModel model = documentFetch.toObject(FetchedRoleModel.class);
            messageUtils.deleteMessage(jda, model);
            documentFetch.getReference().delete();
            System.out.println("Deleted fetch cache");
        } catch (Exception e) {

        }
    }
}

package pl.xnik3e.Guardian;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;

@SpringBootApplication
@EnableScheduling
public class GuardianApplication {

	@Bean
	FirebaseApp firebaseApp() throws IOException{
		GoogleCredentials googleCredentials = GoogleCredentials.fromStream(
				new ClassPathResource("service_account.json").getInputStream());
		FirebaseOptions firebaseOptions = FirebaseOptions.builder()
				.setCredentials(googleCredentials)
				.build();
		return FirebaseApp.initializeApp(firebaseOptions, "GuardianBot");
	}

	@Autowired
	@Bean
	Firestore firestore(FirebaseApp app){
		return FirestoreClient.getFirestore(app);
	}


	public static void main(String[] args) {
		SpringApplication.run(GuardianApplication.class, args);
	}

}

package com.example.comdirect;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class Main extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() throws Exception {
        // Starte Spring Context
        springContext = new SpringApplicationBuilder(ComdirectApplication.class).run();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Lade FXML über SpringFXMLLoader
        SpringFXMLLoader fxmlLoader = springContext.getBean(SpringFXMLLoader.class);
        Parent root = fxmlLoader.load("/views/main.fxml").load();

        primaryStage.setTitle("Comdirect Dashboard");
        primaryStage.setScene(new Scene(root));
        primaryStage.setMaximized(true);

        // Beenden der Anwendung bei Fenster schließen
        primaryStage.setOnCloseRequest(event -> {
            try {
                stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        // Schließe den Spring Context, falls er initialisiert wurde
        if (springContext != null) {
            springContext.close();
        }
        // Beende die JavaFX-Plattform
        javafx.application.Platform.exit();

        // Optional: Erzwinge das Beenden der JVM
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

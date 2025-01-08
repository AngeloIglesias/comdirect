package com.example.comdirect.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Controller;

import java.io.*;
import java.net.URL;

@Controller // Spring Controller für FXML
public class MainController {

    @FXML
    private WebView webView; // Verknüpft mit der FXML-Datei

    private WebEngine webEngine;

    /**
     * Initialisierungsmethode, die automatisch nach dem Laden der FXML aufgerufen wird.
     */
    @FXML
    public void initialize() {
        // WebEngine initialisieren und Login-URL laden
        webEngine = webView.getEngine();
        String loginUrl = "https://kunde.comdirect.de/itx/tfe/starten?execution=e7s1";
        webEngine.load(loginUrl);

        // Listener für JNLP-Dateien hinzufügen
        webEngine.locationProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.endsWith(".jnlp")) {
                System.out.println("JNLP file detected: " + newValue);
                handleJNLPDownload(newValue);
            }
        });
    }

    /**
     * Verarbeitet das Herunterladen und Starten der JNLP-Datei.
     *
     * @param jnlpUrl URL der JNLP-Datei
     */
    private void handleJNLPDownload(String jnlpUrl) {
        try {
            // JNLP-Datei herunterladen
            File jnlpFile = downloadJNLP(jnlpUrl);

            // Authentifizierungstoken aus der JNLP-Datei extrahieren
            String authToken = parseJNLPFile(jnlpFile);

            // Anwendung mit dem Token starten
            startApplication(authToken);

            // Erfolgsnachricht anzeigen
            showInfo("Success", "JNLP processed successfully", "The application has been started.");

        } catch (Exception e) {
            // Fehler anzeigen
            showError("Error", "Failed to handle JNLP file.", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Lädt die JNLP-Datei herunter.
     *
     * @param jnlpUrl URL der JNLP-Datei
     * @return Lokale Datei der heruntergeladenen JNLP-Datei
     * @throws IOException Falls ein Fehler beim Download auftritt
     */
    private File downloadJNLP(String jnlpUrl) throws IOException {
        URL url = new URL(jnlpUrl);
        File jnlpFile = new File("session.jnlp");
        try (InputStream in = url.openStream(); OutputStream out = new FileOutputStream(jnlpFile)) {
            in.transferTo(out);
        }
        return jnlpFile;
    }

    /**
     * Parst die JNLP-Datei und extrahiert das Authentifizierungstoken.
     *
     * @param jnlpFile Lokale Datei der JNLP-Datei
     * @return Authentifizierungstoken
     * @throws IOException Falls ein Fehler beim Parsen auftritt
     */
    private String parseJNLPFile(File jnlpFile) throws IOException {
        Document doc = Jsoup.parse(jnlpFile, "UTF-8");
        for (Element argument : doc.select("application-desc > argument")) {
            if (argument.text().startsWith("authentifizierung=")) {
                return argument.text().split("=")[1];
            }
        }
        throw new IllegalStateException("No authentifizierung token found in JNLP file");
    }

    /**
     * Startet die Java-Anwendung mit dem extrahierten Token.
     *
     * @param authToken Authentifizierungstoken
     * @throws IOException Falls ein Fehler beim Starten auftritt
     */
    private void startApplication(String authToken) throws IOException {
        String javaCommand = String.format(
                "java -Xmx1024m -cp tbmxclient.jar:tbmxclient-oss.jar:tbmxclient-skin-comdirect.jar de.xtpro.xtpclient.XTPMain tbmx.client.authentifizierung=%s",
                authToken
        );
        Runtime.getRuntime().exec(javaCommand);
    }

    /**
     * Zeigt eine Erfolgsnachricht in einem Dialog an.
     *
     * @param title   Titel der Nachricht
     * @param header  Kopfzeile der Nachricht
     * @param content Inhalt der Nachricht
     */
    private void showInfo(String title, String header, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Zeigt eine Fehlermeldung in einem Dialog an.
     *
     * @param title   Titel der Fehlermeldung
     * @param header  Kopfzeile der Fehlermeldung
     * @param content Inhalt der Fehlermeldung
     */
    private void showError(String title, String header, String content) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Beispielaktion für den Button-Klick.
     */
    @FXML
    protected void onStartApplicationClick() {
        showInfo("Information", "Starting application!", "The application is being started.");
    }
}

package comdirect.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.web.WebView;
import jodd.http.HttpRequest;
import jodd.http.HttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Controller;
import util.PseudoBrowser;

import java.io.*;

import java.net.URL;

@Controller // Spring Controller für FXML
public class MainController {

    @FXML
    private WebView webView;

    /**
     * Initialisiert die WebView mit den gescrapten Inhalten.
     */
    @FXML
    public void initialize() {
        // URL der Seite
        String url = "https://kunde.comdirect.de/lp/wt/login?execution=e1s1";

        // Browser-ähnliches Verhalten simulieren
        try {
            // PseudoBrowser nutzen, um die Seite zu scrapen
            PseudoBrowser pseudoBrowser = new PseudoBrowser("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
            Document document = pseudoBrowser.grabWebsiteWithCookies(url);

            // Überprüfen, ob der Inhalt erfolgreich geladen wurde
            if (document != null) {
                String htmlContent = document.html();

                // HTML-Inhalt in die WebView laden
                webView.getEngine().loadContent(htmlContent, "text/html");
            } else {
                System.out.println("Fehler: Kein Inhalt vom PseudoBrowser erhalten.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Fehler beim Laden der Seite: " + e.getMessage());
        }
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

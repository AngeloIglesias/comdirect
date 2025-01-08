package comdirect.controllers;

import comdirect.config.ComdirectConfig;
import comdirect.services.BrowseService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.scene.web.WebView;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
public class MainController {

    @Autowired
    private ComdirectConfig config;

    @FXML
    private WebView webView;

    @Autowired
    private BrowseService browseService;

    private String user;
    private String userPin;

    @FXML
    public void initialize() {
        try {
            // Login-Seite laden
            String loginPageHtml = browseService.loadLoginPage();
            displayHtmlInWebView(loginPageHtml);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Fehler", "Fehler beim Initialisieren", e.getMessage());
        }
    }

    /**
     * Beispielaktion für den Button-Klick.
     */
    @FXML
    protected void onStartApplicationClick() {
//        showInfo("Information", "Anmeldung wird durchgeführt!", "Bitte warten...");

        // Login durchführen
        performLoginAndDisplayResponse();

        // Navigation zur Transaktionsseite
        navigateToTransactionPage();
    }

    /**
     * Führt die Anmeldung aus und zeigt die Antwortseite in der WebView an.
     */
    private void performLoginAndDisplayResponse() {
        try {
            requestCredentialsFromUser();
            String actionUrl;

//            // Ziel-URL des "Anmelden"-Buttons extrahieren
//            String loginPageHtml = browseService.loadLoginPage();
//            Document document = Jsoup.parse(loginPageHtml);
//            Element loginForm = document.selectFirst("form");
//            if (loginForm == null) {
//                throw new IllegalStateException("Login-Formular nicht gefunden.");
//            }
//            String actionUrl = loginForm.attr("action");
//            if (!actionUrl.startsWith("http")) {
//                actionUrl = "https://kunde.comdirect.de" + actionUrl; // Absolute URL bilden
//            }

            // Login-Anfrage senden
            actionUrl = config.getPostUrl(); // ToDo: Extract from HTML
            String responseHtml = browseService.performLogin(actionUrl, user, userPin);

            // Antwortseite in der WebView anzeigen
            displayHtmlInWebView(responseHtml);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Fehler", "Login fehlgeschlagen", e.getMessage());
        }
    }

    /**
     * Navigiert zur Transaktionsseite.
     */
    private void navigateToTransactionPage() {
        try {
            String transactionPageHtml = browseService.navigateToPage("https://kunde.comdirect.de/transaction-release");
            displayHtmlInWebView(transactionPageHtml);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Fehler", "Navigation zur Transaktionsseite fehlgeschlagen", e.getMessage());
        }
    }

    /**
     * Fordert den Benutzer zur Eingabe der PIN auf.
     */
    private void requestCredentialsFromUser() {

        if( config.getUser() == null || config.getUser().isEmpty() ) {
            TextInputDialog userDialog = new TextInputDialog();
            userDialog.setTitle("Zugangsnummer erforderlich");
            userDialog.setHeaderText("Bitte geben Sie Ihre Zugangsnummer ein:");
            userDialog.setContentText("Zugangsnummer:");

            // Eingabe des Benutzers abrufen
            Optional<String> result = userDialog.showAndWait();
            result.ifPresentOrElse(
                    userNumber -> user = userNumber,
                    () -> {
                        // Beende die Anwendung, falls keine Zugangsnummer eingegeben wurde
                        showError("Fehler", "Keine Zugangsnummer eingegeben", "Die Anwendung wird beendet.");
                        System.exit(1);
                    }
            );
        }
        else {
            user = config.getUser();
        }

        if( config.getPin() == null || config.getPin().isEmpty() ) {
            TextInputDialog pinDialog = new TextInputDialog();
            pinDialog.setTitle("PIN erforderlich");
            pinDialog.setHeaderText("Bitte geben Sie Ihre PIN ein:");
            pinDialog.setContentText("PIN:");

            // Eingabe des Benutzers abrufen
            Optional<String> result = pinDialog.showAndWait();
            result.ifPresentOrElse(
                    pin -> userPin = pin,
                    () -> {
                        // Beende die Anwendung, falls keine PIN eingegeben wurde
                        showError("Fehler", "Keine PIN eingegeben", "Die Anwendung wird beendet.");
                        System.exit(1);
                    }
            );
        }
        else {
            userPin = config.getPin();
        }
    }

    /**
     * Zeigt HTML-Inhalt in der WebView an.
     *
     * @param htmlContent HTML-Inhalt als String
     */
    private void displayHtmlInWebView(String htmlContent) {
        webView.getEngine().loadContent(htmlContent, "text/html");
    }

    /**
     * Zeigt eine Fehlermeldung in einem Dialog an.
     *
     * @param title   Titel der Fehlermeldung
     * @param header  Kopfzeile der Fehlermeldung
     * @param content Inhalt der Fehlermeldung
     */
    private void showError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Zeigt eine Erfolgsnachricht in einem Dialog an.
     *
     * @param title   Titel der Nachricht
     * @param header  Kopfzeile der Nachricht
     * @param content Inhalt der Nachricht
     */
    private void showInfo(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

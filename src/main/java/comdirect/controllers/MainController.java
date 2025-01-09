package comdirect.controllers;

import com.microsoft.playwright.*;
import comdirect.config.ComdirectConfig;
import comdirect.services.BrowseService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.scene.web.WebView;
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

    private Playwright playwright;
    private Browser browser;

    @FXML
    public void initialize() {
        // Playwright initialisieren
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));

        try {
            // Login-Seite laden
            String loginPageHtml = loadPageWithPlaywright("https://kunde.comdirect.de");
            displayHtmlInWebView(loginPageHtml);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Fehler", "Fehler beim Initialisieren", e.getMessage());
        }
    }

    @FXML
    protected void onStartApplicationClick() {
        try {
            requestCredentialsFromUser();

            // Login ausführen
            String responseHtml = performLogin();
            displayHtmlInWebView(responseHtml);

            // Navigation zur Transaktionsseite
            String transactionHtml = loadPageWithPlaywright("https://kunde.comdirect.de/transaction-release");
            displayHtmlInWebView(transactionHtml);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Fehler", "Aktion fehlgeschlagen", e.getMessage());
        }
    }

    private String performLogin() {
        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();
            page.navigate("https://kunde.comdirect.de");

            // Eingaben in das Login-Formular
            page.fill("#username", user); // Beispiel-Selektor, anpassen!
            page.fill("#password", userPin);
            page.click("button[type='submit']"); // Beispiel-Button, anpassen!

            // Warte, bis die Seite geladen ist
            page.waitForLoadState();

            return page.content();
        }
    }

    private String loadPageWithPlaywright(String url) {
        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();
            page.navigate(url);
            page.waitForLoadState();
            return page.content();
        }
    }

    private void requestCredentialsFromUser() {
        if (config.getUser() == null || config.getUser().isEmpty()) {
            TextInputDialog userDialog = new TextInputDialog();
            userDialog.setTitle("Zugangsnummer erforderlich");
            userDialog.setHeaderText("Bitte geben Sie Ihre Zugangsnummer ein:");
            userDialog.setContentText("Zugangsnummer:");
            Optional<String> result = userDialog.showAndWait();
            result.ifPresentOrElse(
                    userNumber -> user = userNumber,
                    () -> {
                        showError("Fehler", "Keine Zugangsnummer eingegeben", "Die Anwendung wird beendet.");
                        System.exit(1);
                    }
            );
        } else {
            user = config.getUser();
        }

        if (config.getPin() == null || config.getPin().isEmpty()) {
            TextInputDialog pinDialog = new TextInputDialog();
            pinDialog.setTitle("PIN erforderlich");
            pinDialog.setHeaderText("Bitte geben Sie Ihre PIN ein:");
            pinDialog.setContentText("PIN:");
            Optional<String> result = pinDialog.showAndWait();
            result.ifPresentOrElse(
                    pin -> userPin = pin,
                    () -> {
                        showError("Fehler", "Keine PIN eingegeben", "Die Anwendung wird beendet.");
                        System.exit(1);
                    }
            );
        } else {
            userPin = config.getPin();
        }
    }

    private void displayHtmlInWebView(String htmlContent) {
        Platform.runLater(() -> webView.getEngine().loadContent(htmlContent, "text/html"));
    }

    private void showError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

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
            try (BrowserContext context = browser.newContext()) {
                Page page = context.newPage();
                page.navigate("https://kunde.comdirect.de");

                // Warte, bis die Seite vollständig geladen ist
                page.waitForLoadState();

                // Cookie-Banner schließen (falls sichtbar)
                if (page.locator("button:has-text('Alle akzeptieren')").isVisible()) {
                    page.click("button:has-text('Alle akzeptieren')");
                    System.out.println("Cookie-Banner akzeptiert.");
                }

                // HTML der Seite extrahieren und in der WebView anzeigen
                String loginPageHtml = page.content();
                displayHtmlInWebView(loginPageHtml);
            }
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

            // Warte, bis die Seite vollständig geladen ist
            page.waitForLoadState();

            /// /////////////////////////////



            /// /////////////////////////////

            // Formular ausfüllen
            page.fill("input[name='param1']", user); // Benutzername
            page.fill("input[name='param3']", userPin); // PIN / Passwort

            // Optional: CSRF-Token extrahieren (falls erforderlich)
            String csrfToken = page.locator("input[name='csfCsrfToken']").getAttribute("value");
            if (csrfToken != null) {
                System.out.println("CSRF-Token gefunden: " + csrfToken);
            }

            // Button klicken, um das Formular abzusenden
            page.click("a#loginAction"); // Login-Button

            // Warte, bis die Seite erneut geladen ist oder ein Redirect erfolgt
            page.waitForLoadState();

            // Seite prüfen (z.B. auf Dashboard oder Fehlermeldungen)
            if (page.url().contains("dashboard")) {
                System.out.println("Login erfolgreich!");
            } else if (page.content().contains("Fehler")) {
                System.out.println("Login fehlgeschlagen. Fehlermeldung auf der Seite gefunden.");
            }

            // Rückgabe des HTML-Inhalts
            return page.content();
        } catch (Exception e) {
            e.printStackTrace();
            return "Fehler beim Login: " + e.getMessage();
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

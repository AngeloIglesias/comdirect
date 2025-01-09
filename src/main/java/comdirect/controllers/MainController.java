package comdirect.controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.playwright.*;
import comdirect.config.ComdirectConfig;
import comdirect.services.BrowseService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.Optional;

@Controller
public class MainController {

    @Autowired
    private ComdirectConfig config;

    @FXML
    private WebView webView;

//    @Autowired
//    private BrowseService browseService;

    private String user;
    private String userPin;

    private Playwright playwright;
    private Browser browser;

    private BrowserContext context;
    private Page page;

    @FXML
    public void initialize() {
        // Playwright initialisieren
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));

        try {
            // Browser-Kontext und Seite erstellen
            context = browser.newContext();
            page = context.newPage();

            // Login-Seite laden
            page.navigate("https://kunde.comdirect.de");

            // Warte, bis die Seite vollständig geladen ist
            page.waitForLoadState();

            // Cookie-Banner schließen (falls sichtbar)
            if (page.locator("button:has-text('Alle akzeptieren')").isVisible()) {
                page.click("button:has-text('Alle akzeptieren')");
                System.out.println("Cookie-Banner akzeptiert.");
            }

            /// ///
            // Bridge zwischen JavaFX-WebView und Playwright erstellen
            WebViewBridge bridge = new WebViewBridge(this);
            JSObject window = (JSObject) webView.getEngine().executeScript("window");
            window.setMember("bridge", bridge);
            /// //

            /// ///
            // Zusätzlicher Schutz: Blockiere externe Navigation
//            page.onRequest(request -> {
//                if (!request.url().startsWith("https://kunde.comdirect.de")) {
//                    System.out.println("Blockierte Anfrage: " + request.url());
//                    request.abort();
//                }
//            });
            ///


            // HTML der Seite extrahieren und in der WebView anzeigen
            String loginPageHtml = page.content();
            displayHtmlInWebView(loginPageHtml);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Fehler", "Fehler beim Initialisieren", e.getMessage());
        }
    }

    private String performLogin() {
        try {
            // Warte, bis die Login-Seite vollständig geladen ist
            page.waitForLoadState();

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
        try {
            page.navigate(url);
            page.waitForLoadState();
            return page.content();
        } catch (Exception e) {
            e.printStackTrace();
            return "Fehler beim Laden der Seite: " + e.getMessage();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
        // JavaScript deaktivieren (z.B. für Links und Formulare) und Befehle an JavaFX-WebView weiterleiten
        Platform.runLater(() -> webView.getEngine().loadContent(
                htmlContent + "<script>" +
                        "document.querySelectorAll('a').forEach(link => link.addEventListener('click', e => {" +
                        "    e.preventDefault();" +
                        "    window.bridge.onLinkClicked(link.href);" +
                        "}));" +
                        "document.querySelectorAll('form').forEach(form => form.addEventListener('submit', e => {" +
                        "    e.preventDefault();" +
                        "    let formData = new FormData(form);" +
                        "    let obj = {};" +
                        "    formData.forEach((value, key) => { obj[key] = value; });" +
                        "    window.bridge.onFormSubmitted(JSON.stringify(obj));" +
                        "}));" +
                        "</script>"
        ));
    }

    public void handleLinkClick(String href) {
        try {
            page.navigate(href); // Playwright übernimmt die Navigation
            String updatedHtml = page.content();
            displayHtmlInWebView(updatedHtml);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Fehler", "Link-Navigation fehlgeschlagen", e.getMessage());
        }
    }

    public void handleFormSubmission(String formDataJson) {
        try {
            // Deserialisiere das JSON (formDataJson) und fülle Playwright-Formulare
            Map<String, String> formData = new Gson().fromJson(formDataJson, new TypeToken<Map<String, String>>() {}.getType());
            for (Map.Entry<String, String> entry : formData.entrySet()) {
                page.fill("input[name='" + entry.getKey() + "']", entry.getValue());
            }

            // Formular abschicken (z. B. durch einen Submit-Button-Klick)
            page.click("button[type='submit']");
            String updatedHtml = page.content();
            displayHtmlInWebView(updatedHtml);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Fehler", "Formular-Verarbeitung fehlgeschlagen", e.getMessage());
        }
    }


    private void showError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

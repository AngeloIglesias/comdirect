package comdirect.controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.playwright.*;
import comdirect.config.ComdirectConfig;
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
            System.out.println("Bridge erfolgreich registriert: " + (window != null));
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

            webView.getEngine().locationProperty().addListener((obs, oldLocation, newLocation) -> {
                Platform.runLater(() -> {
                    System.out.println( "Location in WebView changed: " + newLocation);
                });
            });



            // HTML der Seite extrahieren und in der WebView anzeigen
            String loginPageHtml = page.content();
            displayHtmlInWebView(loginPageHtml);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Fehler", "Fehler beim Initialisieren", e.getMessage());
        }

        // Run checks:
        webView.getEngine().setJavaScriptEnabled(true); // Required for Playwright communication
        // Teste, ob die Bridge erreichbar ist
        Object testResult = webView.getEngine().executeScript("typeof window.bridge.testConnection === 'function'");
        System.out.println("Bridge testConnection erreichbar: " + testResult);
        System.out.println("JavaScript aktiviert: " + webView.getEngine().isJavaScriptEnabled());
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
        Platform.runLater(() -> webView.getEngine().loadContent(
                htmlContent + "<script>" +
                        addDebugCode() +
                        addConsoleLogCode() +
                        addBridgeCode() +
                        "</script>"
        ));
    }

    private String addDebugCode()
    {
        if(false)
        {
            return "const testElement = document.createElement('div');" +
                    "testElement.innerText = 'JavaScript läuft!';" +
                    "testElement.style.position = 'fixed';" +
                    "testElement.style.top = '10px';" +
                    "testElement.style.left = '10px';" +
                    "testElement.style.zIndex = '10000';" +
                    "testElement.style.backgroundColor = 'red';" +
                    "testElement.style.color = 'white';" +
                    "testElement.style.padding = '10px';" +
                    "testElement.style.fontSize = '20px';" +
                    "document.body.appendChild(testElement);";
        }
        else {
            return "";
        }
    }

    // Gibt console.log-Ausgaben in der WebView als JavaScript-Alerts aus
    private String addConsoleLogCode() {
        return "console.log = function(...messages) {" +
                "    let logDiv = document.getElementById('logDiv');" +
                "    if (!logDiv) {" +
                "        logDiv = document.createElement('div');" +
                "        logDiv.id = 'logDiv';" +
                "        logDiv.style.position = 'fixed';" +
                "        logDiv.style.top = '0';" +
                "        logDiv.style.left = '0';" +
                "        logDiv.style.width = '20%';" + // Nur 30% des Bildschirms breit
                "        logDiv.style.height = '100%';" + // Gesamte Höhe des Bildschirms
                "        logDiv.style.overflowY = 'auto';" + // Scrollbar für lange Logs
                "        logDiv.style.backgroundColor = 'red';" +
                "        logDiv.style.color = 'white';" +
                "        logDiv.style.padding = '10px';" +
                "        logDiv.style.fontSize = '14px';" +
                "        logDiv.style.zIndex = '9999';" +
                "        logDiv.style.whiteSpace = 'pre-wrap';" + // Ermöglicht Zeilenumbrüche
                "        document.body.appendChild(logDiv);" +
                "    }" +
                "    const logMessage = messages.map(m => typeof m === 'object' ? JSON.stringify(m, null, 2) : m).join(' ');" +
                "    const logEntry = document.createElement('div');" +
                "    logEntry.innerText = logMessage;" +
                "    logEntry.style.marginBottom = '5px';" + // Abstand zwischen Logs
                "    logDiv.appendChild(logEntry);" +
                "    logDiv.scrollTop = logDiv.scrollHeight;" + // Automatisch nach unten scrollen
                "};";
    }



    private String addBridgeCode()
    {
        return "window.bridge = {" +
                "onLinkClicked: function(href) {" +
                "window.location.href = 'bridge://onLinkClicked?href=' + encodeURIComponent(href);" +
                "}," +
                "onFormSubmitted: function(formData) {" +
                "window.location.href = 'bridge://onFormSubmitted?formData=' + encodeURIComponent(formData);" +
                "}" +
                "};";
    }

    public void handleLinkClick(String href) {
        System.out.println("Link geklickt: " + href);
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
        System.out.println("Formular abgeschickt: " + formDataJson);
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

    public static class WebViewBridge {
        private MainController controller;

        public WebViewBridge(MainController controller) {
            this.controller = controller;
        }

        public void onLinkClicked(String href) {
            System.out.println("Benutzer hat Link geklickt: " + href);
            controller.handleLinkClick(href);
        }

        public void onFormSubmitted(String formData) {
            System.out.println("Formular wurde abgeschickt: " + formData);
            controller.handleFormSubmission(formData);
        }

        public void testConnection() {
            System.out.println("Bridge connected!");
        }

        public void logMessage(String message) {
            System.out.println("WebView Log: " + message);
        }

    }
}

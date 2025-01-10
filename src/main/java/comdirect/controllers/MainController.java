package comdirect.controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.playwright.*;
import comdirect.config.ComdirectConfig;
import comdirect.services.BrowseService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class MainController {

    @Autowired
    private ComdirectConfig config;

    @FXML
    private WebView webView;

    @Autowired
    private BrowseService browseService;

    @FXML
    public void initialize() {
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////
        /// WebView-Initialisierung
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////

        try {
            // HTML der Seite extrahieren und in der WebView anzeigen
            displayHtmlInWebView(browseService.getLoginPage());

            webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    // Seite wurde vollständig geladen
                    System.out.println("Seite vollständig geladen, registriere Bridge und JavaScript.");

                    // Registriere die Bridge nur, wenn sie nicht bereits registriert ist
                    JSObject window = (JSObject) webView.getEngine().executeScript("window");
                    BrowserUtils.WebViewBridge bridge = new BrowserUtils.WebViewBridge(this);
                    window.setMember("bridge", bridge);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            BrowserUtils.showError("Fehler", "Fehler beim Initialisieren", e.getMessage());
        }

        // Run checks:
        webView.getEngine().setJavaScriptEnabled(true); // JavaScript aktivieren
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// WebView-Interaktionen
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void displayHtmlInWebView(String htmlContent) {
        Platform.runLater(() -> webView.getEngine().loadContent(
                htmlContent + "<script>" +
                        BrowserUtils.addDebugCode() +
                        BrowserUtils.addConsoleLogCode() +
                        BrowserUtils.addBridgeCode() +
                        "</script>"
        ));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Event-Handler für WebView-Interaktionen
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @FXML
    protected void onStartApplicationClick() {
        try {
            BrowserUtils.requestCredentialsFromUser(config);

            // Login ausführen
            String responseHtml = browseService.performLogin(config.getUser(), config.getPin());
            displayHtmlInWebView(responseHtml);

            // ToDo: What ist the sense of this code?
//            // Navigation zur Transaktionsseite
//            String transactionHtml = loadPageWithPlaywright("https://kunde.comdirect.de/transaction-release");
//            displayHtmlInWebView(transactionHtml);
        } catch (Exception e) {
            e.printStackTrace();
            BrowserUtils.showError("Fehler", "Aktion fehlgeschlagen", e.getMessage());
        }
    }


    public void handleLinkClick(String href) {
        System.out.println("Link geklickt: " + href);
        try {
            displayHtmlInWebView(browseService.loadPage(href));
        } catch (Exception e) {
            e.printStackTrace();
            BrowserUtils.showError("Fehler", "Link-Navigation fehlgeschlagen", e.getMessage());
        }
    }

    public void handleFormSubmission(String formDataJson) {
        System.out.println("Formular abgeschickt: " + formDataJson);
        try {
            displayHtmlInWebView(browseService.postForm(formDataJson));
        } catch (Exception e) {
            e.printStackTrace();
            BrowserUtils.showError("Fehler", "Formular-Verarbeitung fehlgeschlagen", e.getMessage());
        }
    }


}

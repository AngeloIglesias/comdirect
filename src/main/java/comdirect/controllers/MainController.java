package comdirect.controllers;

import comdirect.config.ComdirectConfig;
import comdirect.services.BrowseService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Controller
public class MainController {

    @Value("${comdirect.ui.enable-javascript-console}")
    boolean enableJavaScriptConsole;

    @Value("${comdirect.ui.enable-javascript-debug}")
    boolean enableJavaScriptDebug;

    @Autowired
    private ComdirectConfig config;

    @FXML
    private WebView webView;

    @Autowired
    private BrowseService browseService;
    private boolean isLoading;

    @FXML
    public void initialize() {
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////
        /// WebView-Initialisierung
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////

        webView.getEngine().setJavaScriptEnabled(true); // Make sure JavaScript is enabled!
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
        webView.getEngine().locationProperty().addListener((obs, oldLocation, newLocation) -> {
            if (newLocation.startsWith("bridge://")) {
                handleBridgeRequest(newLocation);
            }
        });

        displayHtmlInWebView(browseService.getLoginPage());
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// WebView-Interaktionen
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private synchronized void displayHtmlInWebView(String htmlContent) {
        if (isLoading) return;
        isLoading = true;
        Platform.runLater(() -> {
            webView.getEngine().loadContent(appendScripts(htmlContent));
            isLoading = false;
        });
    }

    private String appendScripts (String htmlContent) {
        return htmlContent + "<script>" +
            (enableJavaScriptDebug ? BrowserUtils.addDebugCode() : "") +
            (enableJavaScriptConsole ? BrowserUtils.addConsoleLogCode() : "") +
            BrowserUtils.addBridgeCode() +
            "</script>";
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Event handlers for JavaFX controls
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @FXML
    protected void onStartApplicationClick() {
        try {
            BrowserUtils.requestCredentialsFromUser(config);

            // Login ausführen
            String responseHtml = browseService.performLogin(config.getLogin().getUser(), config.getLogin().getPin());
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

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Event handlers for WebView interactions
    /// These methods are called from JavaScript code in the WebView (see BrowserUtils.addBridgeCode())
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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

    private void handleBridgeRequest(String url) {
        if (url.startsWith("bridge://onLinkClicked")) {
            String href = extractQueryParam(url, "href");
            handleLinkClick(href);
        } else if (url.startsWith("bridge://onFormSubmitted")) {
            String formData = extractQueryParam(url, "formData");
            handleFormSubmission(formData);
        } else {
            System.out.println("Unbekannter Bridge-Event: " + url);
        }
    }

    private String extractQueryParam(String url, String param) {
        try {
            String query = url.split("\\?")[1];
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue[0].equals(param)) {
                    return URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.name());
                }
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Extrahieren des Parameters: " + e.getMessage());
        }
        return null;
    }
}

package comdirect.controllers;

import comdirect.config.ComdirectConfig;
import comdirect.services.BrowseService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
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

    @FXML
    private TextField addressBar;

    @FXML
    private ComboBox<String> browserSelector;

    @Autowired
    private BrowseService browseService;
    private boolean isLoading;

    @FXML
    public void initialize() {
        // Browser-Dropdown initialisieren
        browserSelector.getItems().addAll("chromium", "firefox", "webkit", "edge");
        browserSelector.setValue(config.getBrowser().getDefaultBrowser()); // Standardwert aus application.yml


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

        displayHtmlInWebView(browseService.navigateToAndCloseCookieBanner(config.getLogin().getUrl3()));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// WebView-Interaktionen
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private synchronized void displayHtmlInWebView(String htmlContent) {
        if (isLoading) return;
        isLoading = true;
        Platform.runLater(() -> {
            try {
                webView.getEngine().loadContent(appendScripts(htmlContent));
                addressBar.setText(browseService.page.url());
            } finally {
                // isLoading zurücksetzen, auch wenn ein Fehler auftritt
                isLoading = false;
            }
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
    /// Event handlers for WebView interactions
    /// These methods are called from JavaScript code in the WebView (see BrowserUtils.addBridgeCode())
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void handleLinkClick(String href) {
        System.out.println("Link geklickt: " + href);

        try {
            String absoluteUrl = resolveUrl(href);
            System.out.println("Absolute URL: " + absoluteUrl);

            if (!isValidUrl(absoluteUrl)) {
                BrowserUtils.showError("Fehler", "Ungültige URL", "Die URL \"" + absoluteUrl + "\" ist ungültig.");
                return;
            }

            displayHtmlInWebView(browseService.navigateTo(absoluteUrl));
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

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Helper methods
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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

    private String resolveUrl(String href) {
        try {
            if (href.startsWith("//")) {
                // Protokoll-relative URL ergänzen
                return "https:" + href;
            }

            if (href.startsWith("#")) {
                // Interner Anker, füge zur aktuellen URL hinzu
                return browseService.page.url() + href;
            }

            if (href.startsWith("http://") || href.startsWith("https://")) {
                // Absolute URL
                return href;
            }

            // Relative URL in absolute URL umwandeln
            String baseUrl = browseService.page.url();
            return new java.net.URL(new java.net.URL(baseUrl), href).toString();
        } catch (Exception e) {
            System.err.println("Fehler beim Erstellen der absoluten URL: " + e.getMessage());
            return href; // Fallback auf den Original-Link
        }
    }
    private boolean isValidUrl(String url) {
        try {
            new java.net.URI(url);
            return true;
        } catch (Exception e) {
            System.err.println("Ungültige URL: " + url);
            return false;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Event handlers for JavaFX controls
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @FXML
    protected void onBackClick() {
        displayHtmlInWebView(browseService.navigateBack());
    }

    @FXML
    protected void onHomeClick() {
        displayHtmlInWebView(browseService.navigateTo(config.getUi().getHomeUrl()));
    }

    @FXML
    protected void onAddressEntered() {
        String url = addressBar.getText();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        displayHtmlInWebView(browseService.navigateTo(url));
    }

    @FXML
    protected void onStartApplicationClick() {
        // ToDo: Implement the auto login, download and webstart functionality, here
        BrowserUtils.showError("Fehler", "Aktion fehlgeschlagen", "Not implemented yet.");
    }

    @FXML
    public void onForwardClick(ActionEvent actionEvent) {
        displayHtmlInWebView(browseService.navigateForward());
    }

    @FXML
    public void onBrowserSelectionChanged(ActionEvent actionEvent) {
        String selectedBrowser = browserSelector.getValue();
        browseService.changeBrowser(selectedBrowser);
    }

    @FXML
    public void onRefreshClick(ActionEvent actionEvent) {
        displayHtmlInWebView(browseService.refreshPage());
    }

    @FXML
    public void onLoginClick(ActionEvent actionEvent) {
        try {
            BrowserUtils.requestCredentialsFromUser(config);

            // Login ausführen
            String responseHtml = browseService.performLogin(config.getLogin().getUser(), config.getLogin().getPin());
            displayHtmlInWebView(responseHtml);

        } catch (Exception e) {
            e.printStackTrace();
            BrowserUtils.showError("Fehler", "Aktion fehlgeschlagen", e.getMessage());
        }
    }
}

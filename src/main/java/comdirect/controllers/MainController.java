package comdirect.controllers;

import comdirect.config.ComdirectConfig;
import comdirect.services.BookmarkManager;
import comdirect.services.BrowseService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import util.BrowserUtils;

import static util.BrowserUtils.*;

@Controller
public class MainController {

    @Value("${comdirect.ui.enable-javascript-console}")
    boolean enableJavaScriptConsole;

    @Value("${comdirect.ui.enable-javascript-debug}")
    boolean enableJavaScriptDebug;

    @Autowired
    private ComdirectConfig config;

    @FXML
    WebView webView;

    @FXML
    private TextField addressBar;

    @FXML
    private ComboBox<String> browserSelector;

    @FXML
    private ComboBox<String> bookmarkSelector;

    @Autowired
    private BrowseService browseService;

    @Autowired
    private BookmarkManager bookmarkManager;

    private boolean isLoading;

    @FXML
    public void initialize() {
        // Browser-Dropdown initialisieren
        browserSelector.getItems().addAll("chromium", "firefox", "webkit", "edge");
        browserSelector.setValue(config.getBrowser().getDefaultBrowser()); // Standardwert aus application.yml

        // Bookmarks in die ComboBox laden
        bookmarkSelector.getItems().addAll(bookmarkManager.getBookmarkNames());

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
                window.setMember("bridge", this);
            }
        });
        webView.getEngine().locationProperty().addListener((obs, oldLocation, newLocation) -> {
            if (newLocation.startsWith("bridge://")) {
                handleBridgeRequest(newLocation);
            }
        });

        if(config.getUi().isLoadHomePageAtStartup()) {
            if (config.getUi().isAutoCloseCookieBannerAtStartup()) {
                // Cookie-Banner schließen
                displayHtmlInWebView(browseService.navigateToAndCloseCookieBanner(config.getUi().getUrlHome()));
            } else {
                // Standardseite anzeigen
                displayHtmlInWebView(browseService.navigateTo(config.getUi().getUrlHome()));
            }
        }
        if(config.getLogin().isAutoLogin()) {
            onLoginClick(null);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// WebView-Interaktionen
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void handleBridgeRequest(String url) {
        if (url.startsWith("bridge://onLinkClicked")) {
            String href = extractQueryParam(url, "href");
            handleLinkClicked(href);
        } else if (url.startsWith("bridge://onFormSubmitted")) {
            String formData = extractQueryParam(url, "formData");
            handleFormSubmitted(formData);
        } else {
            System.out.println("Unbekannter Bridge-Event: " + url);
        }
    }

    void handleFormSubmitted(String formDataJson) {
        System.out.println("Formular abgeschickt: " + formDataJson);
        try {
            displayHtmlInWebView(browseService.postForm(formDataJson));
        } catch (Exception e) {
            e.printStackTrace();
            BrowserUtils.showError("Fehler", "Formular-Verarbeitung fehlgeschlagen", e.getMessage());
        }
    }

    void handleLinkClicked(String href) {
        System.out.println("Link geklickt: " + href);

        try {
            String absoluteUrl = resolveUrl(href, browseService.page);
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

    synchronized void displayHtmlInWebView(String htmlContent) {
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
    /// Event handlers for JavaFX controls
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @FXML
    protected void onHomeClick() {
        displayHtmlInWebView(browseService.navigateTo(config.getUi().getUrlHome()));
        bookmarkSelector.setValue(bookmarkManager.getPageName());
    }

    @FXML
    protected void onBackClick() {
        displayHtmlInWebView(browseService.navigateBack());
        bookmarkSelector.setValue(bookmarkManager.getPageName());
    }

    @FXML
    public void onForwardClick(ActionEvent actionEvent) {
        bookmarkSelector.setValue(null);
        displayHtmlInWebView(browseService.navigateForward());
        bookmarkSelector.setValue(bookmarkManager.getPageName());
    }

    @FXML
    protected void onAddressEntered() {
        String url = addressBar.getText();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        displayHtmlInWebView(browseService.navigateTo(url));
        bookmarkSelector.setValue(bookmarkManager.getPageName());
    }

    @FXML
    protected void onStartApplicationClick() {
        // ToDo: Implement the auto login, download and webstart functionality, here
        BrowserUtils.showError("Fehler", "Aktion fehlgeschlagen", "Not implemented yet.");
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
        bookmarkSelector.setValue(null);
        try {
            if(config.getLogin().isUseDifferentLoginUrl())
            {
                if(config.getLogin().isAutoCloseCookieBanner()) {
                    // Cookie-Banner schließen und Login-Seite anzeigen
                    displayHtmlInWebView(browseService.navigateToAndCloseCookieBanner(config.getLogin().getUrl()));
                } else {
                    // Login-Seite anzeigen
                    displayHtmlInWebView(browseService.navigateTo(config.getLogin().getUrl()));
                }
            }

            if( BrowserUtils.requestCredentialsFromUser(config))
            {
                // Login ausführen
                String responseHtml = browseService.performLogin(config.getLogin().getUser(), config.getLogin().getPin());
                displayHtmlInWebView(responseHtml);
            }
        } catch (Exception e) {
            e.printStackTrace();
            BrowserUtils.showError("Fehler", "Aktion fehlgeschlagen", e.getMessage());
        }
    }

    @FXML
    public void onBookmarkSelectionChanged(ActionEvent actionEvent) {
        // Name des ausgewählten Bookmarks abrufen
        String selectedBookmarkName = bookmarkSelector.getValue();

        // URL zum Bookmark abrufen
        String url = bookmarkManager.getBookmarkUrlByName(selectedBookmarkName);

        if (url != null) {
            // URL im Browser öffnen
            displayHtmlInWebView(browseService.navigateTo(url));
        } else {
            System.err.println("Fehler: Keine URL für das ausgewählte Bookmark gefunden.");
        }
    }
}

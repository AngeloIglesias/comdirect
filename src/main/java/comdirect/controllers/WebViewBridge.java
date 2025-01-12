package comdirect.controllers;

import com.microsoft.playwright.Page;
import comdirect.services.BrowseService;
import javafx.application.Platform;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Component
public class WebViewBridge {
    private final MainController controller;
    private BrowseService browseService;

    public WebViewBridge(MainController controller, BrowseService browseService) {
        this.controller = controller;
        this.browseService = browseService;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Event handlers for WebView interactions
    /// These methods are called from JavaScript code in the WebView (see BrowserUtils.addBridgeCode())
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void onLinkClicked(String href) {
        controller.handleLinkClicked(href);
    }

    public void onFormSubmitted(String formDataJson) {
        controller.handleFormSubmitted(formDataJson);
    }

    /**
     * Methode wird von JavaScript aufgerufen, wenn sich ein Texteingabefeld ändert.
     */
    public void onTextInputChange(String inputNameOrId, String newValue) {
        System.out.println("Textinput geändert: " + inputNameOrId + " = " + newValue);
        // Synchronisiere Playwright mit der neuen Eingabe
        Platform.runLater(() -> {
            if (inputNameOrId == null || inputNameOrId.isEmpty()) {
                System.err.println("Ungültiger Input-Name oder ID.");
                return;
            }

            try {
                // Playwright: Fülle das entsprechende Eingabefeld
                browseService.page.fill("input[name='" + inputNameOrId + "'], input[id='" + inputNameOrId + "']", newValue);
                System.out.println("Playwright aktualisiert: " + inputNameOrId + " = " + newValue);
            } catch (Exception e) {
                System.err.println("Fehler beim Synchronisieren mit Playwright: " + e.getMessage());
            }
        });
    }

    public void onDropdownChange(String dropdownNameOrId, String selectedValue) {
        Platform.runLater(() -> {
            // Synchronisiere Playwright mit der neuen Auswahl
            browseService.page.selectOption("select[name='" + dropdownNameOrId + "'], select[id='" + dropdownNameOrId + "']", selectedValue);
        });
    }


    public void testConnection() {
        System.out.println("Bridge connected!");
    }

    public void logMessage(String message) {
        System.out.println("WebView Log: " + message);
    }
}
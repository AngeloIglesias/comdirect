package comdirect.controllers;

import com.microsoft.playwright.Page;
import comdirect.config.ComdirectConfig;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;

import java.util.Optional;

/**
 * Todo: Sollten nach DDD in dedizierte Service-Klassen, z. B. JavaScriptService oder WebViewService verschoben werden.
 */
public class BrowserUtils {
    static String addDebugCode()
    {
        return "";
    }

    // Gibt console.log-Ausgaben in der WebView als JavaScript-Alerts aus
    static String addConsoleLogCode() {
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

    static String addBridgeCode() {
        return "window.bridge = {" +
                "    onLinkClicked: function(href) {" +
                "        console.log('[Bridge] Link geklickt:', href);" +
                "        try {" +
                "            window.location.href = 'bridge://onLinkClicked?href=' + encodeURIComponent(href);" +
                "        } catch (error) {" +
                "            console.error('[Bridge] Fehler beim Verarbeiten von onLinkClicked:', error);" +
                "        }" +
                "    }," +
                "    onFormSubmitted: function(formData) {" +
                "        console.log('[Bridge] Formular abgeschickt:', formData);" +
                "        try {" +
                "            window.location.href = 'bridge://onFormSubmitted?formData=' + encodeURIComponent(formData);" +
                "        } catch (error) {" +
                "            console.error('[Bridge] Fehler beim Verarbeiten von onFormSubmitted:', error);" +
                "        }" +
                "    }" +
                "};" +
                // Logs für Events
                "document.querySelectorAll('a').forEach(link => link.addEventListener('click', e => {" +
                "    e.preventDefault();" + // Verhindert Standardnavigation
                "    console.log('[Event] Link geklickt:', link.href);" +
                "    window.bridge.onLinkClicked(link.href);" +
                "}));" +
                "document.querySelectorAll('form').forEach(form => form.addEventListener('submit', e => {" +
                "    e.preventDefault();" + // Verhindert Standardformularabsenden
                "    const formData = new FormData(form);" +
                "    const formObject = {};" +
                "    formData.forEach((value, key) => { formObject[key] = value; });" +
                "    console.log('[Event] Formular abgeschickt:', formObject);" +
                "    window.bridge.onFormSubmitted(JSON.stringify(formObject));" +
                "}));";
    }


    static boolean requestCredentialsFromUser(ComdirectConfig config1) {
        if (config1.getLogin().getUser() == null || config1.getLogin().getUser().isEmpty()) {
            TextInputDialog userDialog = new TextInputDialog();
            userDialog.setTitle("Zugangsnummer erforderlich");
            userDialog.setHeaderText("Bitte geben Sie Ihre Zugangsnummer ein:");
            userDialog.setContentText("Zugangsnummer:");
            Optional<String> result = userDialog.showAndWait();
            if( result.isEmpty())
            {
                return false;
            }
            result.ifPresent(userNumber -> config1.getLogin().setUser(userNumber));
        }

        if (config1.getLogin().getPin() == null || config1.getLogin().getPin().isEmpty()) {
            TextInputDialog pinDialog = new TextInputDialog();
            pinDialog.setTitle("PIN erforderlich");
            pinDialog.setHeaderText("Bitte geben Sie Ihre PIN ein:");
            pinDialog.setContentText("PIN:");
            Optional<String> result = pinDialog.showAndWait();
            if( result.isEmpty())
            {
                return false;
            }
            result.ifPresent(pin -> config1.getLogin().setPin(pin)); // ToDo: Never save PIN/Password readably!
        }
        return true;
    }

    static void showError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void closeCookieBanner(Page page) {
        if (page.locator("button:has-text('Alle akzeptieren')").isVisible()) {
            page.click("button:has-text('Alle akzeptieren')");
            System.out.println("Cookie-Banner akzeptiert.");
        }
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

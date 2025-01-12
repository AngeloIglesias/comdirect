package comdirect.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.playwright.*;
import comdirect.config.ComdirectConfig;
import comdirect.controllers.BrowserUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BrowseService {
    private final ComdirectConfig config;

    private final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36";

    private List<String> history = new ArrayList<>(); // Manuelle History

    private int currentIndex = -1; // Index der aktuellen Seite


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Var (Stateful Bean, ToDo: Externalize state to a separate class)
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    //ToDo: Change Encapsulation
    public Playwright playwright;
    public Browser browser;

    public BrowserContext context;
    public Page page;



    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Construction & Teardown
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public BrowseService(ComdirectConfig config) {
        this.config = config;

        initPlaywright();
    }

    public void initPlaywright() {
        // Playwright initialisieren
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(config.getBrowser().isHeadless()));

        // Browser-Kontext und Seite erstellen
        context = browser.newContext();
        page = context.newPage();
    }

    /**
     * Cleanup method to close the Playwright browser and context. Avoids memory leaks.
     */
    @PreDestroy
    public void cleanUp() {
        if (page != null) page.close();
        if (context != null) context.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Playwright Interactions
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// @param url

    public String navigateToAndCloseCookieBanner(String url) {
        // Login-Seite laden
        page.navigate(url);

        // Warte, bis die Seite vollständig geladen ist
        page.waitForLoadState();

        // Cookie-Banner schließen (falls sichtbar)
        BrowserUtils.closeCookieBanner(page, config.getBrowser().isAutoCloseCookieBanner());

        addToHistory(url);

        // HTML der Seite extrahieren und in der WebView anzeigen
        return page.content();
    }

    public String performLogin(String username, String password) {
        // Warte, bis die Login-Seite vollständig geladen ist
        page.waitForLoadState();

        // Cookie-Banner schließen (falls sichtbar)
        BrowserUtils.closeCookieBanner(page, config.getBrowser().isAutoCloseCookieBanner());

        // Benutzername und Passwort eingeben
        page.fill("input[name='loginForm:userName']", username);
        page.fill("input[name='loginForm:pin']", password);

        // Login-Button klicken
        page.click("button[type='submit']");

        // Warte, bis die Seite vollständig geladen ist
        page.waitForLoadState();

        // HTML der Seite extrahieren und in der WebView anzeigen
        return page.content();
    }

    public String navigateTo(String url) {
        try {
            // Navigiere zur URL
            page.navigate(url);

            // Warte, bis die Seite vollständig geladen ist
            page.waitForLoadState();

            addToHistory(url);

            // Gebe den HTML-Inhalt zurück
            return page.content();
        } catch (Exception e) {
            System.err.println("Fehler beim Laden der Seite: " + e.getMessage());
            return "<html><body><h1>Fehler</h1><p>Die Seite konnte nicht geladen werden.</p></body></html>";
        }
    }

    public String navigateBack() {
        if (currentIndex > 0) {
            currentIndex--;
            page.navigate(history.get(currentIndex));
            page.waitForLoadState();
            return page.content(); // HTML der alten Seite
        }
        throw new IllegalStateException("Keine vorherige Seite verfügbar");
    }

    public String navigateForward() {
        if (currentIndex < history.size() - 1) {
            currentIndex++;
            page.navigate(history.get(currentIndex));
            page.waitForLoadState();
            return page.content(); // HTML der nächsten Seite
        }
        throw new IllegalStateException("Keine nächste Seite verfügbar");
    }

    private void addToHistory(String url) {
        if (currentIndex < history.size() - 1) {
            // Entferne alle zukünftigen Einträge, wenn wir in der Mitte des Verlaufs sind
            history = new ArrayList<>(history.subList(0, currentIndex + 1));
        }
        history.add(url);
        currentIndex = history.size() - 1; // Setze den Index auf das Ende der Historie
    }

    public String postForm(String formDataJson) {
        // Deserialisiere das JSON (formDataJson) und fülle Playwright-Formulare
        Map<String, String> formData = new Gson().fromJson(formDataJson, new TypeToken<Map<String, String>>() {}.getType());
        for (Map.Entry<String, String> entry : formData.entrySet()) {
            page.fill("input[name='" + entry.getKey() + "']", entry.getValue());
        }

        // Formular abschicken (z. B. durch einen Submit-Button-Klick)
        page.click("button[type='submit']"); // ToDo: Fix this

        // Warte, bis die Seite vollständig geladen ist
        page.waitForLoadState();

        // HTML der Seite extrahieren und in der WebView anzeigen
        return page.content();
    }

    public void changeBrowser(String browserType) {
        try {
            if (browser != null) {
                browser.close();
            }

            switch (browserType.toLowerCase()) {
                case "firefox":
                    browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(config.getBrowser().isHeadless()));
                    break;
                case "webkit":
                    browser = playwright.webkit().launch(new BrowserType.LaunchOptions().setHeadless(config.getBrowser().isHeadless()));
                    break;
                case "edge": // wie chromium, aber mit explizitem Pfad
                    // Edge mit explizitem Pfad starten
                    String edgePath = config.getBrowser().getEdgePath();
                    browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                            .setChannel("msedge") // Playwright erkennt Edge als "msedge"
                            .setExecutablePath(Path.of(edgePath))
                            .setHeadless(config.getBrowser().isHeadless()));
                    break;
                case "chromium":
                default:
                    browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(config.getBrowser().isHeadless()));
                    break;
            }

            context = browser.newContext();
            page = context.newPage();
            System.out.println("Browser gewechselt zu: " + browserType);
        } catch (Exception e) {
            System.err.println("Fehler beim Wechseln des Browsers: " + e.getMessage());
        }
    }

    public String refreshPage() {
        page.reload();
        page.waitForLoadState();
        return page.content();
    }
}

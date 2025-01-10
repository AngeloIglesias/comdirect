package comdirect.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.playwright.*;
import comdirect.controllers.BrowserUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class BrowseService {

    @Value("${comdirect.auto-close-cookie-banner}")
    boolean autoCloseCookieBanner;

    // Login-URL aus application.yml laden
//    @Value("${comdirect.login.url0}") //ToDo: Fix this
    private String loginUrl = "https://kunde.comdirect.de";

    private final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36";

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Var (Stateful Bean, ToDo: Externalize state to a separate class)
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    //ToDo: Change Encapsulation
    public Playwright playwright;
    public Browser browser;

    public BrowserContext context;
    public Page page;

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Constructors
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public BrowseService() {
        initPlaywright();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Playwright Interactions
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void initPlaywright() {
        // Playwright initialisieren
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));

        // Browser-Kontext und Seite erstellen
        context = browser.newContext();
        page = context.newPage();
    }

    public String getLoginPage() {
        // Login-Seite laden
        page.navigate(loginUrl);

        // Warte, bis die Seite vollständig geladen ist
        page.waitForLoadState();

        // Cookie-Banner schließen (falls sichtbar)
        BrowserUtils.closeCookieBanner(page, autoCloseCookieBanner);

        // HTML der Seite extrahieren und in der WebView anzeigen
        return page.content();
    }

    public String performLogin(String username, String password) {
        // Warte, bis die Login-Seite vollständig geladen ist
        page.waitForLoadState();

        // Cookie-Banner schließen (falls sichtbar)
        BrowserUtils.closeCookieBanner(page, autoCloseCookieBanner);

        // Benutzername und Passwort eingeben
        page.fill("input[name='loginForm:userName']", username);
        page.fill("input[name='loginForm:pin']", password);

        // Login-Button klicken
        page.click("button[type='submit']");

        // Warte, bis die Seite vollständig geladen ist
        page.waitForLoadState();

        // Cookie-Banner schließen (falls sichtbar)
        BrowserUtils.closeCookieBanner(page, autoCloseCookieBanner);

        // HTML der Seite extrahieren und in der WebView anzeigen
        return page.content();
    }

    public String loadPage(String url) {
        // Seite laden
        page.navigate(url);

        // Warte, bis die Seite vollständig geladen ist
        page.waitForLoadState();

//        // Cookie-Banner schließen (falls sichtbar)
//        BrowserUtils.closeCookieBanner(page, autoCloseCookieBanner);

        // HTML der Seite extrahieren und in der WebView anzeigen
        return page.content();
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

}

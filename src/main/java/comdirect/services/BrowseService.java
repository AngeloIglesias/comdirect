package comdirect.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class BrowseService {

    // Login-URL aus application.yml laden
    @Value("${comdirect.login.url0}")
    private String loginUrl;

    private final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36";
    private Map<String, String> cookies = new HashMap<>();

    /**
     * Lädt die Login-Seite und gibt den HTML-Inhalt zurück.
     *
     * @return HTML-Inhalt der Login-Seite
     * @throws Exception Falls ein Fehler auftritt
     */
    public String loadLoginPage() throws Exception {
        Connection.Response response = Jsoup.connect(loginUrl)
                .userAgent(userAgent)
                .execute();

        // Cookies speichern
        cookies.putAll(response.cookies());

        return response.body();
    }

    /**
     * Führt den Login durch und gibt die Antwortseite als HTML zurück.
     *
     * @param actionUrl Ziel-URL des Login-Formulars
     * @param username  Benutzername/Zugangsdaten
     * @param pin       Benutzer-PIN
     * @return HTML-Inhalt der Antwortseite
     * @throws Exception Falls ein Fehler auftritt
     */
    public String performLogin(String actionUrl, String username, String pin) throws Exception {
        Connection.Response response = Jsoup.connect(actionUrl)
                .userAgent(userAgent)
                .data("zugangsnummer", username)
                .data("pin", pin)
                .cookies(cookies)
                .method(Connection.Method.POST)
                .execute();

        // Cookies aktualisieren
        cookies.putAll(response.cookies());

        return response.body();
    }

    /**
     * Navigiert zu einer bestimmten Seite und gibt den HTML-Inhalt zurück.
     *
     * @param url Ziel-URL
     * @return HTML-Inhalt der Seite
     * @throws Exception Falls ein Fehler auftritt
     */
    public String navigateToPage(String url) throws Exception {
        Connection.Response response = Jsoup.connect(url)
                .userAgent(userAgent)
                .cookies(cookies)
                .execute();

        return response.body();
    }
}

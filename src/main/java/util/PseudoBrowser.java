package util;

import jodd.http.HttpBrowser;
import jodd.http.HttpRequest;
import jodd.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Simuliert einen Browser, um Webseiten herunterzuladen.
 */
@Slf4j
public class PseudoBrowser {

    private String userAgent;

    public PseudoBrowser(String defaultUserAgent) {
        this.userAgent = defaultUserAgent;
    }

    /**
     * Führt eine HTTP-Anfrage aus und gibt den JSoup-Document zurück.
     *
     * @param uri Die URL der Webseite.
     * @return JSoup-Document mit dem Inhalt der Webseite.
     */
    public Document grabWebsiteWithCookies(String uri) {
        log.debug("______________________ Starte HTTP Request ________________________");

        HttpBrowser httpBrowser = new HttpBrowser();
        HttpRequest request = HttpRequest.get(uri)
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "de-DE,de;q=0.9,en;q=0.8")
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1");

        // Anfrage senden
        HttpResponse response = httpBrowser.sendRequest(request);

        // Inhalt der Seite abrufen
        String html = response.bodyText();

        // HTML-String mit JSoup parsen
        Document document = Jsoup.parse(html);

        log.debug("______________________ Beende HTTP Request ________________________");

        return document;
    }

    /**
     * Überprüft, ob ein HTTP-Status ein Redirect ist.
     *
     * @param statusCode Der HTTP-Statuscode.
     * @return True, wenn der Status ein Redirect ist.
     */
    private static boolean isRedirect(int statusCode) {
        return statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308;
    }

    /**
     * Setzt den User-Agent für die Anfragen.
     *
     * @param userAgent Der zu verwendende User-Agent.
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}

package comdirect.controllers;

public class WebViewBridge {
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
}

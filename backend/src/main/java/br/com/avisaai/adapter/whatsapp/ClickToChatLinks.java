package br.com.avisaai.adapter.whatsapp;

import br.com.avisaai.application.WhatsAppLinks;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Adaptador gratuito: links oficiais "Click to Chat" do WhatsApp (https://faq.whatsapp.com/5913398998672934).
 * Não usa API paga nem automação não oficial; o envio é feito pelo próprio atendente, com o texto já pronto.
 */
@Component
public class ClickToChatLinks implements WhatsAppLinks {

    @Override
    public String chatLink(String phone, String text) {
        return "https://wa.me/" + digits(phone) + "?text=" + encode(text);
    }

    @Override
    public String desktopLink(String phone, String text) {
        return "whatsapp://send?phone=" + digits(phone) + "&text=" + encode(text);
    }

    private static String digits(String phone) {
        return phone == null ? "" : phone.replaceAll("\\D", "");
    }

    /** URLEncoder usa "+" para espaço; o WhatsApp espera "%20". */
    private static String encode(String text) {
        return URLEncoder.encode(text == null ? "" : text, StandardCharsets.UTF_8).replace("+", "%20");
    }
}

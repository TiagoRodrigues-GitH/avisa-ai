package br.com.avisaai;

import br.com.avisaai.adapter.whatsapp.ClickToChatLinks;
import br.com.avisaai.domain.DefaultTemplates;
import br.com.avisaai.domain.NotificationKind;
import br.com.avisaai.domain.Phone;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessagesAndLinksTest {

    @Test
    void normalizesBrazilianPhones() {
        assertThat(Phone.normalize("(43) 99999-1234")).isEqualTo("5543999991234");
        assertThat(Phone.normalize("+55 43 3333-1234")).isEqualTo("554333331234");
        assertThat(Phone.normalize("0055 11 98888-7777")).isEqualTo("5511988887777");
        assertThat(Phone.pretty("5543999991234")).isEqualTo("+55 (43) 99999-1234");
        assertThatThrownBy(() -> Phone.normalize("1234")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fillsVariablesAndNeverLeavesBracesForTheCustomer() {
        String text = DefaultTemplates.render("Olá, {nome}! {titulo} {desconhecida}em {data}.",
                Map.of("nome", "Ana", "titulo", "Consulta", "data", "sexta, 20/11"));
        assertThat(text).isEqualTo("Olá, Ana! Consulta em sexta, 20/11.");
        for (NotificationKind kind : NotificationKind.values()) {
            assertThat(DefaultTemplates.text(kind)).as(kind.name()).isNotBlank();
        }
    }

    @Test
    void buildsOfficialClickToChatLinks() {
        ClickToChatLinks links = new ClickToChatLinks();
        assertThat(links.chatLink("5543999991234", "Olá, Ana! Responda *1* & pronto"))
                .isEqualTo("https://wa.me/5543999991234?text=Ol%C3%A1%2C%20Ana%21%20Responda%20*1*%20%26%20pronto");
        assertThat(links.desktopLink("5543999991234", "Oi")).isEqualTo("whatsapp://send?phone=5543999991234&text=Oi");
    }
}

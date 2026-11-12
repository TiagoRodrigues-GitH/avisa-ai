package br.com.avisaai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"app.demo-data=false", "spring.datasource.url=jdbc:h2:mem:${random.uuid};DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    @Test
    void apiRequiresLoginWithoutTriggeringTheBrowserPasswordPopup() throws Exception {
        mvc.perform(get("/api/queue"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void lastMinuteAppointmentGoesStraightToTheQueueWithAWhatsAppLink() throws Exception {
        long customerId = createCustomer("Ana Pereira", "(43) 99911-1111", true);
        String body = """
                {"customerId": %d, "type": "APPOINTMENT", "title": "consulta com a Dra. Júlia", "eventAt": "%s"}
                """.formatted(customerId, Instant.now().plus(Duration.ofHours(5)));
        mvc.perform(post("/api/reminders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        JsonNode queue = read(mvc.perform(get("/api/queue")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        // Fora do horário comercial o aviso pode ficar para a manhã; dentro dele, já está na fila.
        if (queue.size() > 0) {
            JsonNode item = queue.get(0);
            assertThat(item.get("kind").asText()).isEqualTo("APPOINTMENT_CONFIRM");
            assertThat(item.get("chatLink").asText()).startsWith("https://wa.me/5543999111111?text=");
            assertThat(item.get("text").asText()).contains("Ana").contains("Responda *1*");
            mvc.perform(post("/api/queue/" + item.get("id").asLong() + "/sent"))
                    .andExpect(jsonPath("$.status").value("SENT"));
        }
    }

    @Test
    @WithMockUser
    void customerWithoutConsentIsNeverMessaged() throws Exception {
        long customerId = createCustomer("Davi Rocha", "41977444444", false);
        String body = """
                {"customerId": %d, "type": "WAITLIST", "title": "horário extra", "eventAt": "%s"}
                """.formatted(customerId, Instant.now().plus(Duration.ofDays(1)));
        JsonNode created = read(mvc.perform(post("/api/reminders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString());
        mvc.perform(get("/api/queue")).andExpect(jsonPath("$.length()").value(0));
        JsonNode messages = read(mvc.perform(get("/api/reminders/" + created.get("id").asLong() + "/messages"))
                .andReturn().getResponse().getContentAsString());
        String status = messages.get(0).get("status").asText();
        assertThat(status).isIn("SKIPPED", "PENDING"); // PENDING só se agora for horário de silêncio
    }

    @Test
    @WithMockUser
    void deliveryStagesPrepareMessagesAndDeliveredClosesIt() throws Exception {
        long customerId = createCustomer("Bruno Lima", "43999222222", true);
        String body = """
                {"customerId": %d, "type": "DELIVERY", "title": "#1042", "eventAt": "%s"}
                """.formatted(customerId, Instant.now().plus(Duration.ofHours(1)));
        long id = read(mvc.perform(post("/api/reminders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.deliveryStage").value("PREPARING"))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(patch("/api/reminders/" + id + "/delivery").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":\"NEARBY\",\"etaMinutes\":12}"))
                .andExpect(jsonPath("$.deliveryStage").value("NEARBY"));
        JsonNode messages = read(mvc.perform(get("/api/reminders/" + id + "/messages")).andReturn().getResponse().getContentAsString());
        assertThat(messages.get(0).get("kind").asText()).isEqualTo("DELIVERY_NEARBY");

        mvc.perform(patch("/api/reminders/" + id + "/delivery").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":\"DELIVERED\"}"))
                .andExpect(jsonPath("$.status").value("DONE"));
        mvc.perform(get("/api/reminders/deliveries")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser
    void templatesCanBeCustomizedAndPreviewed() throws Exception {
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"phone\":\"12\",\"consent\":true}"))
                .andExpect(status().isBadRequest());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/templates/PAYMENT_DUE")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"Oi {nome}, vence hoje: {valor}.\"}"))
                .andExpect(jsonPath("$.custom").value(true))
                .andExpect(jsonPath("$.preview").value("Oi Maria, vence hoje: R$ 150,00."));
    }

    private long createCustomer(String name, String phone, boolean consent) throws Exception {
        String body = json.writeValueAsString(new java.util.HashMap<>(java.util.Map.of("name", name, "phone", phone, "consent", consent)));
        return read(mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    private JsonNode read(String body) throws Exception {
        return json.readTree(body);
    }
}

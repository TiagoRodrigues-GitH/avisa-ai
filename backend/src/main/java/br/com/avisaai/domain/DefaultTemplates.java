package br.com.avisaai.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Textos padrão (editáveis na tela "Mensagens") e o preenchimento das variáveis entre chaves:
 * {nome}, {empresa}, {titulo}, {data}, {hora}, {valor}, {endereco}, {minutos}.
 * Mensagens curtas, com o nome da empresa e uma ação clara (responder 1 ou 2), como recomendam os guias de
 * mensagens transacionais do próprio WhatsApp Business.
 */
public final class DefaultTemplates {

    private static final Pattern VAR = Pattern.compile("\\{([a-z_]+)}");
    private static final Map<NotificationKind, String> TEXT = new EnumMap<>(NotificationKind.class);

    static {
        TEXT.put(NotificationKind.APPOINTMENT_CONFIRM, "Olá, {nome}! Aqui é da {empresa}. Lembrete: {titulo} em {data} às {hora}.\nResponda *1* para confirmar ou *2* para remarcar.");
        TEXT.put(NotificationKind.APPOINTMENT_SOON, "{nome}, seu horário na {empresa} é hoje às {hora}. Endereço: {endereco}. Até já!");
        TEXT.put(NotificationKind.PAYMENT_UPCOMING, "Olá, {nome}! Lembrete da {empresa}: {titulo} no valor de {valor} vence em {data}. Se já pagou, desconsidere.");
        TEXT.put(NotificationKind.PAYMENT_DUE, "{nome}, hoje ({data}) vence {titulo} — {valor}. Qualquer dúvida, é só responder aqui.");
        TEXT.put(NotificationKind.PAYMENT_OVERDUE, "Olá, {nome}. Não identificamos o pagamento de {titulo} ({valor}), vencido em {data}. Se já pagou, envie o comprovante por aqui, por favor.");
        TEXT.put(NotificationKind.PICKUP_READY, "Boa notícia, {nome}! {titulo} já está pronto para retirada na {empresa} ({endereco}).");
        TEXT.put(NotificationKind.PICKUP_REMINDER, "{nome}, {titulo} continua aguardando a sua retirada na {empresa}. Podemos ajudar com algo?");
        TEXT.put(NotificationKind.DELIVERY_OUT, "{nome}, seu pedido {titulo} saiu para entrega! 🚚");
        TEXT.put(NotificationKind.DELIVERY_NEARBY, "{nome}, o entregador está a caminho e deve chegar em cerca de {minutos} minutos.");
        TEXT.put(NotificationKind.DELIVERY_DONE, "Pedido {titulo} entregue. Obrigado pela preferência, {nome}!");
        TEXT.put(NotificationKind.RETURN_DUE, "Olá, {nome}! Está chegando a hora do seu retorno: {titulo}, previsto para {data}. Quer agendar? Responda com o melhor dia e horário.");
        TEXT.put(NotificationKind.WAITLIST_SLOT, "{nome}, abriu um horário na {empresa}: {data} às {hora}. Quer ficar com ele? Responda *1* para sim.");
        TEXT.put(NotificationKind.QUOTE_PENDING, "Olá, {nome}! O orçamento de {titulo} ({valor}) está pronto. Responda *1* para aprovar ou tire suas dúvidas por aqui.");
        TEXT.put(NotificationKind.QUOTE_FOLLOWUP, "{nome}, conseguiu avaliar o orçamento de {titulo}? Estamos à disposição.");
        TEXT.put(NotificationKind.FEEDBACK_REQUEST, "{nome}, obrigado pela visita à {empresa}! De 0 a 10, quanto você nos recomendaria? Sua resposta nos ajuda a melhorar.");
    }

    private DefaultTemplates() {
    }

    public static String text(NotificationKind kind) {
        return TEXT.get(kind);
    }

    /** Troca cada {variavel} pelo valor; variáveis sem valor ficam em branco (nunca aparece "{x}" para o cliente). */
    public static String render(String template, Map<String, String> values) {
        Matcher m = VAR.matcher(template);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String value = values.getOrDefault(m.group(1), "");
            m.appendReplacement(out, Matcher.quoteReplacement(value == null ? "" : value));
        }
        m.appendTail(out);
        return out.toString().replaceAll(" {2,}", " ").replace(" .", ".").replace("()", "").trim();
    }
}

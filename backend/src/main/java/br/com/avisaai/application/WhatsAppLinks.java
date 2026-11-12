package br.com.avisaai.application;

/**
 * Porta de saída: como a mensagem chega ao WhatsApp. A implementação gratuita monta o link oficial
 * "clique para conversar" (wa.me) com o texto pronto; quem envia é o atendente, com um toque.
 * Outra implementação (por exemplo a API paga da Meta) poderia ser ligada aqui sem mudar o resto do sistema.
 */
public interface WhatsAppLinks {

    /** Link que abre a conversa no app ou no WhatsApp Web, com a mensagem já escrita. */
    String chatLink(String phone, String text);

    /** Link que abre direto o aplicativo do WhatsApp no computador (WhatsApp Desktop). */
    String desktopLink(String phone, String text);
}

package br.com.avisaai.domain;

/** Telefone no formato internacional usado pelo WhatsApp (só dígitos, com DDI). */
public final class Phone {

    private Phone() {
    }

    /**
     * Normaliza um número brasileiro: "(43) 99999-1234" → "5543999991234".
     * Números com 10 ou 11 dígitos recebem o DDI 55; números que já começam com 55 e têm 12–13 dígitos ficam como estão.
     *
     * @throws IllegalArgumentException se não parecer um telefone válido
     */
    public static String normalize(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Telefone obrigatório");
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.startsWith("00")) {
            digits = digits.substring(2);
        }
        if (digits.length() == 10 || digits.length() == 11) {
            digits = "55" + digits;
        }
        if (digits.length() < 12 || digits.length() > 15) {
            throw new IllegalArgumentException("Telefone inválido: informe DDD e número");
        }
        return digits;
    }

    /** "5543999991234" → "+55 (43) 99999-1234" para exibição. */
    public static String pretty(String normalized) {
        if (normalized == null || !normalized.startsWith("55") || normalized.length() < 12) {
            return normalized == null ? "" : "+" + normalized;
        }
        String ddd = normalized.substring(2, 4);
        String number = normalized.substring(4);
        int split = number.length() - 4;
        return "+55 (" + ddd + ") " + number.substring(0, split) + "-" + number.substring(split);
    }
}

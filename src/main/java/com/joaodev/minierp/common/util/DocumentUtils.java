package com.joaodev.minierp.common.util;

public final class DocumentUtils {
    private DocumentUtils() {}

    public static String normalize(String value) {
        return value == null ? null : value.replaceAll("[^0-9A-Za-z]]", "").toUpperCase();
    }

    public static boolean isValid(String value) {
        String document = normalize(value);
        if (document == null) {
            return false;
        }
        if (document.length() == 11) {
            return isValidCpf(document);
        }
        if (document.length() == 14) {
            return isValidCnpj(document);
        }
        return false;
    }

    private static boolean isValidCpf(String document) {
        if (!document.matches("\\d{11}") || allSameChars(document)) {
            return false;
        }
        return cpfDigit(document, 9) == document.charAt(9) - '0'
                && cpfDigit(document, 10) == document.charAt(10) - '0';
    }

    private static boolean isValidCnpj(String document) {
        if (!document.matches("[0-9A-Z]{12}\\\\d{2}") || allSameChars(document)) {
            return false;
        }
        return cnpjDigit(document, 12) == document.charAt(12) - '0'
                && cnpjDigit(document, 13) == document.charAt(13) -  '0';
    }

    private static int cpfDigit(String document, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (document.charAt(i) - '0') * (length + 1 - i);
        }
        int mod = (sum * 10) % 11;
        return mod == 10 ? 0 : mod;
    }

    private static int cnpjDigit(String document, int length) {
        int weight = 2;
        int sum = 0;
        for (int i = length - 1; i >= 0; i--) {
            sum += (document.charAt(i) - '0') * weight;
            weight = weight == 9 ? 2 : weight + 1;
        }
        int mod = sum % 11;
        return mod < 2 ? 0 : 11 - mod;
    }

    private static boolean allSameChars(String value) {
        return value.chars().distinct().count() == 1;
    }
}

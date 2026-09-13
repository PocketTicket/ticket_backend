package com.example.ticket;

import jakarta.enterprise.context.ApplicationScoped;

import java.security.SecureRandom;

/**
 * Produces the codes that go into the QR images.
 *
 * <p>The code is the only thing standing between a guest and the door, so it is
 * drawn from {@link SecureRandom} rather than from a counter or a UUID derived
 * from time: 24 characters out of a 32 character alphabet is 120 bits, which is
 * far beyond guessing even if someone knows a few valid codes.
 *
 * <p>The alphabet is upper case letters and digits with I, O, 0 and 1 removed.
 * That keeps a code readable if it ever has to be typed in by hand when a phone
 * screen is too dark to scan, and it lets the QR encoder use its compact
 * alphanumeric mode instead of byte mode.
 *
 * <p>Uniqueness is not assumed here - the {@code ticket_code} column is UNIQUE and
 * the caller retries on the (astronomically unlikely) collision.
 */
@ApplicationScoped
public class TicketCodeGenerator {

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LENGTH = 24;

    private final SecureRandom random = new SecureRandom();

    public String newCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);

        for (int i = 0; i < CODE_LENGTH; i++) {
            // ALPHABET.length is a power of two, so nextInt stays unbiased.
            code.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return code.toString();
    }
}

package com.example.service;

import com.example.models.order.Order;
import com.example.models.ticket.Ticket;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * The emails to customers. They are sent from a noreply address (MAIL_FROM); questions go
 * to the support address (SUPPORT_EMAIL), which forwards them to the admin.
 */
@ApplicationScoped
public class MailService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final int QR_CODE_SIZE_PX = 400;

    @Inject
    Mailer mailer;

    @ConfigProperty(name = "ticket.frontend-url")
    String frontendUrl;

    @ConfigProperty(name = "ticket.bank.recipient")
    String bankRecipient;

    @ConfigProperty(name = "ticket.bank.iban")
    String bankIban;

    @ConfigProperty(name = "ticket.support-email")
    String supportEmail;

    /** Sent right after an order was placed: what to pay, to whom and by when. */
    public void sendPaymentInstructions(Order order) {
        String text = """
                Hallo %s %s,

                vielen Dank für Ihre Bestellung! Ihre Tickets sind bis zum %s für Sie reserviert.
                Bitte überweisen Sie den Betrag bis dahin, sonst verfällt die Reservierung.

                Betrag:            %s
                Empfänger:         %s
                IBAN:              %s
                Verwendungszweck:  Bestellung %d

                Sobald Ihre Zahlung eingegangen ist, schicken wir Ihnen Ihre Tickets per E-Mail.

                Bei Fragen erreichen Sie uns unter %s.
                """.formatted(
                order.user().firstName(),
                order.user().lastName(),
                DATE.format(order.paymentDueAt()),
                NumberFormat.getCurrencyInstance(Locale.GERMANY).format(order.total()),
                bankRecipient,
                bankIban,
                order.orderId(),
                supportEmail);

        send(Mail.withText(order.user().email(),
                "Bestellung " + order.orderId() + ": Zahlungsinformationen", text));
    }

    /** Sent once the payment has been confirmed: one QR code per ticket, attached as PNG. */
    public void sendTickets(Order order) {
        String text = """
                Hallo %s %s,

                Ihre Zahlung ist eingegangen, vielen Dank! Im Anhang finden Sie Ihre Tickets,
                einen QR-Code pro Person. Bitte halten Sie diese am Einlass bereit.

                Bei Fragen erreichen Sie uns unter %s.
                """.formatted(order.user().firstName(), order.user().lastName(), supportEmail);

        Mail mail = Mail.withText(order.user().email(),
                "Bestellung " + order.orderId() + ": Ihre Tickets", text);

        for (Ticket ticket : order.tickets()) {
            mail.addAttachment("ticket-" + ticket.ticketId() + ".png",
                    qrCode(frontendUrl + "/tickets/" + ticket.code()), "image/png");
        }
        send(mail);
    }

    /** Sent when an order was cancelled because the payment did not arrive in time. */
    public void sendCancellation(Order order) {
        String text = """
                Hallo %s %s,

                leider ist für Ihre Bestellung %d bis zum %s keine Zahlung eingegangen.
                Die Reservierung wurde deshalb storniert und die Tickets wurden wieder freigegeben.

                Falls Sie bereits überwiesen haben, melden Sie sich bitte unter %s.
                """.formatted(
                order.user().firstName(),
                order.user().lastName(),
                order.orderId(),
                DATE.format(order.paymentDueAt()),
                supportEmail);

        send(Mail.withText(order.user().email(),
                "Bestellung " + order.orderId() + ": Reservierung storniert", text));
    }

    /** Pressing "reply" on any of these emails writes to the support address. */
    private void send(Mail mail) {
        mailer.send(mail.setReplyTo(supportEmail));
    }

    /** The QR code holds a link, so scanning it opens the ticket's page on the website. */
    private static byte[] qrCode(String content) {
        try {
            BitMatrix matrix = new QRCodeWriter()
                    .encode(content, BarcodeFormat.QR_CODE, QR_CODE_SIZE_PX, QR_CODE_SIZE_PX);
            BufferedImage image = new BufferedImage(
                    matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);

            for (int x = 0; x < matrix.getWidth(); x++) {
                for (int y = 0; y < matrix.getHeight(); y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }

            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(image, "png", png);
            return png.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Could not create the QR code", e);
        }
    }
}

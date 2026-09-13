package com.example.ticket;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import jakarta.enterprise.context.ApplicationScoped;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Renders a ticket code as a PNG QR image, ready to be attached to the
 * confirmation email or served to the browser.
 */
@ApplicationScoped
public class QrCodeGenerator {

    private static final int DEFAULT_SIZE_PX = 512;

    /**
     * Error correction Q recovers about 25% of the image, which is what makes a
     * QR still scan off a creased printout or a fingerprinted phone screen.
     */
    private static final Map<EncodeHintType, Object> HINTS = Map.of(
            EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q,
            EncodeHintType.MARGIN, 2,
            EncodeHintType.CHARACTER_SET, "UTF-8");

    public byte[] toPng(String content) {
        return toPng(content, DEFAULT_SIZE_PX);
    }

    public byte[] toPng(String content, int sizePx) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Cannot encode an empty QR code");
        }

        try {
            BitMatrix matrix = new QRCodeWriter()
                    .encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, HINTS);

            BufferedImage image = new BufferedImage(
                    matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);

            for (int x = 0; x < matrix.getWidth(); x++) {
                for (int y = 0; y < matrix.getHeight(); y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }

            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", png);
            return png.toByteArray();
        } catch (WriterException e) {
            throw new IllegalStateException("Could not encode QR code for \"" + content + "\"", e);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write QR code PNG", e);
        }
    }
}

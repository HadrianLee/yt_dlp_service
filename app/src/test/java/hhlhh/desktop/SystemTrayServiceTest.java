package hhlhh.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Image;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class SystemTrayServiceTest {

    @Test
    void unsupportedTrayMethodsAreNoOps() {
        SystemTrayService trayService = new FakeSystemTrayService(false);

        assertFalse(trayService.isTraySupported());
        assertFalse(trayService.traySupportedProperty().get());

        trayService.attachStage(null);
        trayService.setOpenDownloadAction(null);
        trayService.setOpenSettingsAction(null);
        trayService.setExitAction(null);
        trayService.showTrayIcon(null);
        trayService.notifyTrayMessage("Title", "Message");
        trayService.removeTrayIcon();
    }

    @Test
    void fallbackTrayImageIsSmallNonEmptyBufferedImage() {
        SystemTrayService trayService = new SystemTrayService();

        Image image = trayService.createFallbackTrayImage();

        assertTrue(image instanceof BufferedImage);
        BufferedImage bufferedImage = (BufferedImage) image;
        assertEquals(16, bufferedImage.getWidth());
        assertEquals(16, bufferedImage.getHeight());
        assertTrue(hasVisiblePixel(bufferedImage));
    }

    private boolean hasVisiblePixel(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = (image.getRGB(x, y) >>> 24) & 0xff;
                if (alpha > 0) {
                    return true;
                }
            }
        }
        return false;
    }
}

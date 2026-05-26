package hhlhh.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExitDialogTest {

    @Test
    void exposesTitleAndMessage() {
        ExitDialog exitDialog = new ExitDialog("Still downloading", "Stop before closing.");

        assertEquals("Still downloading", exitDialog.title());
        assertEquals("Stop before closing.", exitDialog.message());
    }

    @Test
    void confirmExitRecordsConfirmedState() {
        ExitDialog exitDialog = new ExitDialog("Exit", "Confirm?");

        exitDialog.confirmExit(null);

        assertTrue(exitDialog.isExitConfirmed());
    }

    @Test
    void cancelExitClearsConfirmedState() {
        ExitDialog exitDialog = new ExitDialog("Exit", "Confirm?");
        exitDialog.confirmExit(null);

        exitDialog.cancelExit(null);

        assertFalse(exitDialog.isExitConfirmed());
    }
}

package org.irmacard.irma_kiosk;

import net.sf.scuba.smartcards.CardService;
import net.sf.scuba.smartcards.TerminalCardService;
import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.idemix.IdemixCredentials;
import org.irmacard.credentials.idemix.descriptions.IdemixVerificationDescription;
import org.irmacard.credentials.idemix.smartcard.CardChangedListener;
import org.irmacard.credentials.idemix.smartcard.IRMACard;
import org.irmacard.credentials.idemix.smartcard.IRMACardHelper;
import org.irmacard.credentials.idemix.smartcard.SmartCardEmulatorService;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Temporary class that verifies the credentials issued in IssueSurfnetRoot
 */
public class VerifyTryout {

    public VerifyTryout() {
        try {
            CardService cs = getNewCardService();
           // CardService cs = getRealCardService();
            IdemixVerificationDescription vd = new IdemixVerificationDescription(
                    "Surfnet", "rootAll");
            Attributes attr = new IdemixCredentials(cs).verify(vd);
            System.out.println(attr);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static CardService getRealCardService() throws CardException {
        CardService cs;
        List<CardTerminal> terminalList = TerminalFactory.getDefault().terminals().list();
        if(!terminalList.isEmpty()) {
            CardTerminal terminal = terminalList.get(0);
            return new TerminalCardService(terminal);
        } else {
            return null;
//            throw new NoCardReaderFoundException("Couldn't find card reader");
        }

//        return cs;
    }

    public static CardService getNewCardService() throws CardException {
        CardService cs;

        final Path path = Paths.get(System.getProperty("user.dir"), "card.json");
        IRMACard card = IRMACardHelper.loadState(path);
        SmartCardEmulatorService emu = new SmartCardEmulatorService(card);
        emu.addListener(new CardChangedListener() {
            @Override
            public void cardChanged(IRMACard card) {
                IRMACardHelper.storeState(card, path);
            }
        });
        cs = emu;

        return cs;
    }
}

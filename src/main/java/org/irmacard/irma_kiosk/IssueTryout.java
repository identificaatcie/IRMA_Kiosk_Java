package org.irmacard.irma_kiosk;

import net.sf.scuba.smartcards.CardService;
import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.idemix.IdemixCredentials;
import org.irmacard.credentials.idemix.IdemixSecretKey;
import org.irmacard.credentials.idemix.info.IdemixKeyStore;
import org.irmacard.credentials.idemix.smartcard.IRMACard;
import org.irmacard.credentials.idemix.smartcard.IRMACardHelper;
import org.irmacard.credentials.idemix.smartcard.SmartCardEmulatorService;
import org.irmacard.credentials.info.CredentialDescription;
import org.irmacard.credentials.info.DescriptionStore;
import org.irmacard.idemix.IdemixService;

import javax.smartcardio.CardException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A temporary class just used to tryout issuing to an IRMA card
 */
public class IssueTryout {
    private CardService cs;
    private IRMACard card;

    public IssueTryout() {

        try {
            initEmuCardService();
            CredentialDescription cd = DescriptionStore.getInstance().
                    getCredentialDescriptionByName("Surfnet", "root");
            IdemixSecretKey isk = IdemixKeyStore.getInstance().getSecretKey(cd);


            // Setup the attributes that will be issued to the card
            Attributes attributes = new Attributes();
            attributes.add("userID", "s1234567@student.ru.nl".getBytes());
            attributes.add("securityHash", "DEADBEEF".getBytes());

            // Setup a connection and send pin for emulated card service
            IdemixService is = new IdemixService(cs);
            IdemixCredentials ic = new IdemixCredentials(is);
            ic.connect();

            is.sendPin("0000".getBytes());
            ic.issue(cd, isk, attributes, null); // null indicates default expiry


            final Path path = Paths.get(System.getProperty("user.dir"), "card.json");
            IRMACardHelper.storeState(card, path);

            // Setup a connection to a real card
//            CardService real = new TerminalCardService();   <--- doesn't exist?

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void initEmuCardService() throws CardException {
        card = new IRMACard();
        SmartCardEmulatorService emu = new SmartCardEmulatorService(card);
        cs = emu;
    }
}

package org.irmacard.irma_kiosk;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.sf.scuba.smartcards.CardService;
import org.irmacard.credentials.Attributes;
import org.irmacard.credentials.CredentialsException;
import org.irmacard.credentials.idemix.IdemixCredentials;
import org.irmacard.credentials.idemix.IdemixSecretKey;
import org.irmacard.credentials.idemix.descriptions.IdemixVerificationDescription;
import org.irmacard.credentials.idemix.info.IdemixKeyStore;
import org.irmacard.credentials.idemix.smartcard.CardChangedListener;
import org.irmacard.credentials.idemix.smartcard.IRMACard;
import org.irmacard.credentials.idemix.smartcard.IRMACardHelper;
import org.irmacard.credentials.idemix.smartcard.SmartCardEmulatorService;
import org.irmacard.credentials.info.CredentialDescription;
import org.irmacard.credentials.info.DescriptionStore;
import org.irmacard.credentials.info.InfoException;
import org.irmacard.idemix.IdemixService;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonObjectParser;


import javax.smartcardio.CardException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Formatter;

/**
 * Created by wietse on 21-9-15.
 */
public class IRMAKiosk {

    private HttpTransport transport;
    private JsonObjectParser jsonObjectParser;
    private final String apikey = "";
    private final Boolean debug = true;
    private CardService cs;
    private IRMACard card;
    private JsonObject result;
    private String PIN;


    public IRMAKiosk()
    {
        transport = new NetHttpTransport.Builder().build();

        URI core = new File(System.getProperty("user.dir")).toURI().resolve("irma_configuration/");
        DescriptionStore.setCoreLocation(core);
        IdemixKeyStore.setCoreLocation(core);

        //Debug setup
        if(debug)
        {
            try{
                cs = getNewCardService();
                card = new IRMACard();
                PIN = "0000";
                //IssueThaliaRoot(cs,card);
                IssueSurfnetRoot(cs, card);
            } catch (CardException e) {
                e.printStackTrace();
            }
        }
        result = VerifyThaliaRoot(cs);
        if(result == null)
        {
            System.out.println("Failed to verify by thalia root. Verifying by surfnet root.");
            result = VerifySurfnetRoot(cs);
            if(result == null)
            {
                System.out.println("Failed to verify by surfnet root. Ask the identificaatcie to fix your root credentials.");
                return;
            }
        }
        System.out.println("Verification succeeded!");
        IssueThaliaCredentials(cs, card, result, PIN);
        System.out.println("Issue succesful!");

    }

    public static void main(String[] args) {

        IRMAKiosk kiosk = new IRMAKiosk();

    }

    public JsonObject VerifySurfnetRoot(CardService cs) {
        try {

            IdemixVerificationDescription vd = new IdemixVerificationDescription(
                    "Surfnet", "rootAll");
            Attributes attr = new IdemixCredentials(cs).verify(vd);

            String SurfnetRoot = new String(attr.get("userID"));
            String mode = "student_number";
            String value = SurfnetRoot.substring(0,8);

            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb);
            formatter.format("https://thalia.nu/api/irma_api.php?apikey=%s&%s=%s",apikey,mode,value);
            HttpResponse response = transport.createRequestFactory().buildGetRequest(new GenericUrl(sb.toString())).execute();
            JsonParser jp = new JsonParser();

            JsonObject jo = jp.parse(response.parseAsString()).getAsJsonObject();
            if(jo.get("status").getAsString().equals("ok"))
            {
                return jo;
            }


        } catch (InfoException e) {
            e.printStackTrace();
        } catch (CredentialsException e) {
            return null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }




    public JsonObject VerifyThaliaRoot(CardService cs) {
        try {

            IdemixVerificationDescription vd = new IdemixVerificationDescription(
                    "Thalia", "rootAll");
            Attributes attr = new IdemixCredentials(cs).verify(vd);

            String ThaliaUser = new String(attr.get("userID"));
            String mode = "thalia_username";

            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb);
            formatter.format("https://thalia.nu/api/irma_api.php?apikey=%s&%s=%s",apikey,mode,ThaliaUser);
            HttpResponse response = transport.createRequestFactory().buildGetRequest(new GenericUrl(sb.toString())).execute();
            JsonParser jp = new JsonParser();

            JsonObject jo = jp.parse(response.parseAsString()).getAsJsonObject();
            if(jo.get("status").getAsString().equals("ok"))
            {
                return jo;
            }


        } catch (InfoException e) {
            e.printStackTrace();
        } catch (CredentialsException e) {
            return null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static CardService getNewCardService() throws CardException {
        final Path path = Paths.get(System.getProperty("user.dir"), "card.json");
        IRMACard card = IRMACardHelper.loadState(path);
        SmartCardEmulatorService emu = new SmartCardEmulatorService(card);
        emu.addListener(new CardChangedListener() {
            @Override
            public void cardChanged(IRMACard card) {
                IRMACardHelper.storeState(card, path);
            }
        });
        return emu;
    }

    public void IssueThaliaCredentials(CardService cs, IRMACard card, JsonObject result,String PIN) {

        //Issue Membership attribute
        try {
            CredentialDescription cd = DescriptionStore.getInstance().
                    getCredentialDescriptionByName("Thalia", "membership");
            IdemixSecretKey isk = IdemixKeyStore.getInstance().getSecretKey(cd);
            // Setup the attributes that will be issued to the card
            Attributes attributes = new Attributes();
            String membership_type = result.get("membership_type").getAsString();
            if(membership_type.contains("Membership"))
            {
                attributes.add("isMember", "yes".getBytes());
            }
            else if(membership_type.contains("Honorary"))
            {
                attributes.add("isHonoraryMember", "yes".getBytes());
            }
            else if(membership_type.contains("Benefactor"))
            {
                attributes.add("isBegunstiger", "yes".getBytes());
            }
            // Setup a connection and send pin for emulated card service
            IdemixService is = new IdemixService(cs);
            IdemixCredentials ic = new IdemixCredentials(is);
            ic.connect();
            is.sendPin(PIN.getBytes());
            System.out.println("ISK± " + isk + "attributes± " + attributes + "cd± " + cd);
            ic.issue(cd, isk, attributes, null); // null indicates default expiry
            final Path path = Paths.get(System.getProperty("user.dir"), "card.json");
            IRMACardHelper.storeState(card, path);

        } catch (Exception e) {
            e.printStackTrace();
        }
        //Issue over18 attribute
        try {
            CredentialDescription cd = DescriptionStore.getInstance().
                    getCredentialDescriptionByName("Thalia", "age");
            IdemixSecretKey isk = IdemixKeyStore.getInstance().getSecretKey(cd);
            // Setup the attributes that will be issued to the card
            Attributes attributes = new Attributes();
            String bday =  result.get("birthday").getAsString();
            System.out.println(bday);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime birthday = LocalDateTime.parse(bday, formatter);
            birthday = birthday.plusYears(18);
            if (birthday.isBefore(LocalDateTime.now()))
            {
                System.out.println("Je bent meerderjarig!");
                attributes.add("over18","yes".getBytes());
            }
            else
            {
                attributes.add("over18","no".getBytes());
            }
            // Setup a connection and send pin for emulated card service
            IdemixService is = new IdemixService(cs);
            IdemixCredentials ic = new IdemixCredentials(is);
            ic.connect();
            is.sendPin(PIN.getBytes());
            ic.issue(cd, isk, attributes, null); // null indicates default expiry
            final Path path = Paths.get(System.getProperty("user.dir"), "card.json");
            IRMACardHelper.storeState(card, path);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void IssueThaliaRoot(CardService cs, IRMACard card) {

        try {
            CredentialDescription cd = DescriptionStore.getInstance().
                    getCredentialDescriptionByName("Thalia", "root");
            IdemixSecretKey isk = IdemixKeyStore.getInstance().getSecretKey(cd);


            // Setup the attributes that will be issued to the card
            Attributes attributes = new Attributes();
            attributes.add("userID", "wkuipers".getBytes());

            // Setup a connection and send pin for emulated card service
            IdemixService is = new IdemixService(cs);
            IdemixCredentials ic = new IdemixCredentials(is);
            ic.connect();

            is.sendPin("0000".getBytes());
            ic.issue(cd, isk, attributes, null); // null indicates default expiry


            final Path path = Paths.get(System.getProperty("user.dir"), "card.json");
            IRMACardHelper.storeState(card, path);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public void IssueSurfnetRoot(CardService cs, IRMACard card) {

        try {
            CredentialDescription cd = DescriptionStore.getInstance().
                    getCredentialDescriptionByName("Surfnet", "root");
            IdemixSecretKey isk = IdemixKeyStore.getInstance().getSecretKey(cd);


            // Setup the attributes that will be issued to the card
            Attributes attributes = new Attributes();
            attributes.add("userID", "s4317904@student.ru.nl".getBytes());
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

}

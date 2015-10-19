package org.irmacard.irma_kiosk;

import org.irmacard.credentials.idemix.info.IdemixKeyStore;
import org.irmacard.credentials.info.DescriptionStore;

import java.io.File;
import java.net.URI;


public class IRMAUtil {
    public static void nain(String[] args) {
        URI core = new File(System.getProperty("user.dir")).toURI().resolve("irma_configuration/");
        DescriptionStore.setCoreLocation(core);
        IdemixKeyStore.setCoreLocation(core);
        System.out.println("IssueSurfnetRoot:");
        new IssueTryout();
        System.out.println("VerifyTryout:");
        new VerifyTryout();
    }
}

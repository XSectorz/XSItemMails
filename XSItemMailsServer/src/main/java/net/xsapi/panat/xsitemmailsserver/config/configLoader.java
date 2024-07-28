package net.xsapi.panat.xsitemmailsserver.config;

import java.io.IOException;

public class configLoader {


    public configLoader() {
        try {
            new mainConfig();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

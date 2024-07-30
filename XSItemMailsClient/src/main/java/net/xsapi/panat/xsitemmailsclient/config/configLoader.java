package net.xsapi.panat.xsitemmailsclient.config;

public class configLoader {

    public configLoader() {
        new mainConfig().loadConfigu();
        new menuConfig().loadConfigu(XS_MENU_FILE.XS_MAIN_MENU);
    }

}

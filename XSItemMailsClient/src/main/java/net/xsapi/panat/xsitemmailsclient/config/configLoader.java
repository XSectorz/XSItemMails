package net.xsapi.panat.xsitemmailsclient.config;

public class configLoader {

    public configLoader() {
        new mainConfig().loadConfigu();
        new messagesConfig().loadConfigu();
        new menuConfig().loadConfigu(XS_MENU_FILE.XS_MAIN_MENU);
        new menuConfig().loadConfigu(XS_MENU_FILE.XS_ITEM_CREATE);
        new menuConfig().loadConfigu(XS_MENU_FILE.XS_REWARD_ITEMS);
        new menuConfig().loadConfigu(XS_MENU_FILE.XS_INVENTORY);
        new menuConfig().loadConfigu(XS_MENU_FILE.XS_OTHER_INVENTORY);
    }

}

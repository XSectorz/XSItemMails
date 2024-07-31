package net.xsapi.panat.xsitemmailsserver.objects;

import java.util.ArrayList;

public class XSItemmails {

    private int id;
    private String itemName;
    private String itemDisplay;
    private ArrayList<String> rewardItems = new ArrayList<>();
    private ArrayList<String> rewardCommands = new ArrayList<>();


    public XSItemmails(int id,String itemName,String itemDisplay,ArrayList<String> rewardItems,ArrayList<String> rewardCommands) {

        this.id = id;
        this.itemName = itemName;
        this.itemDisplay = itemDisplay;
        this.rewardItems = rewardItems;
        this.rewardCommands = rewardCommands;

    }

    public String getItemDisplay() {
        return itemDisplay;
    }

    public void setItemDisplay(String itemDisplay) {
        this.itemDisplay = itemDisplay;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public void setRewardCommands(ArrayList<String> rewardCommands) {
        this.rewardCommands = rewardCommands;
    }

    public void setRewardItems(ArrayList<String> rewardItems) {
        this.rewardItems = rewardItems;
    }

    public int getId() {
        return id;
    }

    public ArrayList<String> getRewardCommands() {
        return rewardCommands;
    }

    public ArrayList<String> getRewardItems() {
        return rewardItems;
    }

    public String getItemName() {
        return itemName;
    }
}

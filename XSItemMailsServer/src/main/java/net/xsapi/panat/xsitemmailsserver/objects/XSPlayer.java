package net.xsapi.panat.xsitemmailsserver.objects;

import java.util.ArrayList;

public class XSPlayer {

    private int id;
    private String name;
    private ArrayList<XSRewards> rewardsList = new ArrayList<>();


    public XSPlayer(int id,String name) {
        this.id = id;
        this.name = name;
    }

    public ArrayList<XSRewards> getRewardsList() {
        return rewardsList;
    }

    public void setRewardsList(ArrayList<XSRewards> rewardsList) {
        this.rewardsList = rewardsList;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }
}

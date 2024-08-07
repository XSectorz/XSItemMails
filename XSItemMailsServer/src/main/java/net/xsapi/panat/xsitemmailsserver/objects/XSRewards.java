package net.xsapi.panat.xsitemmailsserver.objects;

public class XSRewards {

    private String idKeyReward;
    private int count;

    public XSRewards(String idKeyReward, int count) {

        this.count = count;
        this.idKeyReward = idKeyReward;

    }

    public String getIdKeyReward() {
        return idKeyReward;
    }

    public int getCount() {
        return count;
    }

}

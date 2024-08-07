package net.xsapi.panat.xsitemmailsclient.objects;

public class XSRewards {

    private int idReward;
    private int count;

    public XSRewards(int idReward, int count) {

        this.count = count;
        this.idReward = idReward;

    }

    public int getCount() {
        return count;
    }

    public int getIdReward() {
        return idReward;
    }
}

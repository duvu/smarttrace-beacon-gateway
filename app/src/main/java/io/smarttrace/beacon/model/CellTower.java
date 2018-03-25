package io.smarttrace.beacon.model;

/**
 * Created by beou on 3/8/18.
 */

public class CellTower {
    int mcc;
    int mnc;
    int lac;
    int cid;
    int rxlev;

    public CellTower() {
    }

    public int getMcc() {
        return mcc;
    }

    public void setMcc(int mcc) {
        this.mcc = mcc;
    }

    public int getMnc() {
        return mnc;
    }

    public void setMnc(int mnc) {
        this.mnc = mnc;
    }

    public int getLac() {
        return lac;
    }

    public void setLac(int lac) {
        this.lac = lac;
    }

    public int getCid() {
        return cid;
    }

    public void setCid(int cid) {
        this.cid = cid;
    }

    public int getRxlev() {
        return rxlev;
    }

    public void setRxlev(int rxlev) {
        this.rxlev = rxlev;
    }
}

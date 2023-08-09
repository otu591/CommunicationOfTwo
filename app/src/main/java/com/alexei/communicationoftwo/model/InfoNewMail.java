package com.alexei.communicationoftwo.model;

public class InfoNewMail {
    private boolean arrived=false;
    private int count=0;
    private long lastDate=0;

    public InfoNewMail() {
    }


    public boolean isArrived() {
        return arrived;
    }

    public void setArrived(boolean arrived) {
        if(!arrived){
            count=0;
            lastDate=0;
        }else {
            count++;
            lastDate=System.currentTimeMillis();
        }
        this.arrived = arrived;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getLastDate() {
        return lastDate;
    }

    public void setLastDate(long lastDate) {
        this.lastDate = lastDate;
    }
}

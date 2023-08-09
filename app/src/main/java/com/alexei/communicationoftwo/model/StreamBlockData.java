package com.alexei.communicationoftwo.model;

import java.io.Serializable;

public class StreamBlockData implements Serializable {
    private String IP;
    private long access;
    private String name;

    public StreamBlockData(String IP, long access, String name) {
        this.IP = IP;
        this.access = access;
        this.name = name;
    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public long getAccess() {
        return access;
    }

    public void setAccess(long access) {
        this.access = access;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

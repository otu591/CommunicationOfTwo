package com.alexei.communicationoftwo.model;

import java.io.Serializable;

public class InfoAcceptPacketResponse implements Serializable {

   private String key;

    public InfoAcceptPacketResponse(String key) {
        this.key = key;
    }

}

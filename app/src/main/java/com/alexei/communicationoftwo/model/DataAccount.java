package com.alexei.communicationoftwo.model;


public class DataAccount {

    private String pathImg;
    private String name;
    private String email;

    public DataAccount(String pathImg, String name, String email) {

        this.pathImg = pathImg;
        this.name = name;
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPathImg() {
        return pathImg;
    }

    public void setPathImg(String pathImg) {
        this.pathImg = pathImg;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

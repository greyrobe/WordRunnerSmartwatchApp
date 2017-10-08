package com.apps.greyrobe.wordrunner;

import java.io.Serializable;

/**
 * Created by James on 10/5/2017.
 */

public class Book implements Serializable{
    private String id;
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

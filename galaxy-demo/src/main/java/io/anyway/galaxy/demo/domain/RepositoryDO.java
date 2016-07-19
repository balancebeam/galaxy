package io.anyway.galaxy.demo.domain;

import java.io.Serializable;

/**
 * Created by yangzz on 16/7/19.
 */
public class RepositoryDO implements Serializable{

    private int id;

    private String name;

    private long number;

    public RepositoryDO(int id, String name, long number) {
        this.id = id;
        this.name = name;
        this.number = number;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getNumber() {
        return number;
    }

    public void setNumber(long number) {
        this.number = number;
    }

}

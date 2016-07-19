package io.anyway.galaxy.demo.domain;

import java.io.Serializable;

/**
 * Created by yangzz on 16/7/19.
 */
public class OrderDO implements Serializable {

    private int id;

    private String name;

    private String status;

    public OrderDO(int id, String name, String status) {
        this.id = id;
        this.name = name;
        this.status = status;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

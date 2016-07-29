package io.anyway.galaxy.demo.domain;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Created by yangzz on 16/7/19.
 */
@Setter
@Getter
public class RepositoryDO implements Serializable{

    private long id;

    private String name;

    private long stock;

    private long unit_price;

    public RepositoryDO(long id, long stock) {
        this.id = id;
        this.stock = stock;
    }

    public RepositoryDO(long id, String name, long stock, long unit_price) {
        this.id = id;
        this.name = name;
        this.stock = stock;
        this.unit_price = unit_price;
    }

}

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

    private long number;

    private long unit_price;

    public RepositoryDO(long id, long number) {
        this.id = id;
        this.number = number;
    }

    public RepositoryDO(long id, String name, long number, long unit_price) {
        this.id = id;
        this.name = name;
        this.number = number;
        this.unit_price = unit_price;
    }

}

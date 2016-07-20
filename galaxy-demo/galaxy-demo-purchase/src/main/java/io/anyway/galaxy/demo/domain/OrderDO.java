package io.anyway.galaxy.demo.domain;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Created by yangzz on 16/7/19.
 */
@Getter
@Setter
public class OrderDO implements Serializable {

    private long order_id;

    private long item_id;

    private long user_id;

    private String status;

    private long amout;

    public OrderDO(long order_id, long item_id, long user_id, String status, long amout) {
        this.order_id = order_id;
        this.item_id = item_id;
        this.user_id = user_id;
        this.status = status;
        this.amout = amout;
    }
}

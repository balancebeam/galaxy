package io.anyway.galaxy.demo.service;

import io.anyway.galaxy.context.TXContext;
import io.anyway.galaxy.demo.domain.OrderDO;

/**
 * Created by yangzz on 16/7/19.
 */
public interface OrderService {

    boolean addOrder(TXContext ctx,OrderDO orderDO);
}

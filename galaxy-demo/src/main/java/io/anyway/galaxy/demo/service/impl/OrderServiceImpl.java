package io.anyway.galaxy.demo.service.impl;

import io.anyway.galaxy.annotation.TXCompensable;
import io.anyway.galaxy.demo.dao.OrderDao;
import io.anyway.galaxy.demo.domain.OrderDO;
import io.anyway.galaxy.demo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by yangzz on 16/7/19.
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderDao dao;

    @Override
    @TXCompensable(cancel = "deleteOrder")
    public boolean addOrder(OrderDO orderDO) {
        return 0< dao.addOrder(orderDO);
    }

    public boolean deleteOrder(OrderDO orderDO){
        return 0 < dao.deleteOrder(orderDO);
    }
}

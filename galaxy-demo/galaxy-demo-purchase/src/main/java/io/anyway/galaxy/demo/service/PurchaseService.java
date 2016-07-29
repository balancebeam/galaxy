package io.anyway.galaxy.demo.service;

import io.anyway.galaxy.context.SerialNumberGenerator;

/**
 * Created by yangzz on 16/7/19.
 */
public interface PurchaseService {
    /**
     *购买操作
     */
    String purchase(SerialNumberGenerator scenario, long userId, long repositoryId, long number)throws Exception;
}

package io.anyway.galaxy.demo.service;

/**
 * Created by yangzz on 16/7/19.
 */
public interface PurchaseService {
    /**
     *购买操作
     */
    String purchase(long userId, long repositoryId, long number);
}

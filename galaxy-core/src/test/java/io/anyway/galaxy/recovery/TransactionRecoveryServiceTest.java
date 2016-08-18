package io.anyway.galaxy.recovery;

import common.DalTestCase;
import io.anyway.galaxy.common.TransactionStatusEnum;
import io.anyway.galaxy.message.TransactionMessageService;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by xiong.j on 2016/8/18.
 */
public class TransactionRecoveryServiceTest extends DalTestCase{

    @InjectMocks
    @Autowired
    private TransactionRecoveryService transactionRecoveryService;

    @Test
    public void fetchData() throws Exception {
        List<Integer> shardingItems = new ArrayList<Integer>(4);
        shardingItems.add(TransactionStatusEnum.BEGIN.getCode());
        shardingItems.add(TransactionStatusEnum.TRIED.getCode());
        shardingItems.add(TransactionStatusEnum.CANCELLING.getCode());
        shardingItems.add(TransactionStatusEnum.CONFIRMING.getCode());
        transactionRecoveryService.fetchData(shardingItems);
    }

    /*@Test
    public void execute() throws Exception {

    }*/

}
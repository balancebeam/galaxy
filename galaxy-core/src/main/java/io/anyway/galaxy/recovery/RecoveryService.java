package io.anyway.galaxy.recovery;

import java.util.List;

/**
 * Created by xiong.j on 2016/7/21.
 */
public interface RecoveryService {

    /**
     * 分布式事务恢复服务
     *
     * @param shardingItem 分片(一个分片对应一个状态)
     */
    public void execute(List<Integer> shardingItem);
}

package io.anyway.galaxy.repository.impl;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.anyway.galaxy.domain.TransactionInfo;
import io.anyway.galaxy.repository.TransactionRepository;

import java.sql.Connection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by xiong.j on 2016/7/21.
 */
public abstract class CacheableTransactionRepository implements TransactionRepository {

    private int expireDuration = 300;

    private Cache<Long, TransactionInfo> transactionInfoCache;

    @Override
    public int create(Connection conn, TransactionInfo transactionInfo) {
        int result = doCreate(conn, transactionInfo);
        if (result > 0) {
            putToCache(transactionInfo);
        }
        return result;
    }

    @Override
    public int update(Connection conn, TransactionInfo transactionInfo) {
        int result = doUpdate(conn, transactionInfo);
        if (result > 0) {
            putToCache(transactionInfo);
        } else {
            throw new ConcurrentModificationException();
        }
        return result;
    }

    @Override
    public int delete(Connection conn, TransactionInfo transactionInfo) {
        int result = doDelete(conn, transactionInfo);
        if (result > 0) {
            removeFromCache(transactionInfo);
        }
        return result;
    }

    @Override
    public TransactionInfo findById(Connection conn, long txId) {
        TransactionInfo transactionInfo = findFromCache(txId);

        if (transactionInfo == null) {
            transactionInfo = doFindById(conn, txId);

            if (transactionInfo != null) {
                putToCache(transactionInfo);
            }
        }

        return transactionInfo;
    }

    @Override
    public TransactionInfo directFindById(Connection conn, long txId) {
        TransactionInfo transactionInfo = doFindById(conn, txId);

        if (transactionInfo != null) {
            putToCache(transactionInfo);
        }

        return transactionInfo;
    }

    @Override
    public List<TransactionInfo> findSince(Connection conn, java.sql.Date date, int txStatus) {

        List<TransactionInfo> transactionInfos = doFindSince(conn, date, txStatus);

        for (TransactionInfo transactionInfo : transactionInfos) {
            putToCache(transactionInfo);
        }

        return transactionInfos;
    }

    public CacheableTransactionRepository() {
        transactionInfoCache = CacheBuilder.newBuilder().expireAfterAccess(expireDuration, TimeUnit.SECONDS).maximumSize(1000).build();
    }

    protected void putToCache(TransactionInfo transactionInfo) {
        transactionInfoCache.put(transactionInfo.getTxId(), transactionInfo);
    }

    protected void removeFromCache(TransactionInfo transactionInfo) {
        transactionInfoCache.invalidate(transactionInfo.getTxId());
    }

    protected TransactionInfo findFromCache(long txId) {
        return transactionInfoCache.getIfPresent(txId);
    }

    public final void setExpireDuration(int durationInSeconds) {
        this.expireDuration = durationInSeconds;
    }

    protected abstract int doCreate(Connection conn, TransactionInfo transactionInfo);

    protected abstract int doUpdate(Connection conn, TransactionInfo transactionInfo);

    protected abstract int doDelete(Connection conn, TransactionInfo transactionInfo);

    protected abstract TransactionInfo doFindById(Connection conn, long txId);

    protected abstract List<TransactionInfo> doFindSince(Connection conn, java.sql.Date date, int txStatus);
}

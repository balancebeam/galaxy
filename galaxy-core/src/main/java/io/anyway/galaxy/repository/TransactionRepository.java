package io.anyway.galaxy.repository;

import io.anyway.galaxy.domain.TransactionInfo;

import java.sql.Connection;
import java.util.List;

/**
 * Created by changmingxie on 11/12/15.
 */
public interface TransactionRepository {

    int create(Connection conn, TransactionInfo transactionInfo);

    int update(Connection conn, TransactionInfo transactionInfo);

    int delete(Connection conn, TransactionInfo transactionInfo);

    TransactionInfo findById(Connection conn, long txId);

    TransactionInfo directFindById(Connection conn, long txId);

    TransactionInfo lockById(Connection conn, long txId);

    List<TransactionInfo> findSince(Connection conn, java.sql.Date date, int txStatus);

    List<TransactionInfo> findSince(Connection conn, java.sql.Date date, Integer[] txStatus);
    
    List<TransactionInfo> listSince(Connection conn, java.sql.Date date);
}

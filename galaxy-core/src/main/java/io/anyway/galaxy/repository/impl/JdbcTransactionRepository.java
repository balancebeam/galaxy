package io.anyway.galaxy.repository.impl;


import io.anyway.galaxy.domain.TransactionInfo;
import io.anyway.galaxy.exception.DistributedTransactionException;
import org.apache.commons.collections4.CollectionUtils;

import javax.sql.DataSource;
import javax.transaction.xa.Xid;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by xiong.j on 2016/7/21.
 */
public class JdbcTransactionRepository extends CachableTransactionRepository {

    private static final String PG_DATE_SQL = "current_timestamp(0)::timestamp without time zone";

    private DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    protected int doCreate(TransactionInfo transactionInfo) {

        Connection connection = null;
        PreparedStatement stmt = null;

        try {
            connection = this.getConnection();

            StringBuilder builder = new StringBuilder();
            builder.append("INSERT INTO TRANSACTION_INFO " +
                    "(TX_ID, PARENT_ID, BUSINESS_ID, BUSINESS_TYPE, TX_TYPE" +
                    ", TX_STATUS, CONTEXT, PAYLOAD, RETRIED_COUNT, GMT_CREATE" +
                    ", GMT_MODIFIED)" +
                    " VALUES(?,?,?,?,?" +
                    ",?,?,?,?," + PG_DATE_SQL +
                    ", " + PG_DATE_SQL + ")");

            stmt = connection.prepareStatement(builder.toString());

            stmt.setLong(1, transactionInfo.getTxId());
            stmt.setLong(2, transactionInfo.getParentId());
            stmt.setLong(3, transactionInfo.getBusinessId());
            stmt.setString(4, transactionInfo.getBusinessType());
            stmt.setInt(5, transactionInfo.getTxType());
            stmt.setInt(6, transactionInfo.getTxStatus());
            stmt.setString(7, transactionInfo.getContext());
            stmt.setString(8, transactionInfo.getPayload());
            stmt.setInt(9, transactionInfo.getRetried_count());

            return stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DistributedTransactionException(e);
        } finally {
            closeStatement(stmt);
            this.releaseConnection(connection);
        }
    }

    protected int doUpdate(TransactionInfo transactionInfo) {
        Connection connection = null;
        PreparedStatement stmt = null;

        try {
            connection = this.getConnection();

            StringBuilder builder = new StringBuilder();
            builder.append("UPDATE TRANSACTION_INFO SET " +
                    "TX_TYPE = ?, TX_STATUS = ?, CONTEXT = ?, PAYLOAD = ?, RETRIED_COUNT = ?" +
                    ", GMT_MODIFIED = " + PG_DATE_SQL);

            stmt = connection.prepareStatement(builder.toString());

            stmt.setInt(1, transactionInfo.getTxType());
            stmt.setInt(2, transactionInfo.getTxStatus());
            stmt.setString(3, transactionInfo.getContext());
            stmt.setString(4, transactionInfo.getPayload());
            stmt.setInt(5, transactionInfo.getRetried_count());

            int result = stmt.executeUpdate();

            return result;

        } catch (Throwable e) {
            throw new DistributedTransactionException(e);
        } finally {
            closeStatement(stmt);
            this.releaseConnection(connection);
        }
    }

    protected int doDelete(TransactionInfo transactionInfo) {
        Connection connection = null;
        PreparedStatement stmt = null;

        try {
            connection = this.getConnection();

            StringBuilder builder = new StringBuilder();
            builder.append("DELETE FROM TRANSACTION_INFO " +
                    " WHERE TX_ID = ?");

            stmt = connection.prepareStatement(builder.toString());

            stmt.setLong(1, transactionInfo.getTxId());

            return stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DistributedTransactionException(e);
        } finally {
            closeStatement(stmt);
            this.releaseConnection(connection);
        }
    }

    @Override
    protected List<TransactionInfo> doFindSince(Date date, int txStatus) {

        List<TransactionInfo> transactionInfos = new ArrayList<TransactionInfo>();

        Connection connection = null;
        PreparedStatement stmt = null;

        try {
            connection = this.getConnection();

            StringBuilder builder = new StringBuilder();
            builder.append("SELECT TX_ID, PARENT_ID, BUSINESS_ID, BUSINESS_TYPE, TX_TYPE, TX_STATUS, CONTEXT, PAYLOAD, RETRIED_COUNT, GMT_CREATE, GMT_MODIFIED" +
                    "  FROM TCC_TRANSACTION WHERE GMT_MODIFIED < ? AND TX_STATUS = ?");

            stmt = connection.prepareStatement(builder.toString());

            stmt.setDate(1, date);

            ResultSet resultSet = stmt.executeQuery();

            while (resultSet.next()) {
                transactionInfos.add(resultSet2Bean(resultSet));
            }
        } catch (Throwable e) {
            throw new DistributedTransactionException(e);
        } finally {
            closeStatement(stmt);
            this.releaseConnection(connection);
        }

        return transactionInfos;
    }

    protected TransactionInfo doFindById(long txId) {

        TransactionInfo transactionInfo = null;

        Connection connection = null;
        PreparedStatement stmt = null;

        try {
            connection = this.getConnection();

            StringBuilder builder = new StringBuilder();
            builder.append("SELECT TX_ID, PARENT_ID, BUSINESS_ID, BUSINESS_TYPE, TX_TYPE, TX_STATUS, CONTEXT, PAYLOAD, RETRIED_COUNT, GMT_CREATE, GMT_MODIFIED" +
                    "  FROM TCC_TRANSACTION WHERE TX_ID = ? OR PARENT_ID = ?");

            stmt.setLong(1, txId);
            stmt.setLong(2, txId);

            stmt = connection.prepareStatement(builder.toString());

            ResultSet resultSet = stmt.executeQuery();

            while (resultSet.next()) {
                transactionInfo = resultSet2Bean(resultSet);
            }
        } catch (Throwable e) {
            throw new DistributedTransactionException(e);
        } finally {
            closeStatement(stmt);
            this.releaseConnection(connection);
        }

        return transactionInfo;
    }

    private TransactionInfo resultSet2Bean(ResultSet resultSet) throws Throwable{
        TransactionInfo transactionInfo = new TransactionInfo();

        transactionInfo.setTxId(resultSet.getLong(1));
        transactionInfo.setParentId(resultSet.getLong(2));
        transactionInfo.setBusinessId(resultSet.getLong(3));
        transactionInfo.setBusinessType(resultSet.getString(4));
        transactionInfo.setTxType(resultSet.getInt(5));
        transactionInfo.setTxStatus(resultSet.getInt(6));
        transactionInfo.setContext(resultSet.getString(7));
        transactionInfo.setPayload(resultSet.getString(8));
        transactionInfo.setRetried_count(resultSet.getInt(9));
        transactionInfo.setGmtCreated(resultSet.getDate(10));
        transactionInfo.setGmtModified(resultSet.getDate(11));

        return transactionInfo;
    }

    protected Connection getConnection() {
        try {
            return this.dataSource.getConnection();
        } catch (SQLException e) {
            throw new DistributedTransactionException(e);
        }
    }

    protected void releaseConnection(Connection con) {
        try {
            if (con != null && !con.isClosed()) {
                con.close();
            }
        } catch (SQLException e) {
            throw new DistributedTransactionException(e);
        }
    }

    private void closeStatement(Statement stmt) {
        try {
            if (stmt != null && !stmt.isClosed()) {
                stmt.close();
            }
        } catch (Exception ex) {
            throw new DistributedTransactionException(ex);
        }
    }
}

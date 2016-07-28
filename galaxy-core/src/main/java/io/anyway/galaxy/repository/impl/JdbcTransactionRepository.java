package io.anyway.galaxy.repository.impl;

import com.google.common.base.Strings;
import io.anyway.galaxy.common.Constants;
import io.anyway.galaxy.domain.TransactionInfo;
import io.anyway.galaxy.exception.DistributedTransactionException;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xiong.j on 2016/7/21.
 */
@Repository
public class JdbcTransactionRepository extends CacheableTransactionRepository {

	private static final String PG_DATE_SQL = "current_timestamp(0)::timestamp without time zone";

	private static final String ORACLE_DATE_SQL = "sysdate";

	protected int doCreate(Connection conn, TransactionInfo transactionInfo) {

		PreparedStatement stmt = null;

		try {

			StringBuilder builder = new StringBuilder();
			builder.append("INSERT INTO TRANSACTION_INFO " + "(TX_ID, PARENT_ID, BUSINESS_ID, BUSINESS_TYPE, TX_TYPE"
					+ ", TX_STATUS, CONTEXT, PAYLOAD, RETRIED_COUNT, GMT_CREATE" + ", GMT_MODIFIED)"
					+ " VALUES(?,?,?,?,?" + ",?,?,?,?," + getDateSql(conn) + ", " + getDateSql(conn) + ")");

			stmt = conn.prepareStatement(builder.toString());

			stmt.setLong(1, transactionInfo.getTxId());
			stmt.setLong(2, transactionInfo.getParentId());
			stmt.setString(3, transactionInfo.getSerialNumber());
			stmt.setString(4, transactionInfo.getBusinessType());
			stmt.setInt(5, transactionInfo.getTxType());
			stmt.setInt(6, transactionInfo.getTxStatus());
			stmt.setString(7, transactionInfo.getContext());
			stmt.setString(8, transactionInfo.getPayload());
			stmt.setInt(9, transactionInfo.getRetried_count());

			return stmt.executeUpdate();

		} catch (Throwable e) {
			throw new DistributedTransactionException(e);
		} finally {
			closeStatement(stmt);
			// this.releaseConnection(conn);
		}
	}

	protected int doUpdate(Connection conn, TransactionInfo transactionInfo) {
		PreparedStatement stmt = null;

		try {

			StringBuilder builder = new StringBuilder();
			builder.append("UPDATE TRANSACTION_INFO SET ");
			if (transactionInfo.getTxType() != -1) {
				builder.append("TX_TYPE = ?, ");
			}
			if (transactionInfo.getTxStatus() != -1) {
				builder.append("TX_STATUS = ?, ");
			}
			if (!Strings.isNullOrEmpty(transactionInfo.getContext())) {
				builder.append("CONTEXT = ?, ");
			}
			if (!Strings.isNullOrEmpty(transactionInfo.getPayload())) {
				builder.append("PAYLOAD = ?, ");
			}
			if (transactionInfo.getRetried_count() != -1) {
				builder.append("RETRIED_COUNT = ?, ");
			}
			builder.append("GMT_MODIFIED = " + getDateSql(conn) + " WHERE TX_ID = ?");

			stmt = conn.prepareStatement(builder.toString());

			int condition = 0;

			if (transactionInfo.getTxType() != -1) {
				stmt.setInt(++condition, transactionInfo.getTxType());
			}
			if (transactionInfo.getTxStatus() != -1) {
				stmt.setInt(++condition, transactionInfo.getTxStatus());
			}
			if (!Strings.isNullOrEmpty(transactionInfo.getContext())) {
				stmt.setString(++condition, transactionInfo.getContext());
			}
			if (!Strings.isNullOrEmpty(transactionInfo.getPayload())) {
				stmt.setString(++condition, transactionInfo.getPayload());
			}
			if (transactionInfo.getRetried_count() != -1) {
				stmt.setInt(++condition, transactionInfo.getRetried_count());
			}

			stmt.setLong(++condition, transactionInfo.getTxId());

			int result = stmt.executeUpdate();

			return result;

		} catch (Throwable e) {
			throw new DistributedTransactionException(e);
		} finally {
			closeStatement(stmt);
			// this.releaseConnection(connection);
		}
	}

	protected int doDelete(Connection conn, TransactionInfo transactionInfo) {
		PreparedStatement stmt = null;

		try {

			StringBuilder builder = new StringBuilder();
			builder.append("DELETE FROM TRANSACTION_INFO " + " WHERE TX_ID = ?");

			stmt = conn.prepareStatement(builder.toString());

			stmt.setLong(1, transactionInfo.getTxId());

			return stmt.executeUpdate();

		} catch (SQLException e) {
			throw new DistributedTransactionException(e);
		} finally {
			closeStatement(stmt);
			// this.releaseConnection(connection);
		}
	}

	@Override
	protected List<TransactionInfo> doFindSince(Connection conn, Date date, int txStatus) {

		List<TransactionInfo> transactionInfos = new ArrayList<TransactionInfo>();

		PreparedStatement stmt = null;

		try {

			StringBuilder builder = new StringBuilder();
			builder.append(
					"SELECT TX_ID, PARENT_ID, BUSINESS_ID, BUSINESS_TYPE, TX_TYPE, TX_STATUS, CONTEXT, PAYLOAD, RETRIED_COUNT, GMT_CREATE, GMT_MODIFIED"
							+ "  FROM TCC_TRANSACTION WHERE GMT_MODIFIED < ? AND TX_STATUS = ?");

			stmt = conn.prepareStatement(builder.toString());

			stmt.setDate(1, date);

			ResultSet resultSet = stmt.executeQuery();

			while (resultSet.next()) {
				transactionInfos.add(resultSet2Bean(resultSet));
			}
		} catch (Throwable e) {
			throw new DistributedTransactionException(e);
		} finally {
			closeStatement(stmt);
			// this.releaseConnection(conn);
		}

		return transactionInfos;
	}

	protected TransactionInfo doFindById(Connection conn, long txId) {

		TransactionInfo transactionInfo = null;

		PreparedStatement stmt = null;

		try {

			StringBuilder builder = new StringBuilder();
			builder.append(
					"SELECT TX_ID, PARENT_ID, BUSINESS_ID, BUSINESS_TYPE, TX_TYPE, TX_STATUS, CONTEXT, PAYLOAD, RETRIED_COUNT, GMT_CREATE, GMT_MODIFIED"
							+ "  FROM TRANSACTION_INFO WHERE TX_ID = ?");

			stmt = conn.prepareStatement(builder.toString());
			stmt.setLong(1, txId);
			ResultSet resultSet = stmt.executeQuery();

			while (resultSet.next()) {
				transactionInfo = resultSet2Bean(resultSet);
			}
		} catch (Throwable e) {
			throw new DistributedTransactionException(e);
		} finally {
			closeStatement(stmt);
			// this.releaseConnection(conn);
		}

		return transactionInfo;
	}

	protected TransactionInfo doLockById(Connection conn, long txId) {

		TransactionInfo transactionInfo = null;

		PreparedStatement stmt = null;

		try {

			StringBuilder builder = new StringBuilder();
			builder.append(
					"SELECT TX_ID, PARENT_ID, BUSINESS_ID, BUSINESS_TYPE, TX_TYPE, TX_STATUS, CONTEXT, PAYLOAD, RETRIED_COUNT, GMT_CREATE, GMT_MODIFIED"
							+ "  FROM TRANSACTION_INFO WHERE TX_ID = ? FOR UPDATE NO WAIT");
			stmt = conn.prepareStatement(builder.toString());
			stmt.setLong(1, txId);

			ResultSet resultSet = stmt.executeQuery();

			while (resultSet.next()) {
				transactionInfo = resultSet2Bean(resultSet);
			}
		} catch (Throwable e) {
			throw new DistributedTransactionException(e);
		} finally {
			closeStatement(stmt);
			// this.releaseConnection(conn);
		}

		return transactionInfo;
	}

	private TransactionInfo resultSet2Bean(ResultSet resultSet) throws Throwable {
		TransactionInfo transactionInfo = new TransactionInfo();

		transactionInfo.setTxId(resultSet.getLong(1));
		transactionInfo.setParentId(resultSet.getLong(2));
		transactionInfo.setSerialNumber(resultSet.getString(3));
		transactionInfo.setBusinessType(resultSet.getString(4));
		transactionInfo.setTxType(resultSet.getInt(5));
		transactionInfo.setTxStatus(resultSet.getInt(6));
		transactionInfo.setContext(resultSet.getString(7));
		transactionInfo.setPayload(resultSet.getString(8));
		transactionInfo.setRetried_count(resultSet.getInt(9));
		transactionInfo.setGmtCreated(new Date(resultSet.getTimestamp(10).getTime()));
		transactionInfo.setGmtModified(new Date(resultSet.getTimestamp(11).getTime()));

		return transactionInfo;
	}

	protected void releaseConnection(Connection conn) {
		try {
			if (conn != null && !conn.isClosed()) {
				conn.close();
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

	private String getDateSql(Connection conn) throws Throwable{
		String databaseName = getDatabaseName(conn).toLowerCase();
		if (databaseName.equals(Constants.ORACLE.toLowerCase())) {
			return ORACLE_DATE_SQL;
		} else if (databaseName.equals(Constants.POSTGRESQL.toLowerCase())){
			return PG_DATE_SQL;
		}
		throw new Exception("Not support database : " + databaseName);
	}

	private String getDatabaseName(Connection conn) throws Throwable{
		return conn.getMetaData().getDatabaseProductName();
	}


	@Override
	public List<TransactionInfo> listSince(Connection conn, Date date) {

		List<TransactionInfo> transactionInfos = new ArrayList<TransactionInfo>();

		PreparedStatement stmt = null;

		try {

			StringBuilder builder = new StringBuilder();
			builder.append(
					"SELECT TX_ID, PARENT_ID, BUSINESS_ID, BUSINESS_TYPE, TX_TYPE, TX_STATUS, CONTEXT, PAYLOAD, RETRIED_COUNT, GMT_CREATE, GMT_MODIFIED")
					.append("  FROM TRANSACTION_INFO WHERE GMT_MODIFIED > ?");

			stmt = conn.prepareStatement(builder.toString());

			stmt.setDate(1, date);

			ResultSet resultSet = stmt.executeQuery();

			while (resultSet.next()) {
				transactionInfos.add(resultSet2Bean(resultSet));
			}
		} catch (Throwable e) {
			throw new DistributedTransactionException(e);
		} finally {
			closeStatement(stmt);
		}

		return transactionInfos;
	}
}

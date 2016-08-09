package io.anyway.galaxy.repository.impl;

import com.google.common.base.Strings;
import io.anyway.galaxy.common.Constants;
import io.anyway.galaxy.domain.TransactionInfo;
import io.anyway.galaxy.exception.DistributedTransactionException;
import io.anyway.galaxy.spring.DataSourceAdaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
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

	@Autowired
	private DataSourceAdaptor dataSourceAdaptor;

	protected int doCreate(TransactionInfo transactionInfo) {

		Connection conn= DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());

		PreparedStatement stmt = null;

		try {

			StringBuilder builder = new StringBuilder();
			builder.append("INSERT INTO TRANSACTION_INFO " + "(TX_ID, PARENT_ID, BUSINESS_ID, BUSINESS_TYPE, TX_TYPE"
					+ ", TX_STATUS, CONTEXT, PAYLOAD, RETRIED_COUNT, MODULE_ID, GMT_CREATED" + ", GMT_MODIFIED)"
					+ " VALUES(?,?,?,?,?" + ",?,?,?,?,?," + getDateSql(conn) + ", " + getDateSql(conn) + ")");

			stmt = conn.prepareStatement(builder.toString());

			stmt.setLong(1, transactionInfo.getTxId());
			stmt.setLong(2, transactionInfo.getParentId());
			stmt.setString(3, transactionInfo.getBusinessId());
			stmt.setString(4, transactionInfo.getBusinessType());
			stmt.setInt(5, transactionInfo.getTxType());
			stmt.setInt(6, transactionInfo.getTxStatus());
			stmt.setString(7, transactionInfo.getContext());
			stmt.setString(8, transactionInfo.getPayload());
			stmt.setInt(9, transactionInfo.getRetried_count());
			stmt.setString(10,transactionInfo.getModuleId());

			return stmt.executeUpdate();

		} catch (Throwable e) {
			throw new DistributedTransactionException(e);
		} finally {
			closeStatement(stmt);
			releaseConnection(conn);
		}
	}

	protected int doUpdate(TransactionInfo transactionInfo) {

		Connection conn= DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
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
			releaseConnection(conn);
		}
	}

	protected int doDelete(TransactionInfo transactionInfo) {

		Connection conn= DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());
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
			releaseConnection(conn);
		}
	}

	@Override
	protected List<TransactionInfo> doFindSince(Date date, Integer[] txStatus) {

		Connection conn= DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());

		List<TransactionInfo> transactionInfos = new ArrayList<TransactionInfo>();

		PreparedStatement stmt = null;

		try {

			StringBuilder builder = new StringBuilder();
			builder.append(
					"SELECT TX_ID, PARENT_ID, BUSINESS_ID, BUSINESS_TYPE, TX_TYPE, TX_STATUS, CONTEXT, PAYLOAD, RETRIED_COUNT, MODULE_ID, GMT_CREATED, GMT_MODIFIED"
							+ " FROM TRANSACTION_INFO WHERE GMT_CREATED > ? AND GMT_MODIFIED < " + getDateAddSql(conn, 10) + "AND TX_STATUS ");

			if (txStatus.length > 1) {
				builder.append(" IN (");
				for (int i = 0; i < txStatus.length; i++) {
					if (i == 0) {
						builder.append(txStatus[i]);
					} else {
						builder.append(",").append(txStatus[i]);
					}
				}
				builder.append(")");
			}

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
			releaseConnection(conn);
		}

		return transactionInfos;
	}

	@Override
	protected List<TransactionInfo> doFind(TransactionInfo transactionInfo) {
		Connection conn= DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());

		PreparedStatement stmt = null;

		List<TransactionInfo> transactionInfos = new ArrayList<TransactionInfo>();

		try {

			StringBuilder builder = new StringBuilder();
			builder.append(" SELECT TX_ID, PARENT_ID, BUSINESS_ID, BUSINESS_TYPE, TX_TYPE, TX_STATUS, CONTEXT, PAYLOAD, RETRIED_COUNT, MODULE_ID, GMT_CREATED, GMT_MODIFIED"
					     + " FROM TRANSACTION_INFO WHERE 1=1");
			if (transactionInfo.getTxId() > 0L) {
				builder.append("AND TX_ID = ? ");
			}
			if (transactionInfo.getParentId() > 0L) {
				builder.append("AND PARENT_ID = ? ");
			}
			if (!Strings.isNullOrEmpty(transactionInfo.getModuleId())) {
				builder.append("MODULE_ID = ? ");
			}
			if (!Strings.isNullOrEmpty(transactionInfo.getBusinessId())) {
				builder.append("BUSINESS_ID = ? ");
			}
			if (!Strings.isNullOrEmpty(transactionInfo.getBusinessType())) {
				builder.append("BUSINESS_TYPE = ? ");
			}
			if (transactionInfo.getTxType() > -1L) {
				builder.append("TX_TYPE = ? ");
			}
			if (transactionInfo.getTxStatus() > -1L) {
				builder.append("TX_STATUS = ? ");
			}
			if (transactionInfo.getGmtCreated() != null) {
				builder.append("GMT_CREATED = ? ");
			}

			stmt = conn.prepareStatement(builder.toString());

			int condition = 0;

			if (transactionInfo.getTxId() > 0L) {
				stmt.setLong(++condition, transactionInfo.getTxId());
			}
			if (transactionInfo.getParentId() > 0L) {
				stmt.setLong(++condition, transactionInfo.getParentId());
			}
			if (!Strings.isNullOrEmpty(transactionInfo.getModuleId())) {
				stmt.setString(++condition, transactionInfo.getModuleId());
			}
			if (!Strings.isNullOrEmpty(transactionInfo.getBusinessId())) {
				stmt.setString(++condition, transactionInfo.getBusinessId());
			}
			if (!Strings.isNullOrEmpty(transactionInfo.getBusinessType())) {
				stmt.setString(++condition, transactionInfo.getBusinessType());
			}
			if (transactionInfo.getTxType() > -1L) {
				stmt.setInt(++condition, transactionInfo.getTxType());
			}
			if (transactionInfo.getTxStatus() > -1L) {
				stmt.setInt(++condition, transactionInfo.getTxStatus());
			}
			if (transactionInfo.getGmtCreated() != null) {
				stmt.setDate(++condition, transactionInfo.getGmtCreated());
			}

			ResultSet resultSet = stmt.executeQuery();

			while (resultSet.next()) {
				transactionInfos.add(resultSet2Bean(resultSet));
			}

		} catch (Throwable e) {
			throw new DistributedTransactionException(e);
		} finally {
			closeStatement(stmt);
			releaseConnection(conn);
		}
		return transactionInfos;
	}

	@Override
	protected TransactionInfo doFindById(long txId) {

		Connection conn= DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());

		TransactionInfo transactionInfo = null;

		PreparedStatement stmt = null;

		try {

			StringBuilder builder = new StringBuilder();
			builder.append(
					"SELECT TX_ID, PARENT_ID, BUSINESS_ID, BUSINESS_TYPE, TX_TYPE, TX_STATUS, CONTEXT, PAYLOAD, RETRIED_COUNT, MODULE_ID, GMT_CREATED, GMT_MODIFIED"
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
			releaseConnection(conn);
		}

		return transactionInfo;
	}

	protected TransactionInfo doLockById(long txId) {

		Connection conn= DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());

		TransactionInfo transactionInfo = null;

		PreparedStatement stmt = null;

		try {

			StringBuilder builder = new StringBuilder();
			builder.append(
					"SELECT TX_ID, PARENT_ID, BUSINESS_ID, BUSINESS_TYPE, TX_TYPE, TX_STATUS, CONTEXT, PAYLOAD, RETRIED_COUNT, MODULE_ID, GMT_CREATED, GMT_MODIFIED"
							+ "  FROM TRANSACTION_INFO WHERE TX_ID = ? FOR UPDATE NOWAIT");
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
			releaseConnection(conn);
		}

		return transactionInfo;
	}

	private TransactionInfo resultSet2Bean(ResultSet resultSet) throws Throwable {
		TransactionInfo transactionInfo = new TransactionInfo();

		transactionInfo.setTxId(resultSet.getLong(1));
		transactionInfo.setParentId(resultSet.getLong(2));
		transactionInfo.setBusinessId(resultSet.getString(3));
		transactionInfo.setBusinessType(resultSet.getString(4));
		transactionInfo.setTxType(resultSet.getInt(5));
		transactionInfo.setTxStatus(resultSet.getInt(6));
		transactionInfo.setContext(resultSet.getString(7));
		transactionInfo.setPayload(resultSet.getString(8));
		transactionInfo.setRetried_count(resultSet.getInt(9));
		transactionInfo.setModuleId(resultSet.getString(10));
		transactionInfo.setGmtCreated(new Date(resultSet.getTimestamp(11).getTime()));
		transactionInfo.setGmtModified(new Date(resultSet.getTimestamp(12).getTime()));

		return transactionInfo;
	}

	protected void releaseConnection(Connection conn) {
		DataSourceUtils.releaseConnection(conn, dataSourceAdaptor.getDataSource());
	}

	private void closeStatement(Statement stmt) {
		try {
			JdbcUtils.closeStatement(stmt);
			stmt = null;
		} catch (Exception ex) {
			//throw new DistributedTransactionException(ex);
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

	private String getDateAddSql(Connection conn, int second) throws Throwable{
		String databaseName = getDatabaseName(conn).toLowerCase();
		if (databaseName.equals(Constants.ORACLE.toLowerCase())) {
			return "(sysdate - " + second + "/(24*60*60))";
		} else if (databaseName.equals(Constants.POSTGRESQL.toLowerCase())){
			return "(now() - interval '"+ second +"sec')";
		}
		throw new Exception("Not support database : " + databaseName);
	}

	private String getDatabaseName(Connection conn) throws Throwable{
		return conn.getMetaData().getDatabaseProductName();
	}


	@Override
	public List<TransactionInfo> listSince(Date date) {

		Connection conn= DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());

		List<TransactionInfo> transactionInfos = new ArrayList<TransactionInfo>();

		PreparedStatement stmt = null;

		try {

			StringBuilder builder = new StringBuilder();
			builder.append(
					"SELECT TX_ID, PARENT_ID, BUSINESS_ID, BUSINESS_TYPE, TX_TYPE, TX_STATUS, CONTEXT, PAYLOAD, RETRIED_COUNT, MODULE_ID, GMT_CREATED, GMT_MODIFIED")
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
			releaseConnection(conn);
		}

		return transactionInfos;
	}
}

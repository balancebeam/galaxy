package io.anyway.galaxy.repository.impl;

import com.google.common.base.Strings;
import io.anyway.galaxy.common.Constants;
import io.anyway.galaxy.common.TransactionStatusEnum;
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

	private static final String SELECT_DQL = "SELECT TX_ID, PARENT_ID, MODULE_ID, BUSINESS_ID, BUSINESS_TYPE, TX_TYPE, TX_STATUS, CONTEXT, RETRIED_COUNT, NEXT_RETRY_TIME, GMT_CREATED, GMT_MODIFIED";

	@Autowired
	private DataSourceAdaptor dataSourceAdaptor;

	protected int doCreate(TransactionInfo transactionInfo) throws SQLException  {

		Connection conn= DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());

		PreparedStatement stmt = null;

		try {

			StringBuilder builder = new StringBuilder();
			builder.append("INSERT INTO TRANSACTION_INFO " + "(TX_ID, PARENT_ID, BUSINESS_ID, BUSINESS_TYPE, TX_TYPE"
					+ ", TX_STATUS, CONTEXT, RETRIED_COUNT, MODULE_ID, GMT_CREATED" + ", GMT_MODIFIED)"
					+ " VALUES(?,?,?,?,?" + ",?,?,?,?,?," + getDateSql(conn) + ", " + getDateSql(conn) + ")");

			stmt = conn.prepareStatement(builder.toString());

			stmt.setLong(1, transactionInfo.getTxId());
			stmt.setLong(2, transactionInfo.getParentId());
			stmt.setString(3, transactionInfo.getBusinessId());
			stmt.setString(4, transactionInfo.getBusinessType());
			stmt.setInt(5, transactionInfo.getTxType());
			stmt.setInt(6, transactionInfo.getTxStatus());
			stmt.setString(7, transactionInfo.getContext());
			stmt.setInt(8, transactionInfo.getRetried_count());
			stmt.setString(9,transactionInfo.getModuleId());

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
			if (transactionInfo.getNextRetryTime() != null) {
				builder.append("NEXT_RETRY_TIME = ?, ");
			}
			if (transactionInfo.getRetried_count() != -1) {
				builder.append("RETRIED_COUNT = ?, ");
			}

			builder.append("GMT_MODIFIED = " + getDateSql(conn));
			builder.append(" WHERE TX_ID = ? ");
			if (transactionInfo.getParentId() > -1L) {
				builder.append(" AND PARENT_ID = ? ");
			}

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
			if (transactionInfo.getNextRetryTime() != null) {
				stmt.setDate(++condition, transactionInfo.getNextRetryTime());
			}
			if (transactionInfo.getRetried_count() != -1) {
				stmt.setInt(++condition, transactionInfo.getRetried_count());
			}

			stmt.setLong(++condition, transactionInfo.getTxId());

			if (transactionInfo.getParentId() > -1L) {
				stmt.setLong(++condition, transactionInfo.getParentId());
			}
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
			builder.append(SELECT_DQL + " FROM TRANSACTION_INFO WHERE GMT_CREATED > ? AND ")
					.append("(CASE WHEN NEXT_RETRY_TIME IS NOT NULL THEN NEXT_RETRY_TIME ELSE GMT_MODIFIED END) < ")
					.append(getDateSubtSecSql(conn, 10)).append(" AND TX_STATUS ")
					.append(getLimitSql(conn, 1000));

			if (txStatus.length > 0) {
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
	protected List<TransactionInfo> doFind(TransactionInfo transactionInfo, boolean isLock) {
		Connection conn= DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());

		PreparedStatement stmt = null;

		List<TransactionInfo> transactionInfos = new ArrayList<TransactionInfo>();

		try {

			StringBuilder builder = new StringBuilder();
			builder.append(SELECT_DQL + " FROM TRANSACTION_INFO WHERE 1=1");
			if (transactionInfo.getTxId() > 0L) {
				builder.append("AND TX_ID = ? ");
			}
			if (transactionInfo.getParentId() > 0L) {
				builder.append("AND PARENT_ID = ? ");
			}
			if (!Strings.isNullOrEmpty(transactionInfo.getModuleId())) {
				builder.append("AND MODULE_ID = ? ");
			}
			if (!Strings.isNullOrEmpty(transactionInfo.getBusinessId())) {
				builder.append("AND BUSINESS_ID = ? ");
			}
			if (!Strings.isNullOrEmpty(transactionInfo.getBusinessType())) {
				builder.append("AND BUSINESS_TYPE = ? ");
			}
			if (transactionInfo.getTxType() > -1L) {
				builder.append("AND TX_TYPE = ? ");
			}
			if (transactionInfo.getTxStatus() > -1L) {
				builder.append("AND TX_STATUS = ? ");
			}
			if (transactionInfo.getGmtCreated() != null) {
				builder.append("AND GMT_CREATED = ? ");
			}
			if (isLock) {
				builder.append(" FOR UPDATE NOWAIT");
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
			builder.append(SELECT_DQL + "  FROM TRANSACTION_INFO WHERE TX_ID = ?");

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

	protected List<TransactionInfo> doLockByModules(long parentId, List<String> modules) {

		Connection conn= DataSourceUtils.getConnection(dataSourceAdaptor.getDataSource());

		List<TransactionInfo> transactionInfos = new ArrayList<TransactionInfo>();

		PreparedStatement stmt = null;

		try {
			StringBuilder builder = new StringBuilder();
			builder.append(SELECT_DQL + "  FROM TRANSACTION_INFO WHERE PARENT_ID = ? AND TX_ID <> 0 AND MODULE_ID ");

			if (modules.size() > 0) {
				builder.append(" IN (");
				for (int i = 0; i < modules.size(); i++) {
					if (i == 0) {
						builder.append(modules.get(i));
					} else {
						builder.append(",").append(modules.get(i));
					}
				}
				builder.append(")");
			}
			builder.append(" TX_STATUS NOT IN(").append(TransactionStatusEnum.CANCELLED).append(", ")
					.append(TransactionStatusEnum.CONFIRMED)
					.append(")");
			builder.append(" FOR UPDATE NOWAIT");

			stmt = conn.prepareStatement(builder.toString());
			stmt.setLong(1, parentId);

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

	private TransactionInfo resultSet2Bean(ResultSet resultSet) throws Throwable {
		TransactionInfo transactionInfo = new TransactionInfo();

		transactionInfo.setTxId(resultSet.getLong(1));
		transactionInfo.setParentId(resultSet.getLong(2));
		transactionInfo.setModuleId(resultSet.getString(3));
		transactionInfo.setBusinessId(resultSet.getString(4));
		transactionInfo.setBusinessType(resultSet.getString(5));
		transactionInfo.setTxType(resultSet.getInt(6));
		transactionInfo.setTxStatus(resultSet.getInt(7));
		transactionInfo.setContext(resultSet.getString(8));
		transactionInfo.setRetried_count(resultSet.getInt(9));
		transactionInfo.setNextRetryTime(new Date(resultSet.getTimestamp(10).getTime()));
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

	private String getDateSubtSecSql(Connection conn, int second) throws Throwable{
		String databaseName = getDatabaseName(conn).toLowerCase();
		if (databaseName.equals(Constants.ORACLE.toLowerCase())) {
			return "(sysdate - " + second + "/(24*60*60))";
		} else if (databaseName.equals(Constants.POSTGRESQL.toLowerCase())){
			return "(now() - interval '"+ second +"sec')";
		}
		throw new Exception("Not support database : " + databaseName);
	}

	private String getLimitSql(Connection conn, int num) throws Throwable{
		String databaseName = getDatabaseName(conn).toLowerCase();
		if (databaseName.equals(Constants.ORACLE.toLowerCase())) {
			return " LIMIT " + num;
		} else if (databaseName.equals(Constants.POSTGRESQL.toLowerCase())){
			return " ROWNUM <= " + num;
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
			builder.append(SELECT_DQL + " FROM TRANSACTION_INFO WHERE GMT_MODIFIED > ?");

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

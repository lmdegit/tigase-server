/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author:$
 * $Date$
 *
 */
package tigase.server.xmppclient;

//~--- non-JDK imports --------------------------------------------------------

import tigase.component.exceptions.RepositoryException;
import tigase.db.*;
import tigase.db.beans.SDRepositoryBean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.util.TigaseStringprepException;
import tigase.xmpp.BareJID;

//~--- JDK imports ------------------------------------------------------------

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Extended implementation of SeeOtherHost using redirect information from
 * database
 * 
 */
public class SeeOtherHostDB extends SeeOtherHostHashed implements RegistrarBean {

	private static final Logger log = Logger.getLogger(SeeOtherHostDB.class.getName());

	public static final String SEE_OTHER_HOST_TABLE = "tig_see_other_hosts";
	public static final String SEE_OTHER_HOST_DB_URL_KEY = CM_SEE_OTHER_HOST_CLASS_PROP_KEY
			+ "/" + "db-url";
	public static final String SEE_OTHER_HOST_DB_QUERY_KEY =
			CM_SEE_OTHER_HOST_CLASS_PROP_KEY + "/" + "get-host-query";
	public static final String DB_GET_ALL_DATA_DB_QUERY_KEY =
			CM_SEE_OTHER_HOST_CLASS_PROP_KEY + "/" + "get-all-data-query";
	public static final String GET_ALL_QUERY_TIMEOUT_QUERY_KEY =
		CM_SEE_OTHER_HOST_CLASS_PROP_KEY + "/" + "get-all-query-timeout";

	
	public static final String SERIAL_ID = "sid";
	public static final String USER_ID = "uid";
	public static final String NODE_ID = "node_id";
	
	@Inject
	private SeeOtherHostRepository repo;

	// Methods

	@Override
	public BareJID findHostForJID(BareJID jid, BareJID host) {

		BareJID see_other_host = repo.getHostFor(jid);

		if (see_other_host != null && !isNodeShutdown(see_other_host)) {
			return see_other_host;
//		} else {
//			see_other_host = host;
		}

		try {
			see_other_host = repo.queryDBFor(jid);
		} catch (Exception ex) {
			log.log(Level.SEVERE, "DB lookup failed, fallback to SeeOtherHostHashed: ", ex);
		}
		
		if (see_other_host == null || isNodeShutdown(see_other_host)) {
			log.log(Level.FINE, "DB lookup failed or selected node is being stopped, fallback to SeeOtherHostHashed for {0}", jid);
			see_other_host = super.findHostForJID(jid, host);
		}

		return see_other_host;
	}

	@Override
	public void register(Kernel kernel) {

	}

	@Override
	public void unregister(Kernel kernel) {

	}
	
	/**
	 * Performs database check, creates missing schema if necessary
	 *
	 * @throws SQLException
	 */
	private void checkDB() throws SQLException {

	}

	public interface SeeOtherHostRepository<DS extends DataSource> extends DataSourceAware<DS> {

		BareJID getHostFor(BareJID jid);

		BareJID queryDBFor(BareJID jid) throws UserNotFoundException, SQLException, TigaseStringprepException;

	}

	public static class SeeOtherHostDBSDRepositoryBean extends SDRepositoryBean<SeeOtherHostRepository> {

		@Override
		protected Class<?> findClassForDataSource(DataSource dataSource) throws DBInitException {
			return DataSourceHelper.getDefaultClass(SeeOtherHostRepository.class, dataSource.getResourceUri());
		}
	}

	@Repository.Meta(supportedUris = {"jdbc:[^:]+:.*"})
	public static class JDBCSeeOtherHostRepository implements SeeOtherHostRepository<DataRepository> {

		public static final String DEF_DB_GET_HOST_QUERY = " select * from tig_users, "
				+ SEE_OTHER_HOST_TABLE + " where tig_users.uid = " + SEE_OTHER_HOST_TABLE + "."
				+ USER_ID + " and user_id = ?";

		private static final String DEF_DB_GET_ALL_DATA_QUERY =
				"select user_id, node_id from tig_users, " + SEE_OTHER_HOST_TABLE
						+ " where tig_users.uid = " + SEE_OTHER_HOST_TABLE + "." + USER_ID;

		private static final String CREATE_STATS_TABLE = "create table " + SEE_OTHER_HOST_TABLE
				+ " ( " + SERIAL_ID + " serial," + USER_ID + " bigint unsigned NOT NULL, "
				+ NODE_ID + " varchar(2049) NOT NULL, " + " primary key (" + SERIAL_ID + "), "
				+ " constraint tig_see_other_host_constr foreign key (" + USER_ID
				+ ") references tig_users (" + USER_ID + ")" + ")";

		private static final String DERBY_CREATE_STATS_TABLE = "create table " + SEE_OTHER_HOST_TABLE
				+ " ( " + SERIAL_ID + " bigint generated by default as identity not null," + USER_ID + " bigint  NOT NULL, "
				+ NODE_ID + " varchar(2049) NOT NULL, " + " primary key (" + SERIAL_ID + "), "
				+ " constraint tig_see_other_host_constr foreign key (" + USER_ID
				+ ") references tig_users (" + USER_ID + ")" + ")";

		private static final String SQLSERVER_CREATE_STATS_TABLE = "create table " + SEE_OTHER_HOST_TABLE
				+ " ( " + SERIAL_ID + " [bigint] IDENTITY(1,1)," + USER_ID + " bigint NOT NULL, "
				+ NODE_ID + " nvarchar(2049) NOT NULL, " + " primary key (" + SERIAL_ID + "), "
				+ " constraint tig_see_other_host_constr foreign key (" + USER_ID
				+ ") references tig_users (" + USER_ID + ")" + ")";


		private static final int DEF_QUERY_TIME_OUT = 0;

		@ConfigField(desc = "Query to find host for JID", alias = "get-host-query")
		private String get_host_query = DEF_DB_GET_HOST_QUERY;
		@ConfigField(desc = "Query to load mapping data", alias = "get-all-data-query")
		private String get_all_data_query = DEF_DB_GET_ALL_DATA_QUERY;

		private DataRepository data_repo;

		private Map<BareJID, BareJID> redirectsMap =
				new ConcurrentSkipListMap<BareJID, BareJID>();

		@Override
		public BareJID getHostFor(BareJID jid) {
			return redirectsMap.get(jid);
		}

		@Override
		public BareJID queryDBFor(BareJID user) throws UserNotFoundException, SQLException,
													 TigaseStringprepException {

			PreparedStatement get_host = data_repo.getPreparedStatement(user, get_host_query);

			ResultSet rs = null;

			synchronized (get_host) {
				try {
					get_host.setString(1, user.toString());

					rs = get_host.executeQuery();

					if (rs.next()) {
						return BareJID.bareJIDInstance(rs.getString(NODE_ID));
					} else {
						throw new UserNotFoundException("Item does not exist for user: " + user);
					} // end of if (isnext) else
				} finally {
					data_repo.release(null, rs);
				}
			}
		}

		@Override
		public void setDataSource(DataRepository data_repo) throws RepositoryException {
			try {
				DataRepository.dbTypes databaseType = data_repo.getDatabaseType();
				switch (databaseType) {
					case derby:
						data_repo.checkTable(SEE_OTHER_HOST_TABLE, DERBY_CREATE_STATS_TABLE);
						break;
					case jtds:
					case sqlserver:
						data_repo.checkTable(SEE_OTHER_HOST_TABLE, SQLSERVER_CREATE_STATS_TABLE);
						break;
					case postgresql:
					case mysql:
					default:
						data_repo.checkTable(SEE_OTHER_HOST_TABLE, CREATE_STATS_TABLE);
						break;
				}

				data_repo.initPreparedStatement(get_host_query, get_host_query);
				data_repo.initPreparedStatement(get_all_data_query, get_all_data_query);

				this.data_repo = data_repo;
				queryAllDB();
			} catch (SQLException ex) {
				throw new TigaseDBException("Could not initialize repository", ex);
			}
		}

		private void queryAllDB() throws SQLException {
			PreparedStatement get_all = data_repo.getPreparedStatement(null, get_all_data_query);
			get_all.setQueryTimeout(DEF_QUERY_TIME_OUT);

			ResultSet rs = null;

			synchronized (get_all) {
				try {
					rs = get_all.executeQuery();

					while (rs.next()) {
						String user_jid = rs.getString("user_id");
						String node_jid = rs.getString(NODE_ID);
						try {
							BareJID user = BareJID.bareJIDInstance(user_jid);
							BareJID node = BareJID.bareJIDInstance(node_jid);
							redirectsMap.put(user, node);
						} catch (TigaseStringprepException ex) {
							log.warning("Invalid user's or node's JID: " + user_jid + ", " + node_jid);
						}
					} // end of if (isnext) else
				} finally {
					data_repo.release(null, rs);
				}
			}

			log.info("Loaded " + redirectsMap.size() + " redirect definitions from database.");
		}
	}
}
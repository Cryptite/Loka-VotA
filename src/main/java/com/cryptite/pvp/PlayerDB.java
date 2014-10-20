package com.cryptite.pvp;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.bukkit.OfflinePlayer;

import java.sql.*;
import java.util.logging.Logger;

public class PlayerDB {
    private static final Logger log = Logger.getLogger("Database");
    private static DataSource pool;
    private final LokaVotA plugin;

    public PlayerDB(LokaVotA plugin) {
        this.plugin = plugin;
        initDbPool();
    }

    public ResultSet getPlayer(PvPPlayer a) {
        Connection conn = null;
        try {
            conn = dbc();
            if (conn == null) return null;

            OfflinePlayer p = plugin.server.getOfflinePlayer(a.name);

            if (p == null || !p.hasPlayedBefore()) {
                System.out.println("No such OfflinePlayer or hasn't played before: " + a.name);
                return null;
            }

            PreparedStatement s = conn.prepareStatement("SELECT count(*) from players WHERE uuid = '"
                    + p.getUniqueId() + "' limit 1;");
            return s.executeQuery();
        } catch (final SQLException e) {
            System.out.println("Database connection error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null)
                try {
                    conn.close();
                } catch (final SQLException ignored) {
                }
        }
        return null;
    }

    public DataSource initDbPool() {
        pool = null;
        final String dns = "jdbc:mysql://iron.minecraftarium.com:3306/loka";

        pool = new DataSource();
        pool.setDriverClassName("com.mysql.jdbc.Driver");
        pool.setUrl(dns);
        pool.setUsername("loka");
        pool.setPassword("playerdb");

        createTables();

        return pool;
    }

    void createTables() {
        Connection conn = null;
        Statement st = null;
        try {
            conn = dbc();
            if (conn == null)
                return;
            // actions
            String query = "CREATE TABLE IF NOT EXISTS `players` (\n" +
                    "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                    "  `uuid` text(36) NOT NULL,\n" +
                    "  `name` varchar(24) NOT NULL,\n" +
                    "  `town` varchar(32) NOT NULL,\n" +
                    "  `channel` varchar(12) NOT NULL,\n" +
                    "  `lastsethome` bigint(13) NOT NULL,\n" +
                    "  `voteemeralds` int(11) NOT NULL,\n" +
                    "  `voted` int(11) NOT NULL,\n" +
                    "  PRIMARY KEY (`id`),\n" +
                    "  UNIQUE KEY `id` (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
            st = conn.createStatement();
            st.executeUpdate(query);
        } catch (final SQLException e) {
            System.out.println("Database connection error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (st != null)
                try {
                    st.close();
                } catch (final SQLException ignored) {
                }
            if (conn != null)
                try {
                    conn.close();
                } catch (final SQLException ignored) {
                }
        }
    }

    public static Connection dbc() {
        Connection con = null;
        try {
            con = pool.getConnection();
        } catch (final SQLException e) {
            System.out.println("Database connection failed. " + e.getMessage());
            if (!e.getMessage().contains("Pool empty")) {
                e.printStackTrace();
            }
        }
        return con;
    }

    /**
     * Attempt to rebuild the pool, useful for reloads and failed database
     * connections being restored
     */
    public void rebuildPool() {
        // Close pool connections when plugin disables
        if (pool != null) {
            pool.close();
        }
        pool = initDbPool();
    }

    /**
     * Attempt to reconnect to the database
     *
     * @return
     * @throws java.sql.SQLException
     */
    protected boolean attemptToRescueConnection(SQLException e) throws SQLException {
        if (e.getMessage().contains("connection closed")) {
            rebuildPool();
            if (pool != null) {
                final Connection conn = dbc();
                if (conn != null && !conn.isClosed()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     *
     */
    public void handleDatabaseException(SQLException e) {
        try {
            if (attemptToRescueConnection(e)) {
                return;
            }
        } catch (final SQLException e1) {
        }
        System.out.println("Database connection error: " + e.getMessage());
        e.printStackTrace();
    }

    public void shutdown() {
        // Close pool connections when plugin disables
        if (pool != null) {
            pool.close();
        }
    }
}
package net.teamuni.economy.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.DatabaseMetaData;
import net.teamuni.economy.Uconomy;
import net.teamuni.economy.data.MoneyUpdater;
import net.teamuni.economy.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MySQLDatabase implements MoneyUpdater {
    private final HikariDataSource sql;
    private final List<String> economyIDs;

    public MySQLDatabase(String host, int port, String database, String parameters, String user, String password) {
        Uconomy main = Uconomy.getPlugin(Uconomy.class);
        HikariConfig config = new HikariConfig();
        config.setUsername(user);
        config.setPassword(password);
        StringBuilder sb = new StringBuilder("jdbc:mysql://")
                .append(host).append(":").append(port).append("/").append(database);
        if (!parameters.isEmpty()) {
            sb.append(parameters);
        }
        config.setJdbcUrl(sb.toString());

        this.sql = new HikariDataSource(config);
        this.economyIDs = main.getConfig().getStringList("EconomyID");

        try {
            initTable();
            updateColumn();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initTable() throws SQLException {
        try (Connection connection = this.sql.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                StringBuilder query = new StringBuilder("CREATE TABLE IF NOT EXISTS uconomy(uuid varchar(36) primary key");
                for (String economyID : this.economyIDs) {
                    query.append(", ")
                            .append(economyID)
                            .append(" BIGINT");
                }
                query.append(")");
                statement.execute(query.toString());
            }
        }
    }

    private void updateColumn() throws SQLException {
        try (Connection connection = this.sql.getConnection()) {
            DatabaseMetaData dbm = connection.getMetaData();
            for (String economyID : this.economyIDs) {
                ResultSet columns = dbm.getColumns(null, null, "uconomy", economyID);
                if (!columns.next()) {
                    String query = "ALTER TABLE uconomy ADD COLUMN " + economyID + " BIGINT";
                    try (PreparedStatement ps = connection.prepareStatement(query)) {
                        ps.execute();
                    }
                }
            }
        }
    }

    @Override
    public void updatePlayerStats(PlayerData stats) {
        try {
            try (Connection connection = this.sql.getConnection()) {
                StringBuilder query1 = new StringBuilder("INSERT INTO uconomy (uuid, ");
                StringBuilder query2 = new StringBuilder(") VALUE ('").append(stats.getUuid()).append("', ");
                StringBuilder query3 = new StringBuilder(") ON DUPLICATE KEY UPDATE ");
                for (Map.Entry<String, Long> entry : stats.getMoneyMap().entrySet()) {
                    query1.append(entry.getKey())
                            .append(", ");
                    query2.append(entry.getValue())
                            .append(", ");
                    query3.append(entry.getKey())
                            .append(" = ")
                            .append(entry.getValue())
                            .append(", ");
                }
                query1.delete(query1.length() - 2, query1.length());
                query2.delete(query2.length() - 2, query2.length());
                query3.delete(query3.length() - 2, query3.length());
                StringBuilder finalQuery = query1.append(query2).append(query3);

                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(finalQuery.toString());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public PlayerData loadPlayerStats(UUID uuid) {
        try {
            if (hasAccount(uuid)) {
                try (Connection connection = this.sql.getConnection()) {
                    Map<String, Long> map = new HashMap<>();
                    StringBuilder query = new StringBuilder();

                    for (String economyID : this.economyIDs) {
                        query.append("SELECT ")
                                .append(economyID)
                                .append(" FROM uconomy WHERE uuid = '")
                                .append(uuid.toString())
                                .append("'");

                        try (Statement statement = connection.createStatement()) {
                            ResultSet result = statement.executeQuery(query.toString());
                            query.setLength(0);
                            if (result.next()) {
                                map.put(economyID, result.getLong(1));
                            }
                        }
                    }
                    return new PlayerData(uuid, map);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return defaultPlayerStats(uuid);
    }

    @Override
    public PlayerData defaultPlayerStats(UUID uuid) {
        Map<String, Long> map = new HashMap<>();
        for (String economyID : this.economyIDs) {
            map.put(economyID, 0L);
        }
        return new PlayerData(uuid, map);
    }

    @Override
    public boolean hasAccount(UUID uuid) {
        try {
            Connection connection = this.sql.getConnection();
            PreparedStatement statement = connection.prepareStatement("SELECT uuid FROM uconomy WHERE uuid = ?");
            statement.setString(1, uuid.toString());

            try (connection; statement) {
                ResultSet result = statement.executeQuery();
                return result.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        try {
            try (Connection connection = this.sql.getConnection()) {
                StringBuilder query1 = new StringBuilder("INSERT INTO uconomy (uuid, ");
                StringBuilder query2 = new StringBuilder(") VALUE ('").append(player.getUniqueId()).append("', ");
                for (String economyID : this.economyIDs) {
                    query1.append(economyID)
                            .append(", ");
                    query2.append(0)
                            .append(", ");
                }
                query1.delete(query1.length() - 2, query1.length());
                query2.delete(query2.length() - 2, query2.length());
                StringBuilder finalQuery = query1.append(query2).append(")");

                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(finalQuery.toString());
                    Bukkit.getLogger().info("[Uconomy] " + player.getName() + "님의 돈 정보를 생성하였습니다.");
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}

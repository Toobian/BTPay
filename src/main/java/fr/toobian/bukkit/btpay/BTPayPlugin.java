package fr.toobian.bukkit.btpay;

import com.iCo6.Constants;
import com.iCo6.system.Holdings;
import java.sql.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Toobian <toobian@toobian.fr>
 */
public class BTPayPlugin extends JavaPlugin implements Listener{

    private FileConfiguration config;
    private String hostname, port, database, username, password;
    
    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
        
        if(!getDataFolder().exists()) {
            this.saveDefaultConfig();
        }
        
        config = getConfig();
        
        hostname = config.getString("sql.hostname");
        port = config.getString("sql.port");
        database = config.getString("sql.database");
        username = config.getString("sql.username");
        password = config.getString("sql.password");
        
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);
    }
    
    @EventHandler(priority= EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if(this.canReceivePay(event.getPlayer().getName()) && event.getPlayer().hasPermission("btpay.pay")) {
            Holdings compte = new Holdings(event.getPlayer().getName());
            double pay = config.getDouble("pay");
            compte.add(pay);
            List<String> Major = Constants.Nodes.Major.getStringList();
            String message = config.getString("message").replaceAll(":pay:", ""+pay);
            if(pay < 2)
                message = message.replaceAll(":unit:", Major.get(0));
            else
                message = message.replaceAll(":unit:", Major.get(1));
            event.getPlayer().sendMessage(message);
            
        }
        this.changeLastConnection(event.getPlayer().getName());
    }
    
    private boolean canReceivePay(String player) {
        boolean canReceivePay = false;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + database, username, password);
            String sql = "SELECT last_connection FROM btpay WHERE player = ?";
            PreparedStatement stat = connection.prepareStatement(sql);
            stat.setString(1, player);
            ResultSet result = stat.executeQuery();
            if(result.first()) {
                Date date = result.getDate("last_connection");
                Calendar last = new GregorianCalendar(Locale.FRANCE);
                last.setTime(date);
                Calendar today = new GregorianCalendar(Locale.FRANCE);
                canReceivePay = (last.get(Calendar.YEAR) <= today.get(Calendar.YEAR) && last.get(Calendar.MONTH) <= today.get(Calendar.MONTH) && last.get(Calendar.DAY_OF_MONTH) < today.get(Calendar.DAY_OF_MONTH));
            } else {
                canReceivePay = true;
            }
        } catch (SQLException ex) {
            getLogger().log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
        return canReceivePay;
    }
    
    private void changeLastConnection(String player) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + database, username, password);
            String sql = "INSERT INTO btpay (player, last_connection) VALUES(?, ?) ON DUPLICATE KEY UPDATE last_connection=?";
            PreparedStatement stat = connection.prepareStatement(sql);
            stat.setString(1, player);
            Date now = new Date(Calendar.getInstance(Locale.FRANCE).getTimeInMillis());
            stat.setDate(2, now);
            stat.setDate(3, now);
            int res = stat.executeUpdate();
            if(res == 0) {
                getLogger().log(Level.WARNING, "Last connection not saved for "+player);
            }
        } catch (SQLException ex) {
            getLogger().log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
    }
}

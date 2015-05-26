package io.github.OscarNorman.OneVsOne;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.FileConfigurationOptions;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;


public class OneVsOne extends JavaPlugin implements Listener{

	private ArrayList<Game> pendingGames = new ArrayList<Game>();
	private ArrayList<Game> activeGames = new ArrayList<Game>();
	private ArrayList<Game> inProgressGames = new ArrayList<Game>();
	private ArrayList<Game> challengedGames = new ArrayList<Game>();
	public ArrayList<Location> startPoints = new ArrayList<Location>();
	public static Economy econ = null;
	private static final Logger log = Logger.getLogger("Minecraft");


	@EventHandler
	public void damageListener(EntityDamageByEntityEvent e){
		if(e.getDamager() instanceof Player){
			Player en = (Player) e.getDamager();
			Player p = (Player) e.getEntity();
			for(Game g:activeGames){
				if((g.challenged==p||g.challenger==p)||(g.challenged==en||g.challenger==en)){
					e.setCancelled(true);				
				}

			}

		}
	}

	@EventHandler
	public void deathListener(PlayerDeathEvent e){
		for(Game g:activeGames){
			if((g.challenged==e.getEntity())){
				g.challenged.teleport((Location) g.challenger.getMetadata("oldlocation"));
				g.challenger.teleport((Location) g.challenger.getMetadata("oldlocation"));
				g.winner=g.challenger;
				endGame(g);
			}
			if((g.challenger==e.getEntity())){
				g.challenged.teleport((Location) g.challenged.getMetadata("oldlocation"));
				g.challenger.teleport((Location) g.challenger.getMetadata("oldlocation"));
				g.winner=g.challenged;
				endGame(g);
			}

		}
	}

	@Override
	public void onEnable() {
		PluginManager manager = getServer().getPluginManager();
		manager.registerEvents(this, this);
		if (!testForVault() ) {
			log.severe(String.format("[%s] - No Vault Found!", getDescription().getName()));
			return;
		}
		if (!setupEconomy() ) {
			log.severe(String.format("[%s] - Vault Found, But No Economy Plugin! Monatary Rewards Will Not Work!", getDescription().getName()));
			return;
		}
		this.saveDefaultConfig();
		FileConfiguration config = this.getConfig();
		Integer i = 1;
		while(config.contains("arenalocations."+i)){
			Location l = (Location) config.get("arenalocations."+i);
			startPoints.add(l);
			i+=1;
		}
	}


	@Override
	public void onDisable() {
		// TODO Insert logic to be performed when the plugin is disabled
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (Bukkit.getServer().getConsoleSender() == sender) {
			sender.sendMessage("This command can only be run by a player.");
			return true;
		}		

		if (cmd.getName().equalsIgnoreCase("onevsone")) {
			if(args.length>0){
				if(args[0].equals("help")){
					sender.sendMessage(ChatColor.DARK_GREEN+"+===---===---===OneVsOne===---===---===+");
					sender.sendMessage(ChatColor.YELLOW+"This is the help for the OneVsOne Plugin");
					sender.sendMessage(ChatColor.YELLOW+"Use "+ChatColor.DARK_GREEN+"/onevsone help"+ChatColor.YELLOW+" For This Help Message");
					sender.sendMessage(ChatColor.YELLOW+"Use "+ChatColor.DARK_GREEN+"/onevsone <PlayerName>"+ChatColor.YELLOW+" To Challenge A Player To A 1v1");
					sender.sendMessage(ChatColor.YELLOW+"Use "+ChatColor.DARK_GREEN+"/onevsone accept"+ChatColor.YELLOW+" To Accept A Challenge");
					sender.sendMessage(ChatColor.YELLOW+"Use "+ChatColor.DARK_GREEN+"/onevsone deny"+ChatColor.YELLOW+" To Deny A Challenge");
					sender.sendMessage(ChatColor.YELLOW+"Use "+ChatColor.DARK_GREEN+"/onevsone setlocation"+ChatColor.YELLOW+" To Set A 1v1 Spawn Point (This Is Usually Ops Only!)");
					sender.sendMessage(ChatColor.DARK_GREEN+"+===---===---===--+==+--===---===---===+");
				}

				if(args[0].equals("accept")){
					for(Game g:challengedGames){
						if(g.challenged==(Player)sender){
							g.challenger.sendMessage(ChatColor.YELLOW+sender.getName()+ChatColor.DARK_GREEN+" Accepted"+ChatColor.YELLOW+" Your Challenge!");
							sender.sendMessage(ChatColor.YELLOW+"You "+ChatColor.DARK_GREEN+"Accepted"+ChatColor.YELLOW+" The Challenge!");
							challengedGames.remove(g);
							pendingGames.add(g);
							g.myDenial.cancel();
							g.myDenial=null;
							checkStartGame();
							return true;
						}
					}
					sender.sendMessage(ChatColor.YELLOW+"You Have No Games To Accept!");
				}
				if(args[0].equals("deny")){
					for(Game g:challengedGames){
						if(g.challenged==(Player)sender){
							g.challenger.sendMessage(ChatColor.YELLOW+sender.getName()+ChatColor.DARK_GREEN+" Denied"+ChatColor.YELLOW+" Your Challenge!");
							sender.sendMessage(ChatColor.YELLOW+"You "+ChatColor.DARK_GREEN+"Denied"+ChatColor.YELLOW+" The Challenge!");
							challengedGames.remove(g);
							return true;
						}

					}
					sender.sendMessage(ChatColor.YELLOW+"You Have No Games To Deny!");
				}
				if(args[0].equals("setlocation")){
					if(((Player)sender).hasPermission("onevsone.createstart")){
						Location loc = ((Entity) sender).getLocation();
						startPoints.add(loc);
						sender.sendMessage(ChatColor.YELLOW+"Location Set!");
						FileConfiguration config = this.getConfig();
						FileConfigurationOptions opt = config.options();
						opt.header("1v1 Plugin Config");
						config.createSection("arenalocations");
						Integer i = 1;
						for(Location l:startPoints){
							config.set("arenalocations."+i,l);
							i+=1;
						}
						try {
							config.save(this.getDataFolder() + File.separator + "config.yml");
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				for(Player p:Bukkit.getOnlinePlayers()){
					if(args[0].equals(p.getName())){
						Player s = (Player)sender;
						if(p.getName()!=s.getName()){
							sender.sendMessage((ChatColor.DARK_GREEN+"You "+ChatColor.YELLOW+"Challenged "+ChatColor.DARK_GREEN+p.getName()));
							p.sendMessage(ChatColor.DARK_GREEN+((Player)sender).getName()+ChatColor.YELLOW+" Would like to 1v1 You!");
							p.sendMessage(ChatColor.DARK_GREEN+"/onevsone accept"+ChatColor.YELLOW+" to accept the challenge!");
							p.sendMessage(ChatColor.DARK_GREEN+"/onevsone deny"+ChatColor.YELLOW+" to deny it :(");
							//TODO check if either player is in a match
							Game g = new Game();
							g.challenged = p;
							g.challenger = (Player)sender;
							challengedGames.add(g);
							DenialTimer dt = new DenialTimer(this);
							g.myDenial=dt;
							dt.startCountdown(60, p, g);
						}else{
							p.sendMessage(ChatColor.YELLOW+"You Can't Challenge Yourself Silly!");

						}
						return true;
					}
				}
			}


			else{
				sender.sendMessage(ChatColor.DARK_GREEN+"+===---===---===OneVsOne===---===---===+");
				sender.sendMessage(ChatColor.YELLOW+"This is the help for the OneVsOne Plugin");
				sender.sendMessage(ChatColor.YELLOW+"Use "+ChatColor.DARK_GREEN+"/onevsone help"+ChatColor.YELLOW+" For This Help Message");
				sender.sendMessage(ChatColor.YELLOW+"Use "+ChatColor.DARK_GREEN+"/onevsone <PlayerName>"+ChatColor.YELLOW+" To Challenge A Player To A 1v1");
				sender.sendMessage(ChatColor.YELLOW+"Use "+ChatColor.DARK_GREEN+"/onevsone accept"+ChatColor.YELLOW+" To Accept A Challenge");
				sender.sendMessage(ChatColor.YELLOW+"Use "+ChatColor.DARK_GREEN+"/onevsone deny"+ChatColor.YELLOW+" To Deny A Challenge");
				sender.sendMessage(ChatColor.YELLOW+"Use "+ChatColor.DARK_GREEN+"/onevsone setlocation"+ChatColor.YELLOW+" To Set A 1v1 Spawn Point (This Is Usually Ops Only!)");
				sender.sendMessage(ChatColor.DARK_GREEN+"+===---===---===--+==+--===---===---===+");
			}
			return true;

		}

		//If this has happened the function will return true. 
		// If this hasn't happened the value of false will be returned.
		return false; 
	}

	public void checkStartGame(){
		while(pendingGames.size()>0&&startPoints.size()>0){
			Game g = pendingGames.remove(0);
			Location loc = startPoints.remove(0);
			g.gameLoc=loc;
			activeGames.add(g);
			((Metadatable) g.challenged).setMetadata("oldLocation",new FixedMetadataValue(this,(g.challenged).getLocation()));
			((Metadatable) g.challenger).setMetadata("oldLocation",new FixedMetadataValue(this,(g.challenger).getLocation()));

			savePlayer(g.challenged,this);
			savePlayer(g.challenger,this);

			g.challenged.teleport(g.gameLoc);
			g.challenger.teleport(g.gameLoc);

			setInv(g.challenged,"items.equipment","items.armour",this);
			setInv(g.challenged,"items.equipment","items.armour",this);

			GameCountdown cd = new GameCountdown(this, g);
			cd.startCountdown(10, "Starting in"+ChatColor.DARK_GREEN+" #");
		}
	}

	public void startGame(Game game) {
		game.challenged.sendMessage((ChatColor.YELLOW+"Go!"));
		game.challenger.sendMessage((ChatColor.YELLOW+"Go!"));
		for(Player p:Bukkit.getOnlinePlayers()){
			if(p.getName()!=(game.challenged.getName())&&p.getName()!=(game.challenger.getName())){
				game.challenged.hidePlayer(p);
				game.challenger.hidePlayer(p);
				inProgressGames.add(game);
				activeGames.remove(game);
			}
		}
	}
	@SuppressWarnings("deprecation")
	public void endGame(Game game){
		game.challenged.setGameMode(GameMode.SURVIVAL);
		game.challenger.setGameMode(GameMode.SURVIVAL);
		restorePlayer(game.challenged, this);
		restorePlayer(game.challenger, this);
		if(game.winner!=null){
			if(this.getConfig().getBoolean("monataryreward.enabled")){
				if(econ!=null){
					econ.depositPlayer(game.winner.getName(), this.getConfig().getDouble("monataryreward.amount"));
				}
			}
			if(this.getConfig().getBoolean("itemreward.enabled")){
				setInv(game.winner,"itemreward.items",null,this);
			}
		}
		inProgressGames.remove(game);
	}

	private void restorePlayer(Player p,Plugin plugin){
		//clear invs
		PlayerInventory inv = p.getInventory();
		inv.clear();
		inv.setArmorContents(new ItemStack[4]);

		ItemStack[] invStack=(ItemStack[])MetadataHelper.getMetadata("inv",plugin,p);
		if(invStack!=null){
			plugin.getLogger().info("Stack = "+ invStack.toString());
			p.getInventory().setContents(invStack);
		}
		ItemStack[] armStack=(ItemStack[])MetadataHelper.getMetadata("armour",plugin,p);
		if(armStack!=null){
			plugin.getLogger().info("armStack = "+ armStack.toString());
			p.getEquipment().setArmorContents(armStack);
		}

	}
	private void savePlayer(Player p, Plugin plugin){
		ItemStack[] armour = (p).getEquipment().getArmorContents();
		ItemStack[] inv = (p).getInventory().getContents();
		((Metadatable) p).setMetadata("armour",new FixedMetadataValue(plugin,armour)); 
		((Metadatable) p).setMetadata("inv",new FixedMetadataValue(plugin,inv));
		PlayerInventory inv1 = (p).getInventory();
		inv1.clear();
		inv1.setArmorContents(new ItemStack[4]);

	}

	@SuppressWarnings("deprecation")
	private void setInv(Player p, String equipmentPath,String armourPath, Plugin plugin){
		PlayerInventory inventory = p.getInventory();
		if(equipmentPath!=null){
			String[] items = plugin.getConfig().getString(equipmentPath).split(",");
			for(String item:items){
				String[] i = item.split(":");

				String type = i[0];
				int count = 1;
				short meta = 0;

				switch (i.length) { 
				case 1: 
					break; 
				case 2: 
					count = Integer.parseInt(i[1]); 
					break; 
				case 3: 
					count = Integer.parseInt(i[2]);  
					meta = Short.parseShort(i[1]);
					break; 
				} 

				Material m = Material.matchMaterial(type);
				if (m != null) {
					ItemStack is;
					if(meta!=0){
						is = new ItemStack(m,count,meta);
						inventory.addItem(is);
					}else{
						is = new ItemStack(m,count);
						inventory.addItem(is);
					}

				} else {
					p.sendMessage("Item "+i+" could not be found");
				}
			}
		}
		if(armourPath!=null){
			ItemStack[] armour = new ItemStack[4];
			String[] armours = plugin.getConfig().getString(armourPath).split(",");
			if(armours.length==4){
				for(int i=0;i<4;i++){				
					armour[i] = new ItemStack(Integer.parseInt(armours[i]));						
				}

				inventory.setArmorContents(armour);
			}
		}

	}

	private boolean testForVault() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		return true;
	}

	private boolean setupEconomy() {
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}


}





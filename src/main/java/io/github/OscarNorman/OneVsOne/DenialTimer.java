package io.github.OscarNorman.OneVsOne;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;


public class DenialTimer {
	private int countdownTimer;
	private OneVsOne plugin;

	public DenialTimer(OneVsOne p){
		plugin=p;
		
	}
	
	
	public void startCountdown(final int time, final Player p,final Game g){
		this.countdownTimer = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this.plugin, new Runnable(){
			int i = time;
			public void run(){

				this.i--;
				if (this.i <= 0) DenialTimer.this.finished(p, g);
			
			}
		}, 0L, 20L);
	}

	public void finished(Player p, Game g){
		Bukkit.getScheduler().cancelTask(this.countdownTimer);
		g.challenger.sendMessage(ChatColor.YELLOW+p.getName()+ChatColor.DARK_GREEN+" Denied"+ChatColor.YELLOW+" Your Challenge!");
		p.sendMessage(ChatColor.YELLOW+"You "+ChatColor.DARK_GREEN+"Denied"+ChatColor.YELLOW+" The Challenge!");
	}


	public void cancel() {
		Bukkit.getScheduler().cancelTask(this.countdownTimer);
		plugin=null;
	}
}



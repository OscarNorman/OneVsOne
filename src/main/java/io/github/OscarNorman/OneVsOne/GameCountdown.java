package io.github.OscarNorman.OneVsOne;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;


public class GameCountdown {


	private final OneVsOne plugin;

	Game game;
	private int countdownTimer;

	public GameCountdown(OneVsOne i, Game game){
		this.plugin = i;
		this.game = game;
	}
	public void startCountdown(final int time, final String msg){
		this.countdownTimer = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this.plugin, new Runnable(){
			int i = time;
			public void run(){
				String m=(ChatColor.YELLOW+msg.replace("#", Integer.toString(i)));
					
				
				this.i--;
				game.challenged.sendMessage(m);
				game.challenger.sendMessage(m);
				if (this.i <= 0) GameCountdown.this.cancel();
			}
		}
		, 0L, 20L);
	}
	public void cancel(){
		Bukkit.getScheduler().cancelTask(this.countdownTimer);
		plugin.startGame(game);
		game.challenged.setHealth(20);
		game.challenger.setHealth(20);
		game.challenged.setFoodLevel(20);
		game.challenger.setFoodLevel(20);
		game.challenged.setGameMode(GameMode.ADVENTURE);
		game.challenger.setGameMode(GameMode.ADVENTURE);
	}
}


package io.github.OscarNorman.OneVsOne;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class MetadataHelper {

	public static Object getMetadata(String key,Plugin plugin,Player p){
		List<MetadataValue> values = p.getMetadata(key);  
		for (MetadataValue value : values) {
			// Plugins are singleton objects, so using == is safe here
			if (value.getOwningPlugin() == plugin) {
				return value.value();

			}
		}
		return null;
	}
}

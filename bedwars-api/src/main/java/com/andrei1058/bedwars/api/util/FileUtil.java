package com.andrei1058.bedwars.api.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;

import org.bukkit.Bukkit;

public class FileUtil {

	public static void delete(File file) {
		if(file.isDirectory()) {
			//noinspection ConstantConditions
			for(File subfile : file.listFiles()) {
				delete(subfile);
			}
		} else {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
		}
	}

	public static void setMainLevel(String worldName){
		Properties properties = new Properties();

		try (FileInputStream in = new FileInputStream("server.properties")) {
			properties.load(in);
		} catch (IOException e) {
			Bukkit.getLogger().log(Level.WARNING, "Could not read server.properties while changing the main level", e);
		}

		properties.setProperty("level-name", worldName);
		properties.setProperty("generator-settings", "minecraft:air;minecraft:air;minecraft:air");
		properties.setProperty("allow-nether", "false");
		properties.setProperty("level-type", "flat");
		properties.setProperty("generate-structures", "false");
		properties.setProperty("spawn-monsters", "false");
		properties.setProperty("max-world-size", "1000");
		properties.setProperty("spawn-animals", "false");

		try (FileOutputStream out = new FileOutputStream("server.properties")) {
			properties.store(out, null);
		} catch (IOException e) {
			Bukkit.getLogger().log(Level.WARNING, "Could not save server.properties while changing the main level", e);
		}
	}
}

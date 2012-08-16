package org.zonedabone.typocorrector;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class TypoCorrector extends JavaPlugin implements Listener {

	Map<String, String> replacements;

	public void onEnable() {
		new Thread(){
			public void run(){
				loadTypos();
			}
		}.start();
		this.getServer().getPluginManager().registerEvents(this, this);
	}

	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent e) {
		if (e.getPlayer().hasPermission("typo.correct")) {
			String message = e.getMessage();
			for (Map.Entry<String, String> typo : replacements.entrySet()) {
				message = correct(message, typo.getKey(), typo.getValue());
			}
			e.setMessage(message);
		}
	}

	private String correct(String text, String typo, String replacement) {
		Pattern p = Pattern.compile("\\b" + typo + "\\b",
				Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(text);

		StringBuffer sb = new StringBuffer();

		while (m.find()) {
			String replace;
			if (Character.isUpperCase(m.group().toCharArray()[0])) {
				replace = Character.toUpperCase(m.group().toCharArray()[0])
						+ replacement.substring(1);
			} else {
				replace = replacement;
			}
			m.appendReplacement(sb, Matcher.quoteReplacement(replace));
		}
		m.appendTail(sb);

		return sb.toString();
	}

	private void loadTypos() {
		replacements = new ConcurrentHashMap<String, String>();
		InputStream istream;
		try {
			istream = new URL(
					"http://en.wikipedia.org/w/api.php?format=xml&action=query&titles=Wikipedia:Lists_of_common_misspellings/For_machines&prop=revisions&rvprop=content")
					.openStream();
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
			return;
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		DataInputStream in = new DataInputStream(istream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		try {
			long loaded = 0;
			while ((strLine = br.readLine()) != null) {
				if (strLine.contains("-&gt;")) {
					strLine = strLine.trim();
					String[] parts = strLine.split("-&gt;");
					replacements.put(parts[0], parts[1].split(", ")[0]);
					loaded++;
				}

			}
			getLogger().info("Loaded " + loaded + " replacements.");
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

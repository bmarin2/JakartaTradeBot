package com.tradebot.service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import lombok.Data;

@Data
public class TelegramBot {
	private final String BOT_TOKEN = "6206961432:AAGY8arfIXHBE8uw8g1knAEnxY9_ekBVr0c";
	private final String CHAT_ID = "5638093694";

	public void sendMessage(String message) throws IOException {
		String urlString = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage";
		URL url = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json; utf-8");
		con.setDoOutput(true);

		String jsonInputString = "{\"chat_id\":\"" + CHAT_ID + "\",\"text\":\"" + message + "\"}";
		try (OutputStream os = con.getOutputStream()) {
			byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
			os.write(input, 0, input.length);
		}
		con.getInputStream();
		con.disconnect();
	}
}

package com.tradebot.service;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class TelegramBot {

	private final String BOT_TOKEN = "6206961432:AAGY8arfIXHBE8uw8g1knAEnxY9_ekBVr0c";
	private final String CHAT_ID = "5638093694";

	public void sendMessage(String message) throws IOException {

                URL url = new URL("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String jsonPayload = "{\"chat_id\":\"" + CHAT_ID + "\",\"text\":\"" + message + "\"}";
                OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
                writer.write(jsonPayload);
                writer.flush();
                writer.close();

                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new RuntimeException("Telegram HTTP error code : " + conn.getResponseCode());
                }
                conn.disconnect();
	}
}

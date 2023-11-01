package com.tradebot.service;

import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.tradebot.binance.UMFuturesClientConfig;
import com.tradebot.db.AlarmDB;
import com.tradebot.model.Alarm;
import com.tradebot.model.FuturesBot;

public class FuturesBotTask implements Runnable {
	
	private FuturesBot futuresBot;
	private UMFuturesClientImpl umFuturesClientImpl;
	private final TelegramBot telegramBot;

	public FuturesBotTask(FuturesBot futuresBot) {
		this.futuresBot = futuresBot;
		umFuturesClientImpl = UMFuturesClientConfig.futuresSignedTest();
		this.telegramBot = new TelegramBot();
	}

	@Override
	public void run() {
		try {
			Alarm alarm = AlarmDB.getOneAlarm(futuresBot.getDemaAlertTaskId());
			if (alarm.getCrosss()) {
				
			}
		} catch (Exception e) {
		}		
		
	}
}

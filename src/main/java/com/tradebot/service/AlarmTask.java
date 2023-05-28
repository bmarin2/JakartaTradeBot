package com.tradebot.service;

import com.binance.connector.client.impl.SpotClientImpl;
import com.tradebot.binance.SpotClientConfig;
import com.tradebot.configuration.OrdersParams;
import com.tradebot.db.AlarmDB;
import com.tradebot.model.Alarm;
import java.math.BigDecimal;
import org.json.JSONObject;

public class AlarmTask implements Runnable {
	
	private Alarm alarm;
	private SpotClientImpl spotClientImpl;
	private final TelegramBot telegramBot;
	private boolean greater;
	
	public AlarmTask(Alarm alarm, boolean greater) throws Exception {
		this.spotClientImpl = SpotClientConfig.spotClientSignTest();
		this.telegramBot = new TelegramBot();
		this.alarm = alarm;
		this.greater = greater;
	}

	@Override
	public void run() {

		try {
			if(!AlarmDB.getOneAlarm(alarm.getId()).getMsgSent()) {
				
				String result = spotClientImpl.createMarket().tickerSymbol(OrdersParams.getTickerSymbolParams(alarm.getSymbol()));
				JSONObject jsonObject = new JSONObject(result);
				BigDecimal newPrice = new BigDecimal(jsonObject.getString("price"));

				if(greater) {
					if(newPrice.compareTo(alarm.getAlarmPrice()) > 0){
						telegramBot.sendMessage("Price is Greater \n" + alarm.getSymbol() + " " + alarm.getAlarmId() + "\n"
							+ "Current price: " + newPrice + "\n"
							+ "Alarm price:   " + alarm.getAlarmPrice());
						AlarmDB.markMessageSent(alarm.getId());
					}
				} else {
					if(newPrice.compareTo(alarm.getAlarmPrice()) < 0){
						telegramBot.sendMessage("Price is Lesser \n" + alarm.getSymbol() + " " + alarm.getAlarmId() + "\n"
							+ "Current price: " + newPrice
							+ "Alarm price:   " + alarm.getAlarmPrice());
						AlarmDB.markMessageSent(alarm.getId());
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
}

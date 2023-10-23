package com.tradebot.service;

import com.binance.connector.client.impl.SpotClientImpl;
import com.tradebot.binance.SpotClientConfig;
import com.tradebot.db.AlarmDB;
import com.tradebot.model.Alarm;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.json.JSONArray;

public class DemaAlertTask implements Runnable {
	
	private Alarm alarm;
	private SpotClientImpl spotClientImpl;
	private final TelegramBot telegramBot;

	private double currentFastEMA;
	private double currentSlowEMA;
	
	private double currentFastDEMA;
	private double currentSlowDEMA;

	private final double multiplierFastDEMA;
	private final double multiplierSlowDEMA;

	public DemaAlertTask(Alarm alarm) throws Exception {
		this.spotClientImpl = SpotClientConfig.spotClientOnlyBaseURLProd();
		this.telegramBot = new TelegramBot();
		this.alarm = alarm;
		this.currentFastEMA = 0;
		this.currentSlowEMA = 0;
		this.currentFastDEMA = 0;
		this.currentSlowDEMA = 0;
		this.multiplierFastDEMA = 2.0 / (double) (alarm.getFastDema() + 1);
		this.multiplierSlowDEMA = 2.0 / (double) (alarm.getSlowDema() + 1);
		resetCross();
		init(getKlinePrices(), alarm.getFastDema(), alarm.getSlowDema());
	}

	@Override
	public void run() {
		try {
			
			double latest = getLatestPrice();
			update(latest);
			
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	private void resetCross() throws Exception {
		Alarm al = AlarmDB.getOneAlarm(alarm.getId());
		al.setCrosss(false);
		AlarmDB.editAlarm(al);
	}
	
	private void init(List<Double> prices, Integer fastDema, Integer slowDema) throws Exception {
		int startIndex = prices.size() - fastDema;

		for (int i = startIndex; i < slowDema; i++) {
			currentFastEMA += prices.get(i);
		}
		
		for (int i = 0; i < slowDema; i++) {
			currentSlowEMA += prices.get(i);
		}

		currentFastEMA = currentFastEMA / (double) fastDema;
		currentFastDEMA = currentFastEMA;

		currentSlowEMA = currentSlowEMA / (double) slowDema;
		currentSlowDEMA = currentSlowEMA;
		
		Alarm al = AlarmDB.getOneAlarm(alarm.getId());
		
		if (currentFastEMA < currentSlowEMA) {
			al.setCrosss(true);
			AlarmDB.editAlarm(al);
		}
	}	
	
	private List<Double> getKlinePrices() {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", alarm.getSymbol());
		parameters.put("interval", alarm.getIntervall());
		parameters.put("limit", alarm.getSlowDema() + 1);
		
		List<Double> priceList = new ArrayList<>();
		
		String result = spotClientImpl.createMarket().klines(parameters);
		
		JSONArray jsonArray = new JSONArray(result);	
		jsonArray.remove(jsonArray.length() - 1);
		
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONArray candlestick = jsonArray.getJSONArray(i);			
			double newPrice = Double.parseDouble(candlestick.getString(4));
			priceList.add(newPrice);
		}
		return priceList;
	}
	
	public void update(double newPrice) throws Exception {
		currentFastEMA = (newPrice - currentFastEMA) * multiplierFastDEMA + currentFastEMA;
		currentSlowEMA = (newPrice - currentSlowEMA) * multiplierSlowDEMA + currentSlowEMA;
		
		// EMA of EMA
		currentFastDEMA = (currentFastEMA - currentFastDEMA) * multiplierFastDEMA + currentFastDEMA;
		currentSlowDEMA = (currentSlowEMA - currentSlowDEMA) * multiplierSlowDEMA + currentSlowDEMA;
		
		double calculatedFastDEMA = (2 * currentFastEMA) - currentFastDEMA;
		double calculatedSlowDEMA = (2 * currentSlowEMA) - currentSlowDEMA;

		Alarm al = AlarmDB.getOneAlarm(alarm.getId());
		
		boolean demaCross = al.getCrosss();
		
		if (demaCross && calculatedFastDEMA > calculatedSlowDEMA) {
			telegramBot.sendMessage("DEMA Alert " + alarm.getSymbol() + " (" + alarm.getIntervall() + ")\n"
				+ "DEMA " + alarm.getFastDema() + " UP crossed " + alarm.getSlowDema());
			
			al.setCrosss(false);
			AlarmDB.editAlarm(al);
			
		} else if (!demaCross && calculatedFastDEMA < calculatedSlowDEMA) {
			telegramBot.sendMessage("DEMA Alert - " + alarm.getSymbol() + " (" + alarm.getIntervall() + ")\n"
				+ "DEMA " + alarm.getFastDema() + " DOWN crossed " + alarm.getSlowDema());
			
			al.setCrosss(true);
			AlarmDB.editAlarm(al);
		}
		
	}
	
	private double getLatestPrice() {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", alarm.getSymbol());
		parameters.put("interval", alarm.getIntervall());
		parameters.put("limit", 2);

		String result = spotClientImpl.createMarket().klines(parameters);
		
		JSONArray jsonArray = new JSONArray(result);
		
		JSONArray innerArray = jsonArray.getJSONArray(0);
		
		double newPrice = Double.parseDouble(innerArray.getString(4));
		return newPrice;
	}
}

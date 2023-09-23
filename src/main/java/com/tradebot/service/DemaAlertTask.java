package com.tradebot.service;

import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.tradebot.binance.UMFuturesClientConfig;
import com.tradebot.model.Alarm;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.json.JSONArray;

public class DemaAlertTask implements Runnable {
	
	private Alarm alarm;
	private UMFuturesClientImpl uMFuturesClientImpl;
	private final TelegramBot telegramBot;

	private double currentFastEMA;
	private double currentSlowEMA;
	
	private double currentFastDEMA;
	private double currentSlowDEMA;

	private final double multiplierFastDEMA;
	private final double multiplierSlowDEMA;
	
	private boolean cross; // if true fast dema is below slow dema and vs

	public DemaAlertTask(Alarm alarm) throws Exception {
		this.cross = false;
		this.uMFuturesClientImpl = UMFuturesClientConfig.futuresClientOnlyBaseURLProd();
		this.telegramBot = new TelegramBot();
		this.alarm = alarm;
		this.currentFastEMA = 0;
		this.currentSlowEMA = 0;
		this.currentFastDEMA = 0;
		this.currentSlowDEMA = 0;
		this.multiplierFastDEMA = 2.0 / (double) (alarm.getFastDema() + 1);
		this.multiplierSlowDEMA = 2.0 / (double) (alarm.getSlowDema() + 1);
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
	
	private void init(List<Double> prices, Integer fastDemaLength, Integer slowDemaLength ) {
		int startIndex = prices.size() - alarm.getFastDema();

		for (int i = startIndex; i < slowDemaLength; i++) {
			currentFastEMA += prices.get(i);
		}
		
		for (int i = 0; i < slowDemaLength; i++) {
			currentSlowEMA += prices.get(i);
		}

		currentFastEMA = currentFastEMA / (double) fastDemaLength;
		currentFastDEMA = currentFastEMA;

		currentSlowEMA = currentSlowEMA / (double) slowDemaLength;
		currentSlowDEMA = currentSlowEMA;
		
		if (currentFastEMA < currentSlowEMA) {
			cross = true;
		}
	}	
	
	private List<Double> getKlinePrices() {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", alarm.getSymbol());
		parameters.put("interval", alarm.getIntervall());
		parameters.put("limit", alarm.getSlowDema() + 1);
		
		List<Double> priceList = new ArrayList<>();
		
		String result = uMFuturesClientImpl.market().klines(parameters);
		
		JSONArray jsonArray = new JSONArray(result);		
		jsonArray.remove(jsonArray.length() - 1);
		
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONArray candlestick = jsonArray.getJSONArray(i);			
			double newPrice = Double.parseDouble(candlestick.getString(4));
			priceList.add(newPrice);
		}
		return priceList;
	}
	
	public void update(double newPrice) throws IOException {
		currentFastEMA = (newPrice - currentFastEMA) * multiplierFastDEMA + currentFastEMA;
		currentSlowEMA = (newPrice - currentSlowEMA) * multiplierSlowDEMA + currentSlowEMA;
		
		// EMA of EMA
		currentFastDEMA = (currentFastEMA - currentFastDEMA) * multiplierFastDEMA + currentFastDEMA;
		currentSlowDEMA = (currentSlowEMA - currentSlowDEMA) * multiplierSlowDEMA + currentSlowDEMA;
		
		double calculatedFastDEMA = (2 * currentFastEMA) - currentFastDEMA;
		double calculatedSlowDEMA = (2 * currentSlowEMA) - currentSlowDEMA;
		
		if (cross && calculatedFastDEMA > calculatedSlowDEMA) {
			telegramBot.sendMessage("DEMA Alert " + alarm.getSymbol() + " (" + alarm.getIntervall() + ")\n"
				+ "DEMA " + alarm.getFastDema() + " UP crossed " + alarm.getSlowDema());
			cross = false;
		} else if (!cross && calculatedFastDEMA < calculatedSlowDEMA) {
			telegramBot.sendMessage("DEMA Alert - " + alarm.getSymbol() + " (" + alarm.getIntervall() + ")\n"
				+ "DEMA " + alarm.getFastDema() + " DOWN crossed " + alarm.getSlowDema());
			cross = true;
		}
		
	}
	
	private double getLatestPrice() {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", alarm.getSymbol());
		parameters.put("interval", alarm.getIntervall());
		parameters.put("limit", 2);

		String result = uMFuturesClientImpl.market().klines(parameters);
		
		JSONArray jsonArray = new JSONArray(result);
		
		JSONArray innerArray = jsonArray.getJSONArray(0);
		
		double newPrice = Double.parseDouble(innerArray.getString(4));
		return newPrice;
	}
}

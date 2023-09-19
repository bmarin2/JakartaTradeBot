package com.tradebot.service;

import com.binance.connector.client.impl.SpotClientImpl;
import com.tradebot.binance.SpotClientConfig;
import com.tradebot.configuration.OrdersParams;
import com.tradebot.model.Alarm;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;

public class DemaAlertTask implements Runnable {
	
	private Alarm alarm;
	private SpotClientImpl spotClientImpl;
	private final TelegramBot telegramBot;
	
	public DemaAlertTask(Alarm alarm) throws Exception {
		this.spotClientImpl = SpotClientConfig.spotClientSignTest();
		this.telegramBot = new TelegramBot();
		this.alarm = alarm;
	}

	@Override
	public void run() {
		BigDecimal dema = calculateDEMA(getKlinePrices(20), 10);
		System.out.println();
		System.out.println("DEMA Length of 10 is: " + dema);
		
	}
	
	
	private List<BigDecimal> getKlinePrices(Integer length) {
		List<BigDecimal> pricesList = new ArrayList<>();
		String result = spotClientImpl.createMarket().klines(OrdersParams.getKlineParams(alarm.getSymbol(), alarm.getIntervall(), length));
		JSONArray jsonArray = new JSONArray(result);
		System.out.println(alarm.getIntervall());
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONArray candlestick = jsonArray.getJSONArray(i);			
			BigDecimal newPrice = new BigDecimal(candlestick.getString(4));
			System.out.println(i + " price: " + newPrice);
			pricesList.add(newPrice);
		}
		return pricesList;
	}
	
	private BigDecimal calculateEMA(List<BigDecimal> prices, Integer length) {

        BigDecimal multiplier = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(length + 1), 8, RoundingMode.DOWN);
        BigDecimal ema = prices.get(0);

        for (int i = 1; i < length; i++) {
            ema = prices.get(i).subtract(ema).multiply(multiplier).add(ema);
        }
        return ema;	   
	}
	
	private BigDecimal calculateDEMA(List<BigDecimal> prices, Integer length) {
		BigDecimal ema1 = calculateEMA(prices, length);
		BigDecimal ema2 = calculateEMA(prices.subList(length, prices.size()), length);
		return ema1.multiply(BigDecimal.valueOf(2)).subtract(ema2);
	}
}

package com.tradebot.service;

import com.binance.connector.client.impl.SpotClientImpl;
import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.tradebot.binance.SpotClientConfig;
import com.tradebot.binance.UMFuturesClientConfig;
import com.tradebot.enums.ChartMode;
import com.tradebot.model.MACDAlarm;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.json.JSONArray;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

public class MACDCross implements Runnable {

	private MACDAlarm macdAlarm;
	private UMFuturesClientImpl umFuturesClientImpl;
	private SpotClientImpl spotClientImpl;
	private final TelegramBot telegramBot;

	private BarSeries series;
	private ClosePriceIndicator closePriceIndicator;
	private MACDIndicator macd;

	public MACDCross(MACDAlarm macdAlarm) {
		if (macdAlarm.getChartMode() == ChartMode.SPOT) {
			spotClientImpl = SpotClientConfig.spotClientOnlyBaseURLProd();
		} else if (macdAlarm.getChartMode() == ChartMode.FUTURES) {
			umFuturesClientImpl = UMFuturesClientConfig.futuresBaseURLProd();
		}
		this.telegramBot = new TelegramBot();
		this.macdAlarm = macdAlarm;
		
		this.series = new BaseBarSeriesBuilder().withName("mySeries").build();
		fillBarSeries(macdAlarm.getDema() * 2);
		closePriceIndicator = new ClosePriceIndicator(this.series);
		macd = new MACDIndicator(closePriceIndicator);
		displayValues();
	}

	@Override
	public void run() {
		calculateNewValues();
		displayValues();		
	}
	
	private void fillBarSeries(int limit) {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", macdAlarm.getSymbol());
		parameters.put("interval", macdAlarm.getIntervall());
		parameters.put("limit", limit + 1);
		List<Double> priceList = new ArrayList<>();

		String result = "";		
		
		if (macdAlarm.getChartMode() == ChartMode.SPOT) {
			result = spotClientImpl.createMarket().klines(parameters);
		} else if (macdAlarm.getChartMode() == ChartMode.FUTURES) {
			result = umFuturesClientImpl.market().klines(parameters);
		}

		JSONArray jsonArray = new JSONArray(result);
		jsonArray.remove(jsonArray.length() - 1);

		for (int i = 0; i < jsonArray.length(); i++) {
			JSONArray candlestick = jsonArray.getJSONArray(i);

			long unixTimestampMillis = candlestick.getLong(6);
			Instant instant = Instant.ofEpochMilli(unixTimestampMillis);
			ZonedDateTime utcZonedDateTime = instant.atZone(ZoneId.of("UTC"));

			this.series.addBar(utcZonedDateTime,
				   candlestick.getString(1),
				   candlestick.getString(2),
				   candlestick.getString(3),
				   candlestick.getString(4),
				   candlestick.getString(5)
			);
		}
	}

	private void displayValues() {
		System.out.println("last macd  : " + this.macd.getValue(macd.getBarSeries().getEndIndex()));
		System.out.println("last signal: " + new EMAIndicator(this.macd, 9).getValue(this.macd.getBarSeries().getEndIndex()));
		System.out.println("last EMA  : " + new EMAIndicator(this.closePriceIndicator, 200).getValue(this.closePriceIndicator.getBarSeries().getEndIndex()));
		System.out.println("------------------------------");
	}
	
	private void calculateNewValues() {
		fillBarSeries(1);
		closePriceIndicator = new ClosePriceIndicator(this.series);
		macd = new MACDIndicator(closePriceIndicator);
	}
}

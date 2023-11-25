package com.tradebot.service;

import com.binance.connector.client.impl.SpotClientImpl;
import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.tradebot.binance.SpotClientConfig;
import com.tradebot.binance.UMFuturesClientConfig;
import com.tradebot.db.AlarmDB;
import com.tradebot.enums.ChartMode;
import com.tradebot.model.Alarm;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.json.JSONArray;

public class TemaOneCrossTask implements Runnable {

	private Alarm alarm;
	private UMFuturesClientImpl umFuturesClientImpl;
	private SpotClientImpl spotClientImpl;
	private final TelegramBot telegramBot;

	private double secondEma;
	private double thirdEma;

	private double secondDema;
	private double thirdDema;

	private double secondTema;
	private double thirdTema;

	private final double multiplierSecondDema;
	private final double multiplierThirdDema;

	public TemaOneCrossTask(Alarm alarm) throws Exception {
		if (alarm.getChartMode() == ChartMode.SPOT) {
			spotClientImpl = SpotClientConfig.spotClientOnlyBaseURLProd();
		} else if (alarm.getChartMode() == ChartMode.FUTURES) {
			umFuturesClientImpl = UMFuturesClientConfig.futuresSignedTest();
		}

		this.telegramBot = new TelegramBot();
		this.alarm = alarm;
		this.secondEma = 0;
		this.thirdEma = 0;
		this.secondDema = 0;
		
		this.secondTema = 0;
		this.thirdTema = 0;
		
		this.multiplierSecondDema = 2.0 / (double) (alarm.getSecondDema() + 1);
		this.multiplierThirdDema = 2.0 / (double) (alarm.getThirdDema() + 1);
		resetCross();
		init(getKlinePrices(), alarm.getSecondDema(), alarm.getThirdDema());
	}

	@Override
	public void run() {
		try {

			double latest = getLatestPrice();
			update(latest, true);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void resetCross() throws Exception {
		Alarm al = AlarmDB.getOneAlarm(alarm.getId());
		al.setCrosss(false);
		al.setCurrentFirstDema(0.0);
		al.setCurrentSecondDema(0.0);
		al.setCurrentThirdDema(0.0);
		al.setLastClosingCandle(0.0);
		AlarmDB.editAlarm(al);
	}

	private void init(List<Double> prices, Integer dema2, Integer dema3) throws Exception {

		List<Double> firstList = prices.subList(0, dema3);
		List<Double> secondList = prices.subList(dema3, prices.size());

		int startIndexSecond = firstList.size() - dema2;
		int startIndexThird = firstList.size() - dema3;

		for (int i = startIndexSecond; i < dema3; i++) {
			secondEma += firstList.get(i);
		}

		for (int i = startIndexThird; i < dema3; i++) {
			thirdEma += firstList.get(i);
		}

		secondEma = secondEma / (double) dema2;
		secondDema = secondEma;
		secondTema = secondEma;

		thirdEma = thirdEma / (double) dema3;
		thirdDema = thirdEma;
		thirdTema = thirdEma;

		for (Double secondListPrice : secondList) {
			update(secondListPrice, false);
		}
		calculateDemas();
		setLastCandle(secondList.get(secondList.size() - 1));
	}

	public void update(double newPrice, boolean setCross) throws Exception {

		secondEma = (newPrice - secondEma) * multiplierSecondDema + secondEma;
		thirdEma = (newPrice - thirdEma) * multiplierThirdDema + thirdEma;

		// EMA of EMA
		secondDema = (secondEma - secondDema) * multiplierSecondDema + secondDema;
		thirdDema = (thirdEma - thirdDema) * multiplierThirdDema + thirdDema;
		
		// EMA of DEMA
		secondTema = (secondDema - secondTema) * multiplierSecondDema + secondTema;
          thirdTema = (thirdDema - thirdTema) * multiplierThirdDema + thirdTema;

		if (setCross) {
			calculateDemas();
		}
	}

	private void calculateDemas() throws Exception {		
		double calculatedSlowTEMA = (3*secondEma) - (3*secondDema) + secondTema;
          double calculatedThirdTEMA = (3*thirdEma) - (3*thirdDema) + thirdTema;

		Alarm al = AlarmDB.getOneAlarm(alarm.getId());

		al.setCurrentSecondDema(calculatedSlowTEMA);
		al.setCurrentThirdDema(calculatedThirdTEMA);

		boolean demaCross = al.getCrosss();

		// for fast crossing slow
		if (demaCross && calculatedSlowTEMA > calculatedThirdTEMA) {
			double percentageIncrease = (alarm.getMinGap() / 100) * calculatedThirdTEMA;
			double incrisedThirdTEMA = calculatedThirdTEMA + percentageIncrease;

			if (calculatedSlowTEMA > incrisedThirdTEMA) {
//				telegramBot.sendMessage("DEMA Alert " + alarm.getSymbol() + " (" + alarm.getIntervall() + ")\n"
//					   + "DEMA " + alarm.getSecondDema() + " UP crossed " + alarm.getThirdDema()
//					   + "\nGap: " + alarm.getMinGap());

				al.setCrosss(false);
			}

		} else if (!demaCross && calculatedSlowTEMA < calculatedThirdTEMA) {
			double percentageIncrease = (alarm.getMinGap() / 100) * calculatedThirdTEMA;
			double decrisedThirdTEMA = calculatedThirdTEMA - percentageIncrease;

			if (calculatedSlowTEMA < decrisedThirdTEMA) {
//				telegramBot.sendMessage("DEMA Alert - " + alarm.getSymbol() + " (" + alarm.getIntervall() + ")\n"
//					   + "DEMA " + alarm.getSecondDema() + " DOWN crossed " + alarm.getThirdDema()
//					   + "\nGap: " + alarm.getMinGap());

				al.setCrosss(true);
			}
		}

		AlarmDB.editAlarm(al);
	}

	private List<Double> getKlinePrices() {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", alarm.getSymbol());
		parameters.put("interval", alarm.getIntervall());
		parameters.put("limit", alarm.getThirdDema() * 3 + 1);
		List<Double> priceList = new ArrayList<>();

		String result = "";

		if (alarm.getChartMode() == ChartMode.SPOT) {
			result = spotClientImpl.createMarket().klines(parameters);
		} else if (alarm.getChartMode() == ChartMode.FUTURES) {
			result = umFuturesClientImpl.market().klines(parameters);
		}

		JSONArray jsonArray = new JSONArray(result);
		jsonArray.remove(jsonArray.length() - 1);

		for (int i = 0; i < jsonArray.length(); i++) {
			JSONArray candlestick = jsonArray.getJSONArray(i);
			double newPrice = Double.parseDouble(candlestick.getString(4));
			priceList.add(newPrice);
		}
		return priceList;
	}

	private double getLatestPrice() {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", alarm.getSymbol());
		parameters.put("interval", alarm.getIntervall());
		parameters.put("limit", 2);

		String result = "";

		if (alarm.getChartMode() == ChartMode.SPOT) {
			result = spotClientImpl.createMarket().klines(parameters);
		} else if (alarm.getChartMode() == ChartMode.FUTURES) {
			result = umFuturesClientImpl.market().klines(parameters);
		}

		JSONArray jsonArray = new JSONArray(result);

		JSONArray innerArray = jsonArray.getJSONArray(0);

		double newPrice = Double.parseDouble(innerArray.getString(4));
		try {
			Alarm al = AlarmDB.getOneAlarm(alarm.getId());
			al.setLastClosingCandle(newPrice);
			AlarmDB.editAlarm(al);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return newPrice;
	}

	// set last candle on init
	private void setLastCandle(double lastCandle) {
		try {
			Alarm al = AlarmDB.getOneAlarm(alarm.getId());
			al.setLastClosingCandle(lastCandle);
			AlarmDB.editAlarm(al);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

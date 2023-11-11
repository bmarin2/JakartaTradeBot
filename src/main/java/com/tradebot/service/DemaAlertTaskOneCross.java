package com.tradebot.service;

import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.tradebot.binance.UMFuturesClientConfig;
import com.tradebot.db.AlarmDB;
import com.tradebot.model.Alarm;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.json.JSONArray;

public class DemaAlertTaskOneCross implements Runnable {

	private Alarm alarm;
	private UMFuturesClientImpl umFuturesClientImpl;
	private final TelegramBot telegramBot;

	private double secondEma;
	private double thirdEma;

	private double secondDema;
	private double thirdDema;

	private final double multiplierSecondDema;
	private final double multiplierThirdDema;

	public DemaAlertTaskOneCross(Alarm alarm) throws Exception {
		umFuturesClientImpl = UMFuturesClientConfig.futuresSignedTest();
		this.telegramBot = new TelegramBot();
		this.alarm = alarm;
		this.secondEma = 0;
		this.thirdEma = 0;
		this.secondDema = 0;
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

		thirdEma = thirdEma / (double) dema3;
		thirdDema = thirdEma;

		if (secondEma < thirdEma) {
			Alarm al = AlarmDB.getOneAlarm(alarm.getId());
			al.setCrosss(true);
			AlarmDB.editAlarm(al);
		}
		
		for (Double secondListPrice : secondList) {
			update(secondListPrice, false);
		}

	}

	public void update(double newPrice, boolean setCross) throws Exception {

		secondEma = (newPrice - secondEma) * multiplierSecondDema + secondEma;
		thirdEma = (newPrice - thirdEma) * multiplierThirdDema + thirdEma;

		// EMA of EMA
		secondDema = (secondEma - secondDema) * multiplierSecondDema + secondDema;
		thirdDema = (thirdEma - thirdDema) * multiplierThirdDema + thirdDema;
		
		if (setCross) {
			double calculatedSlowDEMA = (2 * secondEma) - secondDema;
			double calculatedThirdDEMA = (2 * thirdEma) - thirdDema;

			Alarm al = AlarmDB.getOneAlarm(alarm.getId());

			al.setCurrentSecondDema(calculatedSlowDEMA);
			al.setCurrentThirdDema(calculatedThirdDEMA);

			boolean demaCross = al.getCrosss();

			// for fast crossing slow
			if (demaCross && calculatedSlowDEMA > calculatedThirdDEMA) {

				telegramBot.sendMessage("DEMA Alert " + alarm.getSymbol() + " (" + alarm.getIntervall() + ")\n"
					   + "DEMA " + alarm.getSecondDema() + " UP crossed " + alarm.getThirdDema());

				al.setCrosss(false);

			} else if (!demaCross && calculatedSlowDEMA < calculatedThirdDEMA) {

				telegramBot.sendMessage("DEMA Alert - " + alarm.getSymbol() + " (" + alarm.getIntervall() + ")\n"
					   + "DEMA " + alarm.getSecondDema() + " DOWN crossed " + alarm.getThirdDema());

				al.setCrosss(true);
			}

			AlarmDB.editAlarm(al);
		}
	}

	private List<Double> getKlinePrices() {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", alarm.getSymbol());
		parameters.put("interval", alarm.getIntervall());
		parameters.put("limit", alarm.getThirdDema() * 3 + 1);
		List<Double> priceList = new ArrayList<>();

		String result = umFuturesClientImpl.market().klines(parameters);

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

		String result = umFuturesClientImpl.market().klines(parameters);

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
}

package com.tradebot.service;

import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.tradebot.binance.UMFuturesClientConfig;
import com.tradebot.db.AlarmDB;
import com.tradebot.model.Alarm;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.json.JSONArray;

public class DemaAlertTask implements Runnable {

      private Alarm alarm;
      private UMFuturesClientImpl umFuturesClientImpl;
      private final TelegramBot telegramBot;

      private double firstEma;
      private double secondEma;
      private double thirdEma;

      private double firstDema;
      private double secondDema;
      private double thirdDema;

      private final double multiplierFirstDema;
      private final double multiplierSecondDema;
      private final double multiplierThirdDema;

      public DemaAlertTask(Alarm alarm) throws Exception {
            umFuturesClientImpl = UMFuturesClientConfig.futuresSignedTest();
            this.telegramBot = new TelegramBot();
            this.alarm = alarm;
            this.firstEma = 0;
            this.secondEma = 0;
            this.thirdEma = 0;
            this.firstDema = 0;
            this.secondDema = 0;
            this.multiplierFirstDema = 2.0 / (double) (alarm.getFirstDema() + 1);
            this.multiplierSecondDema = 2.0 / (double) (alarm.getSecondDema() + 1);
            this.multiplierThirdDema = 2.0 / (double) (alarm.getThirdDema() + 1);
            resetCross();
            init(getKlinePrices(), alarm.getFirstDema(), alarm.getSecondDema(), alarm.getThirdDema());
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
            al.setCrosssBig(false);
            AlarmDB.editAlarm(al);
      }

      private void init(List<Double> prices, Integer dema1, Integer dema2, Integer dema3) throws Exception {
            int startIndexFirst = prices.size() - dema1;
            int startIndexSecond = prices.size() - dema2;

            for (int i = startIndexFirst; i < dema3; i++) {
                  firstEma += prices.get(i);
            }

            for (int i = startIndexSecond; i < dema3; i++) {
                  secondEma += prices.get(i);
            }

            for (int i = 0; i < dema3; i++) {
                  thirdEma += prices.get(i);
            }

            firstEma = firstEma / (double) dema1;
            firstDema = firstEma;

            secondEma = secondEma / (double) dema2;
            secondDema = secondEma;

            thirdEma = thirdEma / (double) dema3;
            thirdDema = thirdEma;

            if (firstEma < secondEma) {
                  Alarm al = AlarmDB.getOneAlarm(alarm.getId());
                  al.setCrosss(true);
                  AlarmDB.editAlarm(al);
            }
            if (firstEma < thirdEma) {
                  Alarm al = AlarmDB.getOneAlarm(alarm.getId());
                  al.setCrosssBig(true);
                  AlarmDB.editAlarm(al);
            }
      }

      private List<Double> getKlinePrices() {
            LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("symbol", alarm.getSymbol());
            parameters.put("interval", alarm.getIntervall());
            parameters.put("limit", alarm.getThirdDema() + 1);
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

      public void update(double newPrice) throws Exception {
            firstEma = (newPrice - firstEma) * multiplierFirstDema + firstEma;
            secondEma = (newPrice - secondEma) * multiplierSecondDema + secondEma;
            thirdEma = (newPrice - thirdEma) * multiplierThirdDema + thirdEma;

            // EMA of EMA
            firstDema = (firstEma - firstDema) * multiplierFirstDema + firstDema;
            secondDema = (secondEma - secondDema) * multiplierSecondDema + secondDema;
            thirdDema = (thirdEma - thirdDema) * multiplierThirdDema + thirdDema;

            double calculatedFastDEMA = (2 * firstEma) - firstDema;
            double calculatedSlowDEMA = (2 * secondEma) - secondDema;
            double calculatedThirdDEMA = (2 * thirdEma) - thirdDema;

            Alarm al = AlarmDB.getOneAlarm(alarm.getId());

            al.setCurrentFirstDema(calculatedFastDEMA);
            al.setCurrentSecondDema(calculatedSlowDEMA);
            al.setCurrentThirdDema(calculatedThirdDEMA);

            boolean demaCross = al.getCrosss();
            boolean demaCrossBig = al.getCrosssBig();

            // for fast crossing slow
            if (demaCross && calculatedFastDEMA > calculatedSlowDEMA) {
                  telegramBot.sendMessage("DEMA Alert " + alarm.getSymbol() + " (" + alarm.getIntervall() + ")\n"
                          + "DEMA " + alarm.getFirstDema() + " UP crossed " + alarm.getSecondDema());

                  al.setCrosss(false);

            } else if (!demaCross && calculatedFastDEMA < calculatedSlowDEMA) {
                  telegramBot.sendMessage("DEMA Alert - " + alarm.getSymbol() + " (" + alarm.getIntervall() + ")\n"
                          + "DEMA " + alarm.getFirstDema() + " DOWN crossed " + alarm.getSecondDema());

                  al.setCrosss(true);
            }

            // for fast crossing 200
            if (demaCrossBig && calculatedFastDEMA > calculatedThirdDEMA) {
                  telegramBot.sendMessage("DEMA Alert " + alarm.getSymbol() + " (" + alarm.getIntervall() + ")\n"
                          + "DEMA " + alarm.getFirstDema() + " UP crossed " + alarm.getThirdDema());

                  al.setCrosssBig(false);

            } else if (!demaCrossBig && calculatedFastDEMA < calculatedThirdDEMA) {
                  telegramBot.sendMessage("DEMA Alert - " + alarm.getSymbol() + " (" + alarm.getIntervall() + ")\n"
                          + "DEMA " + alarm.getFirstDema() + " DOWN crossed " + alarm.getThirdDema());

                  al.setCrosssBig(true);
            }

            AlarmDB.editAlarm(al);
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

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

public class TemaTwoCrossTask implements Runnable {

     private Alarm alarm;
     private UMFuturesClientImpl umFuturesClientImpl;
     private SpotClientImpl spotClientImpl;
     private final TelegramBot telegramBot;

     private double firstEma;
     private double secondEma;
     private double thirdEma;

     private double firstDema;
     private double secondDema;
     private double thirdDema;
     
     private double firstTema;
     private double secondTema;
     private double thirdTema;

     private final double multiplierFirstDema;
     private final double multiplierSecondDema;
     private final double multiplierThirdDema;

     public TemaTwoCrossTask(Alarm alarm) throws Exception {
          if (alarm.getChartMode() == ChartMode.SPOT) {
               spotClientImpl = SpotClientConfig.spotClientOnlyBaseURLProd();
          } else if (alarm.getChartMode() == ChartMode.FUTURES) {
               umFuturesClientImpl = UMFuturesClientConfig.futuresSignedTest();
          }
          this.telegramBot = new TelegramBot();
          this.alarm = alarm;
          this.firstEma = 0;
          this.secondEma = 0;
          this.thirdEma = 0;
          this.firstDema = 0;
          this.secondDema = 0;

          this.firstTema = 0;
          this.secondTema = 0;
          this.thirdTema = 0;

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
               update(latest, true);

          } catch (Exception e) {
               e.printStackTrace();
          }
     }

     private void resetCross() throws Exception {
          Alarm al = AlarmDB.getOneAlarm(alarm.getId());
          al.setCrosss(false);
          al.setCrosssBig(false);
          al.setCurrentFirstDema(0.0);
          al.setCurrentSecondDema(0.0);
          al.setCurrentThirdDema(0.0);
          al.setLastClosingCandle(0.0);
          AlarmDB.editAlarm(al);
     }

     private void init(List<Double> prices, Integer dema1, Integer dema2, Integer dema3) throws Exception {

          List<Double> firstList = prices.subList(0, dema3);
          List<Double> secondList = prices.subList(dema3, prices.size());

          int startIndexFirst = firstList.size() - dema1;
          int startIndexSecond = firstList.size() - dema2;
          int startIndexThird = firstList.size() - dema3;

          for (int i = startIndexFirst; i < dema3; i++) {
               firstEma += firstList.get(i);
          }

          for (int i = startIndexSecond; i < dema3; i++) {
               secondEma += firstList.get(i);
          }

          for (int i = startIndexThird; i < dema3; i++) {
               thirdEma += firstList.get(i);
          }

          // calculate SMAs
          firstEma = firstEma / (double) dema1;
          firstDema = firstEma;
          firstTema = firstEma;

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

          firstEma = (newPrice - firstEma) * multiplierFirstDema + firstEma;
          secondEma = (newPrice - secondEma) * multiplierSecondDema + secondEma;
          thirdEma = (newPrice - thirdEma) * multiplierThirdDema + thirdEma;

          // EMA of EMA
          firstDema = (firstEma - firstDema) * multiplierFirstDema + firstDema;
          secondDema = (secondEma - secondDema) * multiplierSecondDema + secondDema;
          thirdDema = (thirdEma - thirdDema) * multiplierThirdDema + thirdDema;
          
          firstTema = (firstDema - firstTema) * multiplierFirstDema + firstTema;
          secondTema = (secondDema - secondTema) * multiplierSecondDema + secondTema;
          thirdTema = (thirdDema - thirdTema) * multiplierThirdDema + thirdTema;

          if (setCross) {
               calculateDemas();
          }
     }

     private void calculateDemas() throws Exception {          
          double calculatedFastTEMA = (3*firstEma) - (3*firstDema) + firstTema;
          double calculatedSlowTEMA = (3*secondEma) - (3*secondDema) + secondTema;
          double calculatedThirdTEMA = (3*thirdEma) - (3*thirdDema) + thirdTema;          

          Alarm al = AlarmDB.getOneAlarm(alarm.getId());

          al.setCurrentFirstDema(calculatedFastTEMA);
          al.setCurrentSecondDema(calculatedSlowTEMA);
          al.setCurrentThirdDema(calculatedThirdTEMA);

          boolean demaCross = al.getCrosss();
          boolean demaCrossBig = al.getCrosssBig();

          // for crossing first and second tema
          if (demaCross && calculatedFastTEMA > calculatedSlowTEMA) {
               double percentageIncrease = (alarm.getMinGap() / 100) * calculatedSlowTEMA;
               double incrisedSlowDEMA = calculatedSlowTEMA + percentageIncrease;

               if (calculatedFastTEMA > incrisedSlowDEMA) {
//                        telegramBot.sendMessage("DEMA Alert " + alarm.getSymbol() + " (" + alarm.getIntervall() + ")\n"
//                                + "DEMA " + alarm.getFirstDema() + " UP crossed " + alarm.getSecondDema()
//                                + "\nGap: " + alarm.getMinGap());

                    al.setCrosss(false);
               }

          } else if (!demaCross && calculatedFastTEMA < calculatedSlowTEMA) {
               double percentageIncrease = (alarm.getMinGap() / 100) * calculatedSlowTEMA;
               double decrisedSlowDEMA = calculatedSlowTEMA - percentageIncrease;

               if (calculatedFastTEMA < decrisedSlowDEMA) {
//                        telegramBot.sendMessage("DEMA Alert - " + alarm.getSymbol() + " (" + alarm.getIntervall() + ")\n"
//                                + "DEMA " + alarm.getFirstDema() + " DOWN crossed " + alarm.getSecondDema()
//                                + "\nGap: " + alarm.getMinGap());

                    al.setCrosss(true);
               }
          }

          // for first crossing 200
          if (demaCrossBig && calculatedFastTEMA > calculatedThirdTEMA) {
               double percentageIncrease = (alarm.getMinGap() / 100) * calculatedThirdTEMA;
               double incrisedThirdTEMA = calculatedThirdTEMA + percentageIncrease;

               if (calculatedFastTEMA > incrisedThirdTEMA) {
//                        telegramBot.sendMessage("DEMA Alert " + alarm.getSymbol() + " (" + alarm.getIntervall() + ")\n"
//                                + "DEMA " + alarm.getFirstDema() + " UP crossed " + alarm.getThirdDema()
//                                + "\nGap: " + alarm.getMinGap());

                    al.setCrosssBig(false);
               }

          } else if (!demaCrossBig && calculatedFastTEMA < calculatedThirdTEMA) {
               double percentageIncrease = (alarm.getMinGap() / 100) * calculatedThirdTEMA;
               double decrisedThirdTEMA = calculatedThirdTEMA - percentageIncrease;

               if (calculatedFastTEMA < decrisedThirdTEMA) {
//                        telegramBot.sendMessage("DEMA Alert - " + alarm.getSymbol() + " (" + alarm.getIntervall() + ")\n"
//                                + "DEMA " + alarm.getFirstDema() + " DOWN crossed " + alarm.getThirdDema()
//                                + "\nGap: " + alarm.getMinGap());

                    al.setCrosssBig(true);
               }
          }

          AlarmDB.editAlarm(al);
     }

     private List<Double> getKlinePrices() {
          LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
          parameters.put("symbol", alarm.getSymbol());
          parameters.put("interval", alarm.getIntervall());
          parameters.put("limit", alarm.getThirdDema() * 4 + 1);
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

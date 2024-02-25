package com.tradebot.service;

import com.binance.connector.client.impl.SpotClientImpl;
import com.binance.connector.futures.client.exceptions.BinanceClientException;
import com.binance.connector.futures.client.exceptions.BinanceConnectorException;
import com.binance.connector.futures.client.exceptions.BinanceServerException;
import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.tradebot.binance.SpotClientConfig;
import com.tradebot.binance.UMFuturesClientConfig;
import com.tradebot.configuration.FuturesOrderParams;
import com.tradebot.db.MACDAlarmDB;
import com.tradebot.model.Alarm;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.StochasticRSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.SMAIndicator;

public class StochRsiEma implements Runnable {

	private Alarm alarm;
	private UMFuturesClientImpl umFuturesClientImpl;
	private SpotClientImpl spotClientImpl;
	private final TelegramBot telegramBot;

	private BarSeries series;

	public StochRsiEma(Alarm alarm) {
		this.alarm = alarm;
		initChartMode(alarm);		
		telegramBot = new TelegramBot();		
		series = new BaseBarSeriesBuilder().withName("mySeries").build();
          series.setMaximumBarCount(100);
          updateValues(true);
          calculateValues();
	}

	@Override
	public void run() {
          updateValues(false);
          calculateValues();
	}

     private void updateValues(boolean firstTime) {
          if (firstTime) {
               fetchBarSeries(alarm.getThirdDema() * 2);
          } else {
               fetchBarSeries(1);
          }

		ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);

		RSIIndicator r = new RSIIndicator(closePriceIndicator, 14);
		Indicator sr = new StochasticRSIIndicator(r, 14);
		Indicator k = new SMAIndicator(sr, 3); // blue
		Indicator d = new SMAIndicator(k, 3); // yellow
		
		
		EMAIndicator ema1 = new EMAIndicator(closePriceIndicator, alarm.getFirstDema());
		System.out.println("ema1 close: " + ema1.getValue(ema1.getBarSeries().getEndIndex()));
		
		EMAIndicator ema2 = new EMAIndicator(closePriceIndicator, alarm.getSecondDema());
		System.out.println("ema2 close: " + ema2.getValue(ema2.getBarSeries().getEndIndex()));
		
		EMAIndicator ema3 = new EMAIndicator(closePriceIndicator, alarm.getThirdDema());
		System.out.println("ema3 close: " + ema3.getValue(ema3.getBarSeries().getEndIndex()));
		
		double atr = new ATRIndicator(series, 14).getValue(series.getEndIndex()).doubleValue();
		System.out.println("atr: " + atr);
		
		System.out.println("Last Candle: " + closePriceIndicator.getValue(closePriceIndicator
				.getBarSeries().getEndIndex()).doubleValue());
		
		System.out.println("k " + k.getValue(k.getBarSeries().getEndIndex()));
		System.out.println("d " + d.getValue(d.getBarSeries().getEndIndex()));

//          macdAlarm.setCurrentMacdLine(
//                  macdIndicator.getValue(macdIndicator.getBarSeries().getEndIndex()).doubleValue()
//          );
//          macdAlarm.setCurrentSignalLine(
//                  new EMAIndicator(macdIndicator, 9).getValue(macdIndicator.getBarSeries().getEndIndex()).doubleValue()
//          );          
//          macdAlarm.setCurrentEma(
//                  new EMAIndicator(closePriceIndicator, macdAlarm.getEma()).getValue(closePriceIndicator.getBarSeries().getEndIndex()).doubleValue()          
//          );          
//          macdAlarm.setLastClosingCandle(closePriceIndicator.getValue(closePriceIndicator.getBarSeries().getEndIndex()).doubleValue());
//
//		macdAlarm.setLastAtr(new ATRIndicator(series, 14).getValue(series.getEndIndex()).doubleValue());

		try {
			System.out.println("get cross:  " + MACDAlarmDB.getMacdCross(alarm.getAlarmId()));
		} catch (Exception ex) {
			Logger.getLogger(MACDCross.class.getName()).log(Level.SEVERE, null, ex);
		}
          System.out.println("--------------------------");
     }

     private void calculateValues() {
//          System.out.println("Calculating values:");
//          System.out.println("--");
//
//          if (macdAlarm.getMacdCrosss() && macdAlarm.getCurrentMacdLine() > macdAlarm.getCurrentSignalLine()) {
//
//			double percentageIncrease = (macdAlarm.getMinGap() / 100) * macdAlarm.getCurrentSignalLine();
//			double incrisedSignalLine = macdAlarm.getCurrentSignalLine() + percentageIncrease;
//               
//			if (macdAlarm.getCurrentMacdLine() > incrisedSignalLine) {
//				macdAlarm.setMacdCrosss(false);
//				System.out.println("Macd Crosss is now false");
//
//				if (macdAlarm.getCurrentMacdLine() > 0 && macdAlarm.getCurrentSignalLine() > 0 && isPriceAboveEmaLine()) {
//					macdAlarm.setGoodForEntry(true);
//					System.out.println("Good for entry");
//					try {
//						telegramBot.sendMessage("Good for entry LONG");
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//				} else {
//					macdAlarm.setGoodForEntry(false);
//					System.out.println("Not good for entry");
//				}
//			}
//
//          } else if (!macdAlarm.getMacdCrosss() && macdAlarm.getCurrentMacdLine() < macdAlarm.getCurrentSignalLine()) {
//			
//			double percentageIncrease = (macdAlarm.getMinGap() / 100) * macdAlarm.getCurrentSignalLine();
//			double incrisedSignalLine = macdAlarm.getCurrentSignalLine() - percentageIncrease;
//               
//			if (macdAlarm.getCurrentMacdLine() < incrisedSignalLine) {
//				macdAlarm.setMacdCrosss(true);
//				System.out.println("Macd Crosss is now true");
//
//				if (macdAlarm.getCurrentMacdLine() < 0 && macdAlarm.getCurrentSignalLine() < 0 && !isPriceAboveEmaLine()) {
//					macdAlarm.setGoodForEntry(true);
//					System.out.println("Good for entry");
//					try {
//						telegramBot.sendMessage("Good for entry SHORT");
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//				} else {
//					macdAlarm.setGoodForEntry(false);
//					System.out.println("Not good for entry");
//				}
//			}
//		}
//
//          try {
//               MACDAlarmDB.editAlarm(macdAlarm);
//          } catch (Exception ex) {
//               ex.printStackTrace();
//          }
     }

	private void fetchBarSeries(int limit) {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", alarm.getSymbol());
		parameters.put("interval", alarm.getIntervall());
		parameters.put("limit", limit + 1);

		String result = "";
          
          try {
               if (alarm.getChartMode().name().startsWith("SPOT")) {
                    result = spotClientImpl.createMarket().klines(parameters);
               } else if (alarm.getChartMode().name().startsWith("FUTURES")) {
                    result = umFuturesClientImpl.market().klines(parameters);
               }
          } catch (BinanceConnectorException e) {
               sendErrorMsg("BinanceConnectorException", e.getMessage());
               e.printStackTrace();
          } catch (BinanceClientException e) {
			sendErrorMsg("BinanceClientException", e.getMessage());
               e.printStackTrace();
          } catch (BinanceServerException e) {
			sendErrorMsg("BinanceServerException", e.getMessage());
               e.printStackTrace();
          }
          
		JSONArray jsonArray = new JSONArray(result);
		jsonArray.remove(jsonArray.length() - 1);
          
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONArray candlestick = jsonArray.getJSONArray(i);

			long unixTimestampMillis = candlestick.getLong(6);
			Instant instant = Instant.ofEpochMilli(unixTimestampMillis);
			ZonedDateTime utcZonedDateTime = instant.atZone(ZoneId.of("UTC"));
               try {
                    series.addBar(utcZonedDateTime,
                            candlestick.getString(1),
                            candlestick.getString(2),
                            candlestick.getString(3),
                            candlestick.getString(4),
                            candlestick.getString(5)
                    );
               } catch (IllegalArgumentException iae) {
                    ZonedDateTime timePlusOneMin = series.getLastBar().getEndTime().plusMinutes(1);
                    series.addBar(timePlusOneMin,
                            candlestick.getString(1),
                            candlestick.getString(2),
                            candlestick.getString(3),
                            candlestick.getString(4),
                            candlestick.getString(5)
                    );
               }
		}
	}

     private Double fetchTickerPrice() {
          String result = "";

          try {
               result = umFuturesClientImpl.market().tickerSymbol(
                       FuturesOrderParams.getTickerParams(alarm.getSymbol())
               );
          } catch (BinanceConnectorException e) {
               sendErrorMsg("BinanceConnectorException", e.getMessage());
               e.printStackTrace();
          } catch (BinanceClientException e) {
               sendErrorMsg("BinanceClientException", e.getMessage());
               e.printStackTrace();
          } catch (BinanceServerException e) {
               sendErrorMsg("BinanceServerException", e.getMessage());
               e.printStackTrace();
          }
          JSONObject jsonResult = new JSONObject(result);
          return jsonResult.optDouble("price");
     }

//     private boolean isPriceAboveEmaLine() {
//          double currentEma = new EMAIndicator(this.closePriceIndicator, alarm.getEma())
//                  .getValue(this.closePriceIndicator.getBarSeries().getEndIndex()).doubleValue();
//          double currentPrice = fetchTickerPrice();
//
//          return currentPrice > currentEma;
//     }
	
	private void initChartMode(Alarm alarm) {
		switch (alarm.getChartMode()) {
			case SPOT_BASE_URL_PROD:
				spotClientImpl = SpotClientConfig.spotClientOnlyBaseURLProd();
				break;
			case SPOT_BASE_URL_TEST:
				spotClientImpl = SpotClientConfig.spotClientOnlyBaseURLTest();
				break;
			case SPOT_SIGNED_PROD:
				spotClientImpl = SpotClientConfig.spotClientSignProd();
				break;
			case SPOT_SIGNED_TEST:
				spotClientImpl = SpotClientConfig.spotClientSignTest();
				break;
			case FUTURES_BASE_URL_PROD:
				umFuturesClientImpl = UMFuturesClientConfig.futuresBaseURLProd();
				break;
			case FUTURES_BASE_URL_TEST:
				umFuturesClientImpl = UMFuturesClientConfig.futuresBaseURLTest();
				break;
			case FUTURES_SIGNED_PROD:
				umFuturesClientImpl = UMFuturesClientConfig.futuresSignedProd();
				break;
			case FUTURES_SIGNED_TEST:
				umFuturesClientImpl = UMFuturesClientConfig.futuresSignedTest();
				break;
			default:
				break;
		}
	}

     private void sendErrorMsg(String type, String msg) {
          try {
               telegramBot.sendMessage("Futures MACD Cross task exception " + alarm.getSymbol() + "\n"
                       + type + "\n" + msg);
          } catch (Exception e) {
               e.printStackTrace();
          }
     }
}

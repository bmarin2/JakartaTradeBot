package com.tradebot.service;

import com.binance.connector.client.impl.SpotClientImpl;
import com.binance.connector.futures.client.exceptions.BinanceClientException;
import com.binance.connector.futures.client.exceptions.BinanceConnectorException;
import com.binance.connector.futures.client.exceptions.BinanceServerException;
import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.tradebot.binance.SpotClientConfig;
import com.tradebot.binance.UMFuturesClientConfig;
import com.tradebot.configuration.FuturesOrderParams;
import com.tradebot.db.AlarmDB;
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
	private long lastTimestamp;
	
	private double K;
	private double D;

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
		SMAIndicator k = new SMAIndicator(sr, 3); // blue
		SMAIndicator d = new SMAIndicator(k, 3); // yellow
		
		K = k.getValue(k.getBarSeries().getEndIndex()).doubleValue();
		D = d.getValue(k.getBarSeries().getEndIndex()).doubleValue();
		
		System.out.println("K: " + K);
		System.out.println("D: " + D);
		
		System.out.println("Last Candle: " + closePriceIndicator.getValue(closePriceIndicator
				.getBarSeries().getEndIndex()).doubleValue());

		EMAIndicator ema1 = new EMAIndicator(closePriceIndicator, alarm.getFirstDema());
		alarm.setCurrentFirstDema(ema1.getValue(ema1.getBarSeries().getEndIndex()).doubleValue());

		EMAIndicator ema2 = new EMAIndicator(closePriceIndicator, alarm.getSecondDema());
		alarm.setCurrentSecondDema(ema2.getValue(ema2.getBarSeries().getEndIndex()).doubleValue());

		EMAIndicator ema3 = new EMAIndicator(closePriceIndicator, alarm.getThirdDema());
		alarm.setCurrentThirdDema(ema3.getValue(ema3.getBarSeries().getEndIndex()).doubleValue());

		double atr = new ATRIndicator(series, 14).getValue(series.getEndIndex()).doubleValue();
		alarm.setAtr(atr);

		alarm.setLastClosingCandle(closePriceIndicator.getValue(closePriceIndicator
				.getBarSeries().getEndIndex()).doubleValue());

		try {
			System.out.println("get cross:  " + AlarmDB.getAlarmCross(alarm.getAlarmId()));
		} catch (Exception ex) {
			Logger.getLogger(MACDCross.class.getName()).log(Level.SEVERE, null, ex);
		}
          System.out.println("--------------------------");
     }

     private void calculateValues() {
		if (alarm.getCrosss() && K > D) {

			double increasedD = D + alarm.getMinGap();

			if (K > increasedD) {
				
				alarm.setCrosss(false);
				System.out.println("*** Cross is now FALSE ***\n");
				
				if (emasSetForLong() && (K < 0.2 || D < 0.2)) {
					alarm.setGoodForEntry(true);
					System.out.println("Good for entry long");

					try {
						telegramBot.sendMessage("Good for entry LONG");
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					alarm.setGoodForEntry(false);
				}
			}
          } else if (!alarm.getCrosss() && K < D) {

			double decreasedD = D - alarm.getMinGap();

			if (K < decreasedD) {
				alarm.setCrosss(true);
				System.out.println("*** Cross is now TRUE ***\n");

				if (emasSetForShort() && (K > 0.8 || D > 0.8)) {
					alarm.setGoodForEntry(true);
					System.out.println("Good for entry short");

					try {
						telegramBot.sendMessage("Good for entry SHORT");
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					alarm.setGoodForEntry(false);
				}
			}
		}

          try {
               AlarmDB.editAlarm(alarm);
          } catch (Exception ex) {
               ex.printStackTrace();
          }
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
		
		System.out.println("old timestamp: " + lastTimestamp);
		
		if (limit == 1) {
			JSONArray array = jsonArray.getJSONArray(0);
			long timestamp = array.getLong(6);
			System.out.println("new timestamp limit 1: " + timestamp);
			
			if (lastTimestamp == timestamp) {
				System.out.println("/// same value skipping");
				return;
			} else {
				System.out.println("*** updating timestamp");
				lastTimestamp = timestamp;
			}
		} else {
			JSONArray array2 = jsonArray.getJSONArray(jsonArray.length() - 1);
			long timestamp2 = array2.getLong(6);
			System.out.println("first timestamp: " + timestamp2);
			lastTimestamp = timestamp2;
		}
		
		System.out.println("new timestamp: " + lastTimestamp);
		
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

	private boolean emasSetForLong() {
		return alarm.getCurrentFirstDema() > alarm.getCurrentSecondDema()
			&& alarm.getCurrentSecondDema() > alarm.getCurrentThirdDema();
	}
	
	private boolean emasSetForShort() {
		return alarm.getCurrentFirstDema() < alarm.getCurrentSecondDema()
			&& alarm.getCurrentSecondDema() < alarm.getCurrentThirdDema();
	}
	
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

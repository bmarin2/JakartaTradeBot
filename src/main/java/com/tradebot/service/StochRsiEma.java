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
import org.json.JSONArray;
import org.json.JSONObject;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.StochasticRSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.adx.ADXIndicator;

public class StochRsiEma implements Runnable {

	private Alarm alarm;
	private UMFuturesClientImpl umFuturesClientImpl;
	private SpotClientImpl spotClientImpl;
	private final TelegramBot telegramBot;
	private long lastTimestamp;
//	private long lastTimestampAdx5min;
//	private int counter;
	private boolean firstTime = true;
	
	private double K;
	private double D;
//	private double adx;
//	private double ema200Adx;
	private boolean aboveAdxLine;
	private boolean isRising;
	private double lastAdxValue;

	private BarSeries series;
//	private BarSeries seriesAdx5min;

	public StochRsiEma(Alarm alarm) {
		this.alarm = alarm;
		initChartMode(alarm);	
		telegramBot = new TelegramBot();		
		series = new BaseBarSeriesBuilder().withName("mySeries").build();
          series.setMaximumBarCount(alarm.getThirdDema() * 2);
//		seriesAdx5min = new BaseBarSeriesBuilder().withName("mySeriesAdx5min").build();
//		seriesAdx5min.setMaximumBarCount(400);
		runner();
          calculateValues();
	}

	@Override
	public void run() {
		runner();
	}
	
	private void runner() {
		if (firstTime) {
			fetchBarSeries(alarm.getThirdDema() * 2);
			firstTime = false;
		} else {
			fetchBarSeries(1);
		}
	}

     private void updateValues() {
		ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);

		Indicator sr = new StochasticRSIIndicator(series, 14);
		SMAIndicator k = new SMAIndicator(sr, 3); // blue
		SMAIndicator d = new SMAIndicator(k, 3); // yellow		
		
		K = k.getValue(k.getBarSeries().getEndIndex()).doubleValue();
		D = d.getValue(k.getBarSeries().getEndIndex()).doubleValue();
		
		ADXIndicator adxIndicator = new ADXIndicator(series, 14);
		double currentAdx = adxIndicator.getValue(adxIndicator.getBarSeries().getEndIndex()).doubleValue();
		
		System.out.println("K:   " + K);
		System.out.println("D:   " + D);
		System.out.println("ADX: " + currentAdx);
		
		if (!aboveAdxLine && currentAdx > 21) {
			aboveAdxLine = true;
		} else if (aboveAdxLine && currentAdx < 21) {
			aboveAdxLine = false;
		}
		
		if (currentAdx > lastAdxValue) {
			isRising = true;
			System.out.println("ADX Rising");
		} else if (currentAdx < lastAdxValue) {
			System.out.println("ADX Faling");
			isRising = false;
		}
		
		lastAdxValue = currentAdx;

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

          System.out.println("--------------------------");
     }
	
//	private void updateAdxValues() {
//		ADXIndicator adxIndicator = new ADXIndicator(seriesAdx5min, 14);
//		adx = adxIndicator.getValue(adxIndicator.getBarSeries().getEndIndex()).doubleValue();
//
//		ClosePriceIndicator closePriceAdx5min = new ClosePriceIndicator(seriesAdx5min);
//		EMAIndicator ema5min200 = new EMAIndicator(closePriceAdx5min, 200);
//		ema200Adx = ema5min200.getValue(ema5min200.getBarSeries().getEndIndex()).doubleValue();
//	}

     private void calculateValues() {
		if (alarm.getCrosss() && K > D) {

			double increasedD = D + alarm.getMinGap();

			if (K > increasedD) {
				
				alarm.setCrosss(false);
				
				if (emasSetForLong() && isAdxSet()) {
					alarm.setGoodForEntry(true);
				} else {
					alarm.setGoodForEntry(false);
				}
			}
          } else if (!alarm.getCrosss() && K < D) {

			double decreasedD = D - alarm.getMinGap();

			if (K < decreasedD) {
				alarm.setCrosss(true);

				if (emasSetForShort() && isAdxSet()) {
					alarm.setGoodForEntry(true);
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
			return;
          } catch (BinanceClientException e) {
			sendErrorMsg("BinanceClientException", e.getMessage());
               e.printStackTrace();
			return;
          } catch (BinanceServerException e) {
			sendErrorMsg("BinanceServerException", e.getMessage());
               e.printStackTrace();
			return;
          }

		JSONArray jsonArray = new JSONArray(result);
		jsonArray.remove(jsonArray.length() - 1);

		if (limit == 1) {
			JSONArray array = jsonArray.getJSONArray(0);
			long timestamp = array.getLong(6);

			if (lastTimestamp == timestamp) {
				System.out.println("// same timestamp skipping");
				return;
			} else {
				System.out.println("** updating timestamp");
				lastTimestamp = timestamp;
			}
		} else {
			JSONArray array2 = jsonArray.getJSONArray(jsonArray.length() - 1);
			long timestamp2 = array2.getLong(6);
			System.out.println("first timestamp: " + timestamp2);
			lastTimestamp = timestamp2;
		}
		
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
		updateValues();
		calculateValues();
	}
	
//	private void fetchBarSeriesForADX(int limit) {
//		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
//		parameters.put("symbol", alarm.getSymbol());
//		parameters.put("interval", "5m");
//		parameters.put("limit", limit + 1);
//
//		String result = "";
//          
//          try {
//               if (alarm.getChartMode().name().startsWith("SPOT")) {
//                    result = spotClientImpl.createMarket().klines(parameters);
//               } else if (alarm.getChartMode().name().startsWith("FUTURES")) {
//                    result = umFuturesClientImpl.market().klines(parameters);
//               }
//          } catch (BinanceConnectorException e) {
//               sendErrorMsg("BinanceConnectorException ADX 5min", e.getMessage());
//               e.printStackTrace();
//			return;
//          } catch (BinanceClientException e) {
//			sendErrorMsg("BinanceClientException ADX 5min", e.getMessage());
//               e.printStackTrace();
//			return;
//          } catch (BinanceServerException e) {
//			sendErrorMsg("BinanceServerException ADX 5min", e.getMessage());
//               e.printStackTrace();
//			return;
//          }
//		
//		JSONArray jsonArray = new JSONArray(result);
//		jsonArray.remove(jsonArray.length() - 1);
//		
//		if (limit == 1) {
//			JSONArray array = jsonArray.getJSONArray(0);
//			long timestamp = array.getLong(6);
//			
//			if (lastTimestampAdx5min == timestamp) {
//				System.out.println("/ADX same timestamp skipping");
//				return;
//			} else {
//				System.out.println("*ADX updating timestamp ADX");
//				lastTimestampAdx5min = timestamp;
//			}
//		} else {
//			JSONArray array2 = jsonArray.getJSONArray(jsonArray.length() - 1);
//			long timestamp2 = array2.getLong(6);
//			lastTimestampAdx5min = timestamp2;
//		}
//		
//		for (int i = 0; i < jsonArray.length(); i++) {
//			JSONArray candlestick = jsonArray.getJSONArray(i);
//
//			long unixTimestampMillis = candlestick.getLong(6);
//			Instant instant = Instant.ofEpochMilli(unixTimestampMillis);
//			ZonedDateTime utcZonedDateTime = instant.atZone(ZoneId.of("UTC"));
//               try {
//                    seriesAdx5min.addBar(utcZonedDateTime,
//                            candlestick.getString(1),
//                            candlestick.getString(2),
//                            candlestick.getString(3),
//                            candlestick.getString(4),
//                            candlestick.getString(5)
//                    );
//               } catch (IllegalArgumentException iae) {
//                    ZonedDateTime timePlusOneMin = seriesAdx5min.getLastBar().getEndTime().plusMinutes(1);
//                    seriesAdx5min.addBar(timePlusOneMin,
//                            candlestick.getString(1),
//                            candlestick.getString(2),
//                            candlestick.getString(3),
//                            candlestick.getString(4),
//                            candlestick.getString(5)
//                    );
//               }
//		}
//		updateAdxValues();
//		calculateValues();
//	}

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

	private boolean emasSetForLong() {
		return alarm.getCurrentFirstDema() > alarm.getCurrentSecondDema()
			&& alarm.getCurrentSecondDema() > alarm.getCurrentThirdDema();
	}
	
	private boolean emasSetForShort() {
		return alarm.getCurrentFirstDema() < alarm.getCurrentSecondDema()
			&& alarm.getCurrentSecondDema() < alarm.getCurrentThirdDema();
	}
	
	private boolean isAdxSet() {
		return aboveAdxLine && isRising;
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
               telegramBot.sendMessage("Futures StochRSI exception " + alarm.getSymbol() + "\n"
                       + type + "\n" + msg);
          } catch (Exception e) {
               e.printStackTrace();
          }
     }
}

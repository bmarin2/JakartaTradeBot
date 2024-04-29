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
import java.util.LinkedList;
import java.util.Queue;
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
	private double ema4;
//	private boolean aboveAdxLine;
//	private boolean isRising;
//	private double lastAdxValue;

	private BarSeries series;
//	private BarSeries seriesAdx5min;
	private Queue<Double> queue;
	private int maxQueueSize = 20; // horizontal box
	private double verticalPercent = 0.09; // vertical box, calculated 2x
	private double upperLimit;
	private double lowerLimit;

	public StochRsiEma(Alarm alarm) {
		this.alarm = alarm;
		initChartMode(alarm);
		telegramBot = new TelegramBot();
		series = new BaseBarSeriesBuilder().withName("mySeries").build();
          series.setMaximumBarCount(420);
//		seriesAdx5min = new BaseBarSeriesBuilder().withName("mySeriesAdx5min").build();
//		seriesAdx5min.setMaximumBarCount(400);
		queue = new LinkedList<>();
		runner();
	}

	@Override
	public void run() {
		runner();
	}

	private void runner() {
		if (firstTime) {
			fetchBarSeries(420);
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
		
//		ADXIndicator adxIndicator = new ADXIndicator(series, 14);
//		double currentAdx = adxIndicator.getValue(adxIndicator.getBarSeries().getEndIndex()).doubleValue();
		
		System.out.println("K:   " + K);
		System.out.println("D:   " + D);
//		System.out.println("ADX: " + currentAdx);
//		System.out.println("aboveAdxLine: " + aboveAdxLine);
//		System.out.println("isRising      " + isRising);
		
//		if (!aboveAdxLine && currentAdx > 18) {
//			aboveAdxLine = true;
//		} else if (aboveAdxLine && currentAdx < 18) {
//			aboveAdxLine = false;
//		}
		
//		if (currentAdx > lastAdxValue) {
//			isRising = true;
//		} else if (currentAdx < lastAdxValue) {
//			isRising = false;
//		}
//		
//		lastAdxValue = currentAdx;

		EMAIndicator ema1 = new EMAIndicator(closePriceIndicator, alarm.getFirstDema());
		alarm.setCurrentFirstDema(ema1.getValue(ema1.getBarSeries().getEndIndex()).doubleValue());

		EMAIndicator ema2 = new EMAIndicator(closePriceIndicator, alarm.getSecondDema());
		alarm.setCurrentSecondDema(ema2.getValue(ema2.getBarSeries().getEndIndex()).doubleValue());

		EMAIndicator ema3 = new EMAIndicator(closePriceIndicator, alarm.getThirdDema());
		alarm.setCurrentThirdDema(ema3.getValue(ema3.getBarSeries().getEndIndex()).doubleValue());
		
		EMAIndicator ema4_tmp = new EMAIndicator(closePriceIndicator, 200);
		ema4 = ema4_tmp.getValue(ema4_tmp.getBarSeries().getEndIndex()).doubleValue();

		if (firstTime) {
			int candleCounter = 400;

			for (int i = 0; i < maxQueueSize; i++) {
				ClosePriceIndicator closePricesSub = new ClosePriceIndicator(series.getSubSeries(0, candleCounter));
				EMAIndicator emaTmp = new EMAIndicator(closePricesSub, 200);
				queue.offer(emaTmp.getValue(emaTmp.getBarSeries().getEndIndex()).doubleValue());
				candleCounter++;
			}
		} else {
			if (queue.size() == maxQueueSize) {
				queue.poll();
			}
			queue.offer(ema4);
		}
		
		double oldest = queue.peek();
		
		double percentValue = oldest * (verticalPercent / 100);
		upperLimit = oldest + percentValue;
		lowerLimit = oldest - percentValue;

		System.out.println("oldest " + oldest);
		System.out.println("upperLimit " + upperLimit);
		System.out.println("lowerLimit " + lowerLimit);
		System.out.println("ema4 " + ema4);

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
				// && (K < 0.5 || D < 0.5)
				if (emasSetForLong() && (K < 0.5 || D < 0.5) && (ema4 > upperLimit)) {
					alarm.setGoodForEntry(true);
				} else {
					alarm.setGoodForEntry(false);
				}
			}
          } else if (!alarm.getCrosss() && K < D) {

			double decreasedD = D - alarm.getMinGap();

			if (K < decreasedD) {
				alarm.setCrosss(true);

				if (emasSetForShort() && (K > 0.5 || D > 0.5) && (ema4 < lowerLimit)) {
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
				return;
			} else {
				lastTimestamp = timestamp;
			}
		} else {
			JSONArray array2 = jsonArray.getJSONArray(jsonArray.length() - 1);
			long timestamp2 = array2.getLong(6);
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
			&& alarm.getCurrentSecondDema() > alarm.getCurrentThirdDema()
			&& alarm.getCurrentThirdDema() > ema4;
	}
	
	private boolean emasSetForShort() {
		return alarm.getCurrentFirstDema() < alarm.getCurrentSecondDema()
			&& alarm.getCurrentSecondDema() < alarm.getCurrentThirdDema()
			&& alarm.getCurrentThirdDema() < ema4;
	}
	
//	private boolean isAdxSet() {
//		return aboveAdxLine && isRising;
//	}
	
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

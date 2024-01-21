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
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.adx.DXIndicator;
import org.ta4j.core.indicators.candles.RealBodyIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

public class ThreeBarsTask implements Runnable {

	private Alarm alarm;
	private UMFuturesClientImpl umFuturesClientImpl;
	private SpotClientImpl spotClientImpl;
	private final TelegramBot telegramBot;

	private BarSeries series;
	private Bar lastBar;
	private int counter;
	private ClosePriceIndicator closePriceIndicator;

	public ThreeBarsTask(Alarm alarm) {
		this.alarm = alarm;
		initChartMode(alarm);		
		telegramBot = new TelegramBot();		
		series = new BaseBarSeriesBuilder().withName("mySeries").build();
          series.setMaximumBarCount(alarm.getFirstDema() * 2);
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
               fetchBarSeries(alarm.getFirstDema() * 2);
          } else {
               fetchBarSeries(1);
          }

          closePriceIndicator = new ClosePriceIndicator(series);


     }

     private void calculateValues() {
          System.out.println("Calculating candle values:");
          System.out.println("--");
		
		Bar lastBar = series.getLastBar();
		Bar beforeLastBar = series.getBar(series.getEndIndex() - 1);
		
		System.out.println("l close:     " + lastBar.getClosePrice());
		System.out.println("isBullish: " + lastBar.isBullish());
		System.out.println("isBearish: " + lastBar.isBearish());
		RealBodyIndicator realBodyIndicator = new RealBodyIndicator(series);
		System.out.println("real body: " + realBodyIndicator.getValue(series.getEndIndex()));
		ADXIndicator adxIndicator = new ADXIndicator(series, 14);
		System.out.println("adxIndicator: " + adxIndicator.getValue(series.getEndIndex()));
		
		if (counter == 3) {		
			if (alarm.getEnterLong() || alarm.getEnterShort()) {
				alarm.setEnterLong(false);
				alarm.setEnterShort(false);
				try {
					AlarmDB.editAlarm(alarm);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			counter = 0;
		}
		
		if (adxIndicator.getValue(series.getEndIndex()).doubleValue() < 25.0) {
			System.out.println("---");
			System.out.println("Below ADX 25, returning..");
			counter = 0;
			return;
		}
		
		if (counter == 0) {			
			if (isSameState(lastBar, beforeLastBar)) {
				if (lastBar.getClosePrice().isGreaterThan(beforeLastBar.getClosePrice())
					   || lastBar.getClosePrice().isLessThan(beforeLastBar.getClosePrice())) {
					counter = 2;
				}
			}
		} else if (counter == 2) {
			if (isSameState(lastBar, beforeLastBar)) {
				if (lastBar.getClosePrice().isGreaterThan(beforeLastBar.getClosePrice())
					   || lastBar.getClosePrice().isLessThan(beforeLastBar.getClosePrice())) {
					counter = 3;
				} else {
					counter = 0;
				}
			} else {
				counter = 0;
			}
		}
		
		if (counter == 3) {
			if (lastBar.isBullish() && isPriceAboveEmaLine()) {
				alarm.setEnterLong(true);
				try {
					telegramBot.sendMessage("Enter Long signal " + alarm.getSymbol());
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (lastBar.isBearish() && !isPriceAboveEmaLine()) {
				alarm.setEnterShort(true);
				try {
					telegramBot.sendMessage("Enter Short signal " + alarm.getSymbol());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			try {
				AlarmDB.editAlarm(alarm);
			} catch (Exception ex) {
				ex.printStackTrace();
			}			
		}
     }
	
	private boolean isSameState(Bar last, Bar before) {
		if (last.isBullish() && before.isBullish()
			   || last.isBearish() && before.isBearish()) {
			return true;
		}
		return false;
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
                    iae.printStackTrace();
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

     private boolean isPriceAboveEmaLine() {
          double currentEma = new EMAIndicator(this.closePriceIndicator, alarm.getFirstDema())
                  .getValue(this.closePriceIndicator.getBarSeries().getEndIndex()).doubleValue();
          double currentPrice = fetchTickerPrice();

          return currentPrice > currentEma;
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
               telegramBot.sendMessage("Futures Three Bars Alarm Cross task exception " + alarm.getSymbol() + "\n"
                       + type + "\n" + msg);
          } catch (Exception e) {
               e.printStackTrace();
          }
     }
}

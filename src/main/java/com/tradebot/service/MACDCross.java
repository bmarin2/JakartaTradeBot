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
import com.tradebot.model.MACDAlarm;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import org.json.JSONArray;
import org.json.JSONObject;
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
	private MACDIndicator macdIndicator;

	public MACDCross(MACDAlarm macdAlarm) {
		this.macdAlarm = macdAlarm;
		initChartMode(macdAlarm);		
		telegramBot = new TelegramBot();		
		series = new BaseBarSeriesBuilder().withName("mySeries").build();
          series.setMaximumBarCount(macdAlarm.getEma() * 2);		
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
               fetchBarSeries(macdAlarm.getEma() * 2);
          } else {
               fetchBarSeries(1);
          }

          closePriceIndicator = new ClosePriceIndicator(series);
		macdIndicator = new MACDIndicator(closePriceIndicator);

          macdAlarm.setCurrentMacdLine(
                  macdIndicator.getValue(macdIndicator.getBarSeries().getEndIndex()).doubleValue()
          );
          macdAlarm.setCurrentSignalLine(
                  new EMAIndicator(macdIndicator, 9).getValue(macdIndicator.getBarSeries().getEndIndex()).doubleValue()
          );          
          macdAlarm.setCurrentEma(
                  new EMAIndicator(closePriceIndicator, macdAlarm.getEma()).getValue(closePriceIndicator.getBarSeries().getEndIndex()).doubleValue()          
          );          
          macdAlarm.setLastClosingCandle(closePriceIndicator.getValue(closePriceIndicator.getBarSeries().getEndIndex()).doubleValue());
          
          System.out.println("series size: " + series.getBarCount());
          System.out.println("macd line:   " + macdAlarm.getCurrentMacdLine());
          System.out.println("signal line: " + macdAlarm.getCurrentSignalLine());
          System.out.println("ema line:    " + macdAlarm.getCurrentEma());
          System.out.println("last candle: " + macdAlarm.getLastClosingCandle());
          System.out.println("--------------------------");
     }

     private void calculateValues() {
          System.out.println("Calculating values:");
          System.out.println("--");

          if (macdAlarm.getMacdCrosss() && macdAlarm.getCurrentMacdLine() > macdAlarm.getCurrentSignalLine()) {

			double percentageIncrease = (macdAlarm.getMinGap() / 100) * macdAlarm.getCurrentSignalLine();
			double incrisedSignalLine = macdAlarm.getCurrentSignalLine() + percentageIncrease;

			if (macdAlarm.getCurrentMacdLine() > incrisedSignalLine) {
				macdAlarm.setMacdCrosss(false);
				System.out.println("Macd Crosss is now false");

				if (macdAlarm.getCurrentMacdLine() < 0 && macdAlarm.getCurrentSignalLine() < 0 && isPriceAboveEmaLine()) {
					macdAlarm.setGoodForEntry(true);
					System.out.println("Good for entry");
					try {
						telegramBot.sendMessage("Good for entry LONG");
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					macdAlarm.setGoodForEntry(false);
					System.out.println("Not good for entry");
				}
			}

          } else if (!macdAlarm.getMacdCrosss() && macdAlarm.getCurrentMacdLine() < macdAlarm.getCurrentSignalLine()) {
			
			double percentageIncrease = (macdAlarm.getMinGap() / 100) * macdAlarm.getCurrentSignalLine();
			double incrisedSignalLine = macdAlarm.getCurrentSignalLine() - percentageIncrease;
			
			if (macdAlarm.getCurrentMacdLine() < incrisedSignalLine) {
				macdAlarm.setMacdCrosss(true);
				System.out.println("Macd Crosss is now true");

				if (macdAlarm.getCurrentMacdLine() > 0 && macdAlarm.getCurrentSignalLine() > 0 && !isPriceAboveEmaLine()) {
					macdAlarm.setGoodForEntry(true);
					System.out.println("Good for entry");
					try {
						telegramBot.sendMessage("Good for entry SHORT");
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					macdAlarm.setGoodForEntry(false);
					System.out.println("Not good for entry");
				}
			}
		}

          try {
               MACDAlarmDB.editAlarm(macdAlarm);
          } catch (Exception ex) {
               ex.printStackTrace();
          }
     }

	private void fetchBarSeries(int limit) {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", macdAlarm.getSymbol());
		parameters.put("interval", macdAlarm.getIntervall());
		parameters.put("limit", limit + 1);

		String result = "";		
		
		if (macdAlarm.getChartMode().name().startsWith("SPOT")) {
			result = spotClientImpl.createMarket().klines(parameters);
		} else if (macdAlarm.getChartMode().name().startsWith("FUTURES")) {
			result = umFuturesClientImpl.market().klines(parameters);
		}

		JSONArray jsonArray = new JSONArray(result);
		jsonArray.remove(jsonArray.length() - 1);

		for (int i = 0; i < jsonArray.length(); i++) {
			JSONArray candlestick = jsonArray.getJSONArray(i);

			long unixTimestampMillis = candlestick.getLong(6);
			Instant instant = Instant.ofEpochMilli(unixTimestampMillis);
			ZonedDateTime utcZonedDateTime = instant.atZone(ZoneId.of("UTC"));

			series.addBar(utcZonedDateTime,
				   candlestick.getString(1),
				   candlestick.getString(2),
				   candlestick.getString(3),
				   candlestick.getString(4),
				   candlestick.getString(5)
			);
		}
	}

     private Double fetchTickerPrice() {
          String result = "";

          try {
               result = umFuturesClientImpl.market().tickerSymbol(
                       FuturesOrderParams.getTickerParams(macdAlarm.getSymbol())
               );
          } catch (BinanceConnectorException e) {
               System.out.println("BinanceConnectorException");
               e.printStackTrace();
          } catch (BinanceClientException e) {
               System.out.println("BinanceClientException");
               e.printStackTrace();
          } catch (BinanceServerException e) {
               System.out.println("BinanceServerException");
               e.printStackTrace();
          }
          JSONObject jsonResult = new JSONObject(result);
          return jsonResult.optDouble("price");
     }

     private boolean isPriceAboveEmaLine() {
          double currentEma = new EMAIndicator(this.closePriceIndicator, macdAlarm.getEma())
                  .getValue(this.closePriceIndicator.getBarSeries().getEndIndex()).doubleValue();
          double currentPrice = fetchTickerPrice();

          return currentPrice > currentEma;
     }
	
	private void initChartMode(MACDAlarm macdAlarm) {
		switch (macdAlarm.getChartMode()) {
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
}

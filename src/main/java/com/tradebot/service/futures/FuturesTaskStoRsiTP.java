package com.tradebot.service.futures;

import com.binance.connector.futures.client.exceptions.BinanceClientException;
import com.binance.connector.futures.client.exceptions.BinanceConnectorException;
import com.binance.connector.futures.client.exceptions.BinanceServerException;
import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.tradebot.binance.UMFuturesClientConfig;
import com.tradebot.configuration.FuturesOrderParams;
import com.tradebot.enums.ChartMode;
import com.tradebot.enums.PositionSide;
import com.tradebot.model.FuturesBot;
import com.tradebot.model.OrderSide;
import com.tradebot.service.TelegramBot;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Queue;
import org.json.JSONArray;
import org.json.JSONObject;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.StochasticRSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

public class FuturesTaskStoRsiTP implements Runnable {

	private FuturesBot futuresBot;
	private UMFuturesClientImpl umFuturesClientImpl;
	private final TelegramBot telegramBot;

	private String currentSLOrder = "";
	private String currentTPOrder = "";
	private PositionSide currentPositionSide;
	private Double entryPrice;

	private long lastTimestamp;
	private boolean firstTime = true;
	private double K;
	private double D;
	private BarSeries series;
	private Queue<Double> queue;
	private int maxQueueSize = 20; // horizontal box
	private double verticalPercent = 0.09; // vertical box, calculated 2x
	private double upperLimit;
	private double lowerLimit;
	private ClosePriceIndicator closePriceIndicator;
	private double atr;

	private double minGap = 0.02;

	private int ema1 = 8;
	private int ema2 = 14;
	private int ema3 = 150;
	private double currentEma1;
	private double currentEma2;
	private double currentEma3;

	private boolean stochasticCross;
	
	private boolean inConsolidation;
	private int consolidationStartLength = 20;
	private int consolidationCounter;

	private boolean overbought;
	private boolean oversold;

	public FuturesTaskStoRsiTP(FuturesBot futuresBot) {
		this.futuresBot = futuresBot;
		telegramBot = new TelegramBot();
		umFuturesClientImpl = initChartMode(futuresBot.getChartMode());
		currentSLOrder = initStopLossOrder();
		currentTPOrder = initTakeProfitOrder();
		currentPositionSide = initExistingPosition();
		System.out.println("init side: " + currentPositionSide);
		if (currentPositionSide != PositionSide.NONE) {
			entryPrice = getEntryPrice();
			System.out.println("Entry price: " + entryPrice);
		}

		series = new BaseBarSeriesBuilder().withName("mySeries").build();
		series.setMaximumBarCount(400 + maxQueueSize);
		queue = new LinkedList<>();
		runner();
	}

	@Override
	public void run() {

		if (currentPositionSide != PositionSide.NONE) {

			if (!currentSLOrder.isEmpty()) {
				String status = getOrderStatus(currentSLOrder);
				if (status.equals("FILLED") || status.equals("CANCELED")) {
					currentPositionSide = PositionSide.NONE;
					double pnl = getRealizedPNL(currentSLOrder);
					currentSLOrder = "";
					cancelOrder(currentTPOrder);
					currentTPOrder = "";
					entryPrice = null;
					System.out.println("SP Triggered");

					try {

						telegramBot.sendMessage("Stop Loss triggered " + futuresBot.getSymbol() + "\n"
							   + "PNL: " + pnl);
					} catch (Exception e) {
						e.printStackTrace();
					}
					return;
				}
			}

			if (!currentTPOrder.isEmpty()) {
				String status = getOrderStatus(currentTPOrder);
				if (status.equals("FILLED")) {
					currentPositionSide = PositionSide.NONE;
					double pnl = getRealizedPNL(currentTPOrder);
					currentTPOrder = "";
					cancelOrder(currentSLOrder);
					currentSLOrder = "";
					entryPrice = null;
					System.out.println("TP Triggered");

					try {

						telegramBot.sendMessage("Take Profit triggered " + futuresBot.getSymbol() + "\n"
							   + "PNL: " + pnl);
					} catch (Exception e) {
						e.printStackTrace();
					}
					return;
				}
			}
		}

		if (currentPositionSide != PositionSide.NONE) {
			System.out.println("SL order: " + currentSLOrder);
			System.out.println("TP order: " + currentTPOrder);
			System.out.println("pos: " + currentPositionSide.toString());
			System.out.println("entry price: " + entryPrice);
		}
		
		runner();
	}

	// ==========================================================================================

	private void runner() {
		if (firstTime) {
			fetchBarSeries(400 + maxQueueSize);
			firstTime = false;
		} else {
			fetchBarSeries(1);
		}
	}

	private void fetchBarSeries(int limit) {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", futuresBot.getSymbol());
		parameters.put("interval", futuresBot.getIntervall());
		parameters.put("limit", limit + 1);

		String result = "";

		try {
			result = umFuturesClientImpl.market().klines(parameters);
		} catch (BinanceConnectorException e) {
			sendTelegramMessage("BinanceConnectorException", e.getMessage());
			e.printStackTrace();
			return;
		} catch (BinanceClientException e) {
			sendTelegramMessage("BinanceClientException", e.getMessage());
			e.printStackTrace();
			return;
		} catch (BinanceServerException e) {
			sendTelegramMessage("BinanceServerException", e.getMessage());
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
		compareValues();
	}

	private void updateValues() {
		closePriceIndicator = new ClosePriceIndicator(series);

		Indicator sr = new StochasticRSIIndicator(series, 14);
		SMAIndicator k = new SMAIndicator(sr, 3); // blue
		SMAIndicator d = new SMAIndicator(k, 3); // yellow		

		K = k.getValue(k.getBarSeries().getEndIndex()).doubleValue();
		D = d.getValue(k.getBarSeries().getEndIndex()).doubleValue();

		System.out.println("K:   " + K);
		System.out.println("D:   " + D);

		EMAIndicator ema1_tmp = new EMAIndicator(closePriceIndicator, ema1);
		currentEma1 = ema1_tmp.getValue(ema1_tmp.getBarSeries().getEndIndex()).doubleValue();

		EMAIndicator ema2_tmp = new EMAIndicator(closePriceIndicator, ema2);
		currentEma2 = ema2_tmp.getValue(ema2_tmp.getBarSeries().getEndIndex()).doubleValue();

		EMAIndicator ema3_tmp = new EMAIndicator(closePriceIndicator, ema3);
		currentEma3 = ema3_tmp.getValue(ema3_tmp.getBarSeries().getEndIndex()).doubleValue();
		
		atr = new ATRIndicator(series, 14).getValue(series.getEndIndex()).doubleValue();

		if (firstTime) {
			int candleCounter = 400;

			for (int i = 0; i < maxQueueSize; i++) {
				ClosePriceIndicator closePricesSub = new ClosePriceIndicator(series.getSubSeries(0, candleCounter));
				EMAIndicator emaTmp = new EMAIndicator(closePricesSub, ema3);
				queue.offer(emaTmp.getValue(emaTmp.getBarSeries().getEndIndex()).doubleValue());
				candleCounter++;
			}
		} else {
			if (queue.size() == maxQueueSize) {
				queue.poll();
			}
			queue.offer(currentEma3);
		}

		double oldest = queue.peek();

		double percentValue = oldest * (verticalPercent / 100);
		upperLimit = oldest + percentValue;
		lowerLimit = oldest - percentValue;

		System.out.println("oldest " + oldest);
		System.out.println("upperLimit " + upperLimit);
		System.out.println("lowerLimit " + lowerLimit);
		System.out.println("---");
		System.out.println("currentEma1 " + currentEma1);
		System.out.println("currentEma2 " + currentEma2);
		System.out.println("currentEma3 " + currentEma3);
		System.out.println("---");
		System.out.println("stochasticCross " + stochasticCross);		
		System.out.println("atr: " + atr);

		if (!inConsolidation && currentEma3 < upperLimit && currentEma3 > lowerLimit) {
			if (consolidationCounter == consolidationStartLength) {
				inConsolidation = true;
				consolidationCounter = 0;
				sendTelegramMessage("in consolidation", "");
			} else {
				consolidationCounter++;
			}
		} else if (inConsolidation && currentEma3 > upperLimit || currentEma3 < lowerLimit) {
			inConsolidation = false;
			consolidationCounter = 0;
			sendTelegramMessage("in trending", "");
		}

		System.out.println("in Consolidation " + inConsolidation);

		System.out.println("--------------------------");
	}

	private void compareValues() {

		if (inConsolidation) {
			RSIIndicator rsiIndicator = new RSIIndicator(closePriceIndicator, 14);
			double currentRsi = rsiIndicator.getValue(rsiIndicator.getBarSeries().getEndIndex()).doubleValue();
			System.out.println("rsiIndicator: " + currentRsi);

			if (!overbought && currentRsi > 70) {
				overbought = true;
			} else if (overbought && currentRsi < 70) {
				if (currentPositionSide == PositionSide.NONE) {
					sendTelegramMessage("entering SHORT from RSI", "");
				}
				overbought = false;
			} else if (!oversold && currentRsi < 30) {
				oversold = true;
			} else if (oversold && currentRsi > 30) {
				if (currentPositionSide == PositionSide.NONE) {
					sendTelegramMessage("entering LONG from RSI", "");
				}
				oversold = false;
			}
		}

		if (stochasticCross && K > D) {
			double increasedD = D + minGap;
			if (K > increasedD) {

				stochasticCross = false;
				// && (K < 0.5 || D < 0.5)
				if (currentPositionSide == PositionSide.NONE && emasSetForLong()
					   && (K < 0.6 || D < 0.6) && (currentEma3 > upperLimit)) {
					sendTelegramMessage("entering long", "");
					//prepareLongOrder();
				}
			}
		} else if (!stochasticCross && K < D) {
			double decreasedD = D - minGap;
			if (K < decreasedD) {

				stochasticCross = true;
				if (currentPositionSide == PositionSide.NONE && emasSetForShort()
					   && (K > 0.4 || D > 0.4) && (currentEma3 < lowerLimit)) {
					sendTelegramMessage("entering short", "");
					//prepareShortOrder();
				}
			}
		}
	}
	
	private boolean emasSetForLong() {
		return currentEma1 > currentEma2
			  && currentEma2 > currentEma3;
	}

	private boolean emasSetForShort() {
		return currentEma1 < currentEma2
			  && currentEma2 < currentEma3;
	}

	private void prepareLongOrder() {
		try {
			telegramBot.sendMessage("Entering LONG trade " + getTime());
		} catch (IOException e) {
			e.printStackTrace();
		}

		createLongOrder();
		entryPrice = getEntryPrice();
		createStopLossOrder(OrderSide.SELL, calculateSL(PositionSide.LONG));
		createTakeProfit(OrderSide.SELL, calculateTP(PositionSide.LONG));
		currentPositionSide = PositionSide.LONG;

	}

	private void prepareShortOrder() {
		try {
			telegramBot.sendMessage("Entering SHORT trade " + getTime());
		} catch (IOException e) {
			e.printStackTrace();
		}

		createShortOrder();
		entryPrice = getEntryPrice();
		createStopLossOrder(OrderSide.BUY, calculateSL(PositionSide.SHORT));
		createTakeProfit(OrderSide.BUY, calculateTP(PositionSide.SHORT));
		currentPositionSide = PositionSide.SHORT;
	}

	private String createLongOrder() {
		return createOrder(OrderSide.BUY);
	}

	private String createShortOrder() {
		return createOrder(OrderSide.SELL);
	}

	private String createOrder(OrderSide orderSide) {
		String orderResult = "";
		String orderId = "";

		try {
			long timeStamp = System.currentTimeMillis();
			orderResult = umFuturesClientImpl.account().newOrder(
				   FuturesOrderParams.getOrderParams(futuresBot.getSymbol(), orderSide,
						 OrderSide.BOTH, futuresBot.getQuantity(), timeStamp)
			);
		} catch (BinanceConnectorException e) {
			sendTelegramMessage("BinanceConnectorException createOrder", e.getMessage());
			e.printStackTrace();
			return "";
		} catch (BinanceClientException e) {
			sendTelegramMessage("BinanceClientException createOrder", e.getMessage());
			e.printStackTrace();
			return "";
		} catch (BinanceServerException e) {
			sendTelegramMessage("BinanceServerException createOrder", e.getMessage());
			e.printStackTrace();
			return "";
		}

		JSONObject jsonResult = new JSONObject(orderResult);
		orderId = jsonResult.optString("orderId");

		System.out.println("Order created " + orderSide.toString() + " ID: " + orderId);

		return orderId;
	}

	private void createStopLossOrder(OrderSide orderSide, double stopPrice) {
		String orderResult = "";
		System.out.println("createding SL Order");
		try {
			System.out.println("start of try");
			long timeStamp = System.currentTimeMillis();
			orderResult = umFuturesClientImpl.account().newOrder(
				   FuturesOrderParams.getStopLossParams(futuresBot.getSymbol(),
						 orderSide, OrderSide.BOTH, futuresBot.getQuantity(),
						 stopPrice, timeStamp)
			);
			System.out.println("end of try");
		} catch (BinanceConnectorException e) {
			sendTelegramMessage("BinanceConnectorException createStopLossOrder", e.getMessage());
			e.printStackTrace();
			return;
		} catch (BinanceClientException e) {
			sendTelegramMessage("BinanceClientException createStopLossOrder", e.getMessage());
			e.printStackTrace();
			return;
		} catch (BinanceServerException e) {
			sendTelegramMessage("BinanceServerException createStopLossOrder", e.getMessage());
			e.printStackTrace();
			return;
		}

		JSONObject jsonResult = new JSONObject(orderResult);
		currentSLOrder = jsonResult.optString("orderId");
		System.out.println("SP Order ID: " + currentSLOrder);
	}

	private void createTakeProfit(OrderSide orderSide, double stopPrice) {
		System.out.println("stopPrice TP: " + stopPrice);
		long timeStamp = System.currentTimeMillis();

		String orderResult = "";

		try {
			orderResult = umFuturesClientImpl.account().newOrder(
				   FuturesOrderParams.getTakeProfitParams(futuresBot.getSymbol(),
						 orderSide, OrderSide.BOTH, futuresBot.getQuantity(),
						 stopPrice, timeStamp)
			);
		} catch (BinanceConnectorException e) {
			sendTelegramMessage("BinanceConnectorException create take profit", e.getMessage());
			e.printStackTrace();
			return;
		} catch (BinanceClientException e) {
			sendTelegramMessage("BinanceClientException create take profit", e.getMessage());
			e.printStackTrace();
			return;
		} catch (BinanceServerException e) {
			sendTelegramMessage("BinanceServerException create take profit", e.getMessage());
			e.printStackTrace();
			return;
		}
		System.out.println("orderResult TP: " + orderResult);
		JSONObject jsonResult = new JSONObject(orderResult);
		currentTPOrder = jsonResult.optString("orderId");
		System.out.println("Take Profit Order ID: " + currentTPOrder);
	}

	private void cancelOrder(String orderId) {
		try {
			long timeStamp = System.currentTimeMillis();
			umFuturesClientImpl.account().cancelOrder(
				   FuturesOrderParams.getCancelOrderParams(futuresBot.getSymbol(),
						 orderId, timeStamp)
			);
		} catch (BinanceConnectorException e) {
			sendTelegramMessage("BinanceConnectorException cancel order", e.getMessage());
			e.printStackTrace();
			return;
		} catch (BinanceClientException e) {
			sendTelegramMessage("BinanceClientException cancel order", e.getMessage());
			e.printStackTrace();
			return;
		} catch (BinanceServerException e) {
			sendTelegramMessage("BinanceServerException cancel order", e.getMessage());
			e.printStackTrace();
			return;
		}
	}

	private String getOrderStatus(String orderId) {
		String orderResult = "";

		try {
			long timeStamp = System.currentTimeMillis();
			orderResult = umFuturesClientImpl.account().queryOrder(
				   FuturesOrderParams.getQueryOrderParams(futuresBot.getSymbol(),
						 orderId, timeStamp)
			);
		} catch (BinanceConnectorException e) {
			sendTelegramMessage("BinanceConnectorException getOrderStatus", e.getMessage());
			e.printStackTrace();
			return "";
		} catch (BinanceClientException e) {
			System.out.println("getErrMsg: " + e.getErrMsg());
			sendTelegramMessage("BinanceClientException getOrderStatus", e.getMessage());
			e.printStackTrace();
			return "";
		} catch (BinanceServerException e) {
			sendTelegramMessage("BinanceServerException getOrderStatus", e.getMessage());
			e.printStackTrace();
			return "";
		}

		JSONObject jsonResult = new JSONObject(orderResult);
		String status = jsonResult.optString("status");
		return status;
	}

	private double calculateSL(PositionSide positionSide) {
		Double price = entryPrice;
		Double result = 0.0;
		if (positionSide == PositionSide.SHORT) {
			result = price + atr * futuresBot.getStopLoss();
		} else if (positionSide == PositionSide.LONG) {
			result = price - atr * futuresBot.getStopLoss();
		}
		String resultFormated = String.format("%.2f", result).replace(',', '.');
		return Double.parseDouble(resultFormated);
	}

	private double calculateTP(PositionSide positionSide) {
		Double price = entryPrice;
		Double result = 0.0;
		if (positionSide == PositionSide.SHORT) {
			result = price - atr * futuresBot.getTakeProfit();
		} else if (positionSide == PositionSide.LONG) {
			result = price + atr * futuresBot.getTakeProfit();
		}
		String resultFormated = String.format("%.2f", result).replace(',', '.');
		return Double.parseDouble(resultFormated);
	}

	private PositionSide initExistingPosition() {
		String jsonResult = "";

		try {
			long timeStamp = System.currentTimeMillis();
			jsonResult = umFuturesClientImpl.account().positionInformation(
				   FuturesOrderParams.getParams(futuresBot.getSymbol(), timeStamp)
			);
		} catch (BinanceConnectorException e) {
			sendTelegramMessage("BinanceConnectorException", e.getMessage());
			e.printStackTrace();
		} catch (BinanceClientException e) {
			sendTelegramMessage("BinanceClientException", e.getMessage());
			e.printStackTrace();
		} catch (BinanceServerException e) {
			sendTelegramMessage("BinanceServerException", e.getMessage());
			e.printStackTrace();
		}

		JSONArray positions = new JSONArray(jsonResult);
		JSONObject positionObject = positions.getJSONObject(0);

		String price = positionObject.optString("entryPrice");

		if (!price.equals("0.0") && !price.equals("0.00000000")) {
			String side = fetchPositionSide();
			if (side.equals("BUY")) {
				return PositionSide.LONG;
			} else if (side.equals("SELL")) {
				return PositionSide.SHORT;
			}
		}
		return PositionSide.NONE;
	}

	private String fetchPositionSide() {
		String jsonResult = "";

		try {
			long timeStamp = System.currentTimeMillis();
			jsonResult = umFuturesClientImpl.account().accountTradeList(
				   FuturesOrderParams.getParams(futuresBot.getSymbol(), timeStamp)
			);
		} catch (BinanceConnectorException e) {
			sendTelegramMessage("BinanceConnectorException", e.getMessage());
			e.printStackTrace();
		} catch (BinanceClientException e) {
			sendTelegramMessage("BinanceClientException", e.getMessage());
			e.printStackTrace();
		} catch (BinanceServerException e) {
			sendTelegramMessage("BinanceServerException", e.getMessage());
			e.printStackTrace();
		}

		JSONArray positions = new JSONArray(jsonResult);
		if (positions.length() > 0) {
			JSONObject tradeObject = positions.getJSONObject(positions.length() - 1);
			return tradeObject.optString("side");
		} else {
			return "none";
		}

	}

	private String initStopLossOrder() {
		String orderId = "";
		String jsonResult = "";

		try {
			long timeStamp = System.currentTimeMillis();
			jsonResult = umFuturesClientImpl.account().currentAllOpenOrders(
				   FuturesOrderParams.getParams(futuresBot.getSymbol(), timeStamp)
			);
		} catch (BinanceConnectorException e) {
			sendTelegramMessage("BinanceConnectorException", e.getMessage());
			e.printStackTrace();
		} catch (BinanceClientException e) {
			sendTelegramMessage("BinanceClientException", e.getMessage());
			e.printStackTrace();
		} catch (BinanceServerException e) {
			sendTelegramMessage("BinanceServerException", e.getMessage());
			e.printStackTrace();
		}

		JSONArray orders = new JSONArray(jsonResult);

		for (int i = 0; i < orders.length(); i++) {
			JSONObject orderObject = orders.getJSONObject(i);

			if ("STOP_MARKET".equals(orderObject.optString("type"))) {
				orderId = Long.toString(orderObject.optLong("orderId"));
				break;
			}
		}
		System.out.println("init SP order: " + orderId);
		return orderId;
	}

	private String initTakeProfitOrder() {
		String orderId = "";

		long timeStamp = System.currentTimeMillis();
		String jsonResult = umFuturesClientImpl.account().currentAllOpenOrders(
			   FuturesOrderParams.getParams(futuresBot.getSymbol(), timeStamp));

		JSONArray orders = new JSONArray(jsonResult);

		for (int i = 0; i < orders.length(); i++) {
			JSONObject orderObject = orders.getJSONObject(i);

			if ("TAKE_PROFIT_MARKET".equals(orderObject.optString("type"))) {
				orderId = Long.toString(orderObject.optLong("orderId"));
				break;
			}
		}
		System.out.println("init TP order: " + orderId);
		return orderId;
	}

	private double getEntryPrice() {
		String jsonResult = "";

		try {
			long timeStamp = System.currentTimeMillis();
			jsonResult = umFuturesClientImpl.account().positionInformation(
				   FuturesOrderParams.getParams(futuresBot.getSymbol(), timeStamp)
			);
		} catch (BinanceConnectorException e) {
			sendTelegramMessage("BinanceConnectorException", e.getMessage());
			e.printStackTrace();
		} catch (BinanceClientException e) {
			sendTelegramMessage("BinanceClientException", e.getMessage());
			e.printStackTrace();
		} catch (BinanceServerException e) {
			sendTelegramMessage("BinanceServerException", e.getMessage());
			e.printStackTrace();
		}

		JSONObject positionObject = new JSONArray(jsonResult).getJSONObject(0);
		return positionObject.optDouble("entryPrice");
	}

	private String getTime() {
		LocalDateTime currentDateTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		return currentDateTime.format(formatter);
	}

	private double getRealizedPNL(String orderId) {
		String jsonResult = "";

		try {
			long timeStamp = System.currentTimeMillis();
			jsonResult = umFuturesClientImpl.account().accountTradeList(
				   FuturesOrderParams.getQueryOrderParams(futuresBot.getSymbol(), orderId, timeStamp)
			);
		} catch (BinanceConnectorException e) {
			sendTelegramMessage("BinanceConnectorException", e.getMessage());
			e.printStackTrace();
		} catch (BinanceClientException e) {
			sendTelegramMessage("BinanceClientException", e.getMessage());
			e.printStackTrace();
		} catch (BinanceServerException e) {
			sendTelegramMessage("BinanceServerException", e.getMessage());
			e.printStackTrace();
		}

		JSONArray tradeList = new JSONArray(jsonResult);
		JSONObject tradeObject = tradeList.getJSONObject(0);

		return tradeObject.optDouble("realizedPnl");
	}

	private UMFuturesClientImpl initChartMode(ChartMode chartMode) {
		switch (chartMode) {
			case FUTURES_BASE_URL_PROD:
				return UMFuturesClientConfig.futuresBaseURLProd();
			case FUTURES_BASE_URL_TEST:
				return UMFuturesClientConfig.futuresBaseURLTest();
			case FUTURES_SIGNED_PROD:
				return UMFuturesClientConfig.futuresSignedProd();
			case FUTURES_SIGNED_TEST:
				return UMFuturesClientConfig.futuresSignedTest();
			default:
				return null;
		}
	}

	private void sendTelegramMessage(String title, String msg) {
		try {
			telegramBot.sendMessage(title + "\n" + msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

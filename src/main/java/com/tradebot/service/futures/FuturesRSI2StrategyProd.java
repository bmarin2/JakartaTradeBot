package com.tradebot.service.futures;

import com.binance.connector.futures.client.exceptions.BinanceClientException;
import com.binance.connector.futures.client.exceptions.BinanceConnectorException;
import com.binance.connector.futures.client.exceptions.BinanceServerException;
import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.tradebot.binance.UMFuturesClientConfig;
import com.tradebot.configuration.FuturesOrderParams;
import com.tradebot.enums.Environment;
import static com.tradebot.enums.Environment.FUTURES_BASE_URL_PROD;
import static com.tradebot.enums.Environment.FUTURES_BASE_URL_TEST;
import static com.tradebot.enums.Environment.FUTURES_SIGNED_PROD;
import static com.tradebot.enums.Environment.FUTURES_SIGNED_TEST;
import com.tradebot.enums.PositionSide;
import com.tradebot.model.FuturesBot;
import com.tradebot.model.OrderSide;
import com.tradebot.service.TelegramBot;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import lombok.Data;
import org.json.JSONArray;
import org.json.JSONObject;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

@Data
public class FuturesRSI2StrategyProd implements Runnable {
	
	
	private FuturesBot futuresBot;
	private UMFuturesClientImpl umFuturesClientImpl;
	private final TelegramBot telegramBot;

	private String currentSLOrder = "";
	private String currentTPOrder = "";

	private double currentStopLossPrice;
	private double currentTakeProfitPrice;
	private PositionSide currentPositionSide;
	private long lastTimestamp;
	private long lastTimestamp2;
	private boolean firstTime = true;

	private BarSeries series;
	private BarSeries series2;
	private Bar currentBar;
	private Bar currentBar2;
	private ClosePriceIndicator closePriceIndicator;
	private ClosePriceIndicator closePriceIndicator2;

	private double atr;

	private int sma1 = 5;
	private int sma2 = 100;
	private double currentSma1;
	private double currentSma2;

	private int win;
	private int lose;

	private double lossSum;
	private double gainSum;

	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

	private Double entryPrice;
	
	private double currentAdx;
	private double lastAdx;
	private boolean fallingAdx;
	
	private double currentRsi;
	private double currentRsi2;
	private double currentAw;
	
	DecimalFormat df = new DecimalFormat("0.0000");

	public FuturesRSI2StrategyProd(FuturesBot futuresBot) {
		this.futuresBot = futuresBot;
		telegramBot = new TelegramBot();
		umFuturesClientImpl = initEnvironment(futuresBot.getEnvironment());
		currentSLOrder = initStopLossOrder();
		currentTPOrder = initTakeProfitOrder();
		currentPositionSide = initExistingPosition();
		System.out.println("init side: " + currentPositionSide);
		if (currentPositionSide != PositionSide.NONE) {
			entryPrice = getEntryPrice();
			System.out.println("Entry price: " + entryPrice);
		}
		series = new BaseBarSeriesBuilder().withName("mySeries").build();
		series.setMaximumBarCount(300);
		
		series2 = new BaseBarSeriesBuilder().withName("mySeries2").build();
		series2.setMaximumBarCount(300);
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
		
		fetchSeries();
		updateValues();
		updateValues2();
		enterTrade();
		
	}
	
	private void fetchSeries() {
		if (firstTime) {
			fetchBarSeries(300, futuresBot.getIntervall(), series, lastTimestamp);
			fetchBarSeries(300, futuresBot.getIntervall2(), series2, lastTimestamp2);
			firstTime = false;
		} else {
			fetchBarSeries(1, futuresBot.getIntervall(), series, lastTimestamp);
			fetchBarSeries(1, futuresBot.getIntervall2(), series2, lastTimestamp2);
		}
	}

	private void fetchBarSeries(int limit, String interval, BarSeries barSeries, long lastTime) {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", futuresBot.getSymbol());
		parameters.put("interval", interval);
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

			if (lastTime == timestamp) {
				return;
			} else {
				lastTime = timestamp;
			}
		} else {
			JSONArray array2 = jsonArray.getJSONArray(jsonArray.length() - 1);
			long timestamp2 = array2.getLong(6);
			lastTime = timestamp2;
		}

		for (int i = 0; i < jsonArray.length(); i++) {
			JSONArray candlestick = jsonArray.getJSONArray(i);

			long unixTimestampMillis = candlestick.getLong(6);
			Instant instant = Instant.ofEpochMilli(unixTimestampMillis);
			ZonedDateTime utcZonedDateTime = instant.atZone(ZoneId.of("UTC"));
			try {
				barSeries.addBar(utcZonedDateTime,
					   candlestick.getString(1),
					   candlestick.getString(2),
					   candlestick.getString(3),
					   candlestick.getString(4),
					   candlestick.getString(5)
				);
			} catch (IllegalArgumentException iae) {
				ZonedDateTime timePlusOneMin = barSeries.getLastBar().getEndTime().plusMinutes(1);
				barSeries.addBar(timePlusOneMin,
					   candlestick.getString(1),
					   candlestick.getString(2),
					   candlestick.getString(3),
					   candlestick.getString(4),
					   candlestick.getString(5)
				);
			}
		}
	}

	private void updateValues() {
		closePriceIndicator = new ClosePriceIndicator(series);
		atr = new ATRIndicator(series, 14).getValue(series.getEndIndex()).doubleValue();
		
		RSIIndicator rsiIndicator1 = new RSIIndicator(closePriceIndicator, 2);
		currentRsi = rsiIndicator1.getValue(rsiIndicator1.getBarSeries().getEndIndex()).doubleValue();
	}
	
	private void updateValues2() {
		closePriceIndicator2 = new ClosePriceIndicator(series2);
		
		RSIIndicator rsiIndicator2 = new RSIIndicator(closePriceIndicator2, 2);
		currentRsi2 = rsiIndicator2.getValue(rsiIndicator2.getBarSeries().getEndIndex()).doubleValue();
		
	}
	
	private void enterTrade() {
		if (currentPositionSide == PositionSide.NONE && currentRsi2 > 60 && currentRsi < 10 ) {
			
			prepareLongOrder();

		} else if (currentPositionSide == PositionSide.NONE && currentRsi2 < 40 && currentRsi > 90 ) {
			
			prepareShortOrder();
		}
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
	
	private UMFuturesClientImpl initEnvironment(Environment environment) {
		switch (environment) {
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
	
	private void sendTelegramMessage(String title, String msg) {
		try {
			telegramBot.sendMessage(title + "\n" + msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getTime() {
		LocalDateTime currentDateTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		return currentDateTime.format(formatter);
	}
	
}

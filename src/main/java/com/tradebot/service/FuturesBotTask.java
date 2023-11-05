package com.tradebot.service;

import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.tradebot.binance.UMFuturesClientConfig;
import com.tradebot.configuration.FuturesOrderParams;
import com.tradebot.db.AlarmDB;
import com.tradebot.model.Alarm;
import com.tradebot.model.FuturesBot;
import com.tradebot.model.OrderSide;
import org.json.JSONObject;

public class FuturesBotTask implements Runnable {
	
	private FuturesBot futuresBot;
	private UMFuturesClientImpl umFuturesClientImpl;
	private final TelegramBot telegramBot;
	
	private boolean currentCross; // cross: fast -> slow
	private boolean currentCrossBig; // cross: fask -> third
	
	private boolean inTrade;
	private String currentSPOrder = "";

	public FuturesBotTask(FuturesBot futuresBot) {
		this.futuresBot = futuresBot;
		umFuturesClientImpl = UMFuturesClientConfig.futuresSignedTest();
		initDemas();
		this.telegramBot = new TelegramBot();		
	}

	@Override
	public void run() {
		
		if (!currentSPOrder.isEmpty()) {
			if (getOrderStatus(currentSPOrder).equals("FILLED")) {
				inTrade = false;
				System.out.println("SP Triggered");
			}
		}
		
		Alarm alarm = null;
		
		try {
			alarm = AlarmDB.getOneAlarm(futuresBot.getDemaAlertTaskId());			
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (currentCross != alarm.getCrosss()) {				
			if (alarm.getCrosss()) {
				if (inTrade) {
					createShortOrder(); // take profit
				}
				createShortOrder(); // crete new order
				createStopLoss(OrderSide.BUY, calctulateSP(true));
				inTrade = true;
			} else {
				if (inTrade) {
					createLongOrder(); // take profit
				}
				createLongOrder(); // crete new order
				createStopLoss(OrderSide.SELL, calctulateSP(false));
				inTrade = true;
			}				
			currentCross = alarm.getCrosss();
		}		

	}
	
	private void initDemas() {
		try {
			Alarm alarm = AlarmDB.getOneAlarm(futuresBot.getDemaAlertTaskId());
			currentCross = alarm.getCrosss();
			currentCrossBig = alarm.getCrosssBig();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void createLongOrder() {
		createOrder(OrderSide.BUY);
	}

	private void createShortOrder() {
		createOrder(OrderSide.SELL);
	}

	private void createOrder(OrderSide orderSide) {
		long timeStamp = System.currentTimeMillis();

		String orderResult = umFuturesClientImpl.account().newOrder(
			   FuturesOrderParams.getOrderParams(futuresBot.getSymbol(), orderSide,
					 OrderSide.BOTH, futuresBot.getQuantity(), timeStamp)
		);
		
		JSONObject jsonResult = new JSONObject(orderResult);
		String orderId = jsonResult.optString("orderId");
		System.out.println("Order createed " + orderSide.toString() + " ID: " + orderId);
	}

	private void createStopLoss(OrderSide orderSide, Double stopPrice) {
		long timeStamp = System.currentTimeMillis();
		
		String orderResult = umFuturesClientImpl.account().newOrder(
			   FuturesOrderParams.getStopLossParams(futuresBot.getSymbol(),
					 orderSide, OrderSide.BOTH, futuresBot.getQuantity(),
					 stopPrice, timeStamp)
		);
		
		JSONObject jsonResult = new JSONObject(orderResult);
		currentSPOrder = jsonResult.optString("orderId");
		System.out.println("SP Order ID: " + currentSPOrder);
	}

	private void cancelOrder(long orderId) {
		long timeStamp = System.currentTimeMillis();
		String orderResult = umFuturesClientImpl.account().cancelOrder(
			   FuturesOrderParams.getCancelOrderParams(futuresBot.getSymbol(),
					 orderId, timeStamp)
		);
	}
	
	private String getOrderStatus(String orderId) {
		long timeStamp = System.currentTimeMillis();
		String orderResult = umFuturesClientImpl.account().queryOrder(
			   FuturesOrderParams.getQueryOrderParams(futuresBot.getSymbol(),
					 orderId, timeStamp)
		);
		JSONObject jsonResult = new JSONObject(orderResult);
		String status = jsonResult.optString("status");
		return status;
	}
	
	private Double getTickerPrice() {
		String result = umFuturesClientImpl.market().tickerSymbol(
			   FuturesOrderParams.getTickerParams(futuresBot.getSymbol())
		);
		JSONObject jsonResult = new JSONObject(result);
		return jsonResult.optDouble("price");
	}

	private Double calctulateSP(boolean side) {
		Double price = getTickerPrice();
		Double percent = price * futuresBot.getStopLoss();
		Double result = 0.0;
		if (side) {
			result = price + percent;
		} else {
			result = price - percent;
		}
		System.out.println("Stop Loss price: " + result);
		return result;
	}
}

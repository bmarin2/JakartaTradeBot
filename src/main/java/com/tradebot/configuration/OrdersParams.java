package com.tradebot.configuration;

import com.tradebot.model.OrderSide;
import java.util.LinkedHashMap;

public class OrdersParams {

//	public static LinkedHashMap<String, Object> getOrderBuyParams(String symbol, Integer quoteQty, long timeStamp) {
//		return getParams("BUY", symbol, quoteQty, timeStamp);
//	}
//	
//	public static LinkedHashMap<String, Object> getOrderSellParams(String symbol, Integer quoteQty, long timeStamp) {
//		return getParams("SELL", symbol, quoteQty, timeStamp);
//	}
	
	public static LinkedHashMap<String, Object> getParams(String symbol, OrderSide side, Integer quoteQty, long timeStamp){
		
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", symbol);
		parameters.put("side", side.toString());
		parameters.put("type", "MARKET");
		//parameters.put("timeInForce", "GTC");
		parameters.put("quoteOrderQty", quoteQty);
		parameters.put("timestamp", timeStamp);
		return parameters;
	}
	
	public static LinkedHashMap<String, Object> getTickerSymbolParams(String symbol) {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", symbol);
		return parameters;
	}
}

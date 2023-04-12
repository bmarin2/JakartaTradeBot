package com.tradebot.configuration;

import com.tradebot.model.OrderSide;
import java.math.BigDecimal;
import java.util.LinkedHashMap;

public class OrdersParams {
	
	public static LinkedHashMap<String, Object> getOrderParams(String symbol, OrderSide side, BigDecimal quoteQty, long timeStamp){
		
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", symbol);
		parameters.put("side", side.toString());
		parameters.put("type", "MARKET");
		parameters.put("quoteOrderQty", quoteQty);
		parameters.put("timestamp", timeStamp);
		return parameters;
	}
	
	public static LinkedHashMap<String, Object> getTickerSymbolParams(String symbol) {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", symbol);
		return parameters;
	}
	
	public static LinkedHashMap<String, Object> getOrder(String symbol, long orderId, long timeStamp) {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", symbol);
		parameters.put("orderId", orderId);
		parameters.put("timestamp", timeStamp);
		return parameters;
	}
}

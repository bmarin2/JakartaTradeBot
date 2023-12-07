package com.tradebot.configuration;

import com.tradebot.model.OrderSide;
import java.util.LinkedHashMap;

public class FuturesOrderParams {
	
	public static LinkedHashMap<String, Object> getOrderParams(String symbol,
												 OrderSide side,
												 OrderSide positionSide,
												 Double quantity,
												 long timeStamp)
	{		
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", symbol);
		parameters.put("side", side.toString());
		parameters.put("positionSide", positionSide.toString());
		parameters.put("type", "MARKET");
		parameters.put("quantity", quantity);
		parameters.put("timestamp", timeStamp);
		return parameters;
	}
	
	public static LinkedHashMap<String, Object> getAccuntInfoParams(long timeStamp) {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("timeStamp", timeStamp);
		return parameters;
	}
	
	public static LinkedHashMap<String, Object> getParams(String symbol, long timeStamp) {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", symbol);
		parameters.put("timeStamp", timeStamp);
		return parameters;
	}
	
	public static LinkedHashMap<String, Object> getAllOrdersParams(String symbol, long timeStamp) {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", symbol);
		parameters.put("limit", 50);
		parameters.put("timeStamp", timeStamp);
		return parameters;
	}
	
	public static LinkedHashMap<String, Object> getStopLossParams(String symbol,
												 OrderSide side,
												 OrderSide positionSide,
												 Double quantity,
												 String stopPrice,
												 long timeStamp)
	{		
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", symbol);
		parameters.put("side", side.toString());
		parameters.put("positionSide", positionSide.toString());
		parameters.put("type", "STOP_MARKET");
		parameters.put("quantity", quantity);
		parameters.put("stopPrice", stopPrice);
		parameters.put("timestamp", timeStamp);
		return parameters;
	}
     
     public static LinkedHashMap<String, Object> getTakeProfitParams(String symbol,
                                                             OrderSide side,
                                                             OrderSide positionSide,
                                                             Double quantity,
                                                             String stopPrice,
                                                             long timeStamp) {
          LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
          parameters.put("symbol", symbol);
          parameters.put("side", side.toString());
          parameters.put("positionSide", positionSide.toString());
          parameters.put("type", "TAKE_PROFIT_MARKET");
          parameters.put("quantity", quantity);
          parameters.put("stopPrice", stopPrice);
          parameters.put("timestamp", timeStamp);
          return parameters;
     }
	
	public static LinkedHashMap<String, Object> getCancelOrderParams(String symbol,
													String orderId,
													long timeStamp)
	{
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", symbol);
		parameters.put("orderId", orderId);
		parameters.put("timeStamp", timeStamp);
		return parameters;
	}
	
	public static LinkedHashMap<String, Object> getQueryOrderParams(String symbol, String orderId, long timeStamp) {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", symbol);
		parameters.put("orderId", orderId);
		parameters.put("timeStamp", timeStamp);
		return parameters;
	}
	
	public static LinkedHashMap<String, Object> getTickerParams(String symbol) {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", symbol);
		return parameters;
	}
}

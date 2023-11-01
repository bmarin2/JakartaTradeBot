package com.tradebot.configuration;

import com.tradebot.model.OrderSide;
import com.tradebot.model.PositionSide;
import java.util.LinkedHashMap;

public class FuturesOrderParams {
	
	public static LinkedHashMap<String, Object> getOrderParams(String symbol,
												 OrderSide side,
												 PositionSide positionSide,
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
	
	public static LinkedHashMap<String, Object> getPositionInformationParams(String symbol, long timeStamp) {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", symbol);
		parameters.put("timeStamp", timeStamp);
		return parameters;
	}
	
	public static LinkedHashMap<String, Object> getAccuntInfoParams(long timeStamp) {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("timeStamp", timeStamp);
		return parameters;
	}
	
	public static LinkedHashMap<String, Object> getAccountTradeListParams(String symbol, long timeStamp) {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", symbol);
		parameters.put("timeStamp", timeStamp);
		return parameters;
	}
}

package com.tradebot.service;

import com.binance.connector.client.exceptions.BinanceClientException;
import com.binance.connector.client.exceptions.BinanceConnectorException;
import com.binance.connector.client.impl.SpotClientImpl;
import com.tradebot.binance.SpotClientConfig;
import com.tradebot.configuration.OrdersParams;
import com.tradebot.db.TradeBotDB;
import com.tradebot.model.TradeBot;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class Task implements Runnable {

	@Getter @Setter
	private TradeBot tradeBot;
	
	@Getter @Setter
	private SpotClientImpl spotClientImpl;
	
	public Task(){
		spotClientImpl = SpotClientConfig.spotClientSignTest();
	}
	
	private TradeBot currentBot;
	
	@Override
	public void run() {
		
		//long timeStamp = System.currentTimeMillis();
		
//		System.out.println("Bot: " + tradeBot.getTaskId() + " " + LocalDateTime.now());
//		System.out.println("Query bot 3:  " + currentBot);
//		System.out.println("------------------------------------------");
		
		
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		
		try {
			//String result = spotClientImpl.createTrade().newOrder(OrdersParams.getParams(tradeBot.getSymbol(), OrderSide.BUY, tradeBot.getQuoteOrderQty(), timeStamp));
			String result = spotClientImpl.createMarket().tickerSymbol(OrdersParams.getTickerSymbolParams("BTCUSDT"));
			System.out.println(result);
		} catch (BinanceConnectorException e) {
			System.err.println((String) String.format("fullErrMessage: %s", e.getMessage()));
		} catch (BinanceClientException e) {
			System.err.println((String) String.format("fullErrMessage: %s \nerrMessage: %s \nerrCode: %d \nHTTPStatusCode: %d",
				e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode()));
		}
	}
}

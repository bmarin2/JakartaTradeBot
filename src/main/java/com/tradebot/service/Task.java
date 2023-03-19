package com.tradebot.service;

import com.binance.connector.client.exceptions.BinanceClientException;
import com.binance.connector.client.exceptions.BinanceConnectorException;
import com.binance.connector.client.impl.SpotClientImpl;
import com.tradebot.binance.SpotClientConfig;
import com.tradebot.configuration.OrdersParams;
import com.tradebot.model.TradeBot;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

@Data
public class Task implements Runnable {

	@Getter @Setter
	private TradeBot tradeBot;
	
	@Getter @Setter
	private SpotClientImpl spotClientImpl;
	
	public Task(){
		spotClientImpl = SpotClientConfig.spotClientSignTest();
	}
	
	Set<BigDecimal> positions = new LinkedHashSet<>();
	
	@Override
	public void run() {
		
		//long timeStamp = System.currentTimeMillis();
		
		try {
			//String result = spotClientImpl.createTrade().newOrder(OrdersParams.getParams(tradeBot.getSymbol(), OrderSide.BUY, tradeBot.getQuoteOrderQty(), timeStamp));
			String result = spotClientImpl.createMarket().tickerSymbol(OrdersParams.getTickerSymbolParams(tradeBot.getSymbol()));
			
			JSONObject jsonObject = new JSONObject(result);
			
			BigDecimal newPosition = new BigDecimal(jsonObject.getString("price"));
			
			System.out.println("Checking the price for " + tradeBot.getSymbol() + " at the price of: " + newPosition.setScale(2, RoundingMode.HALF_DOWN) + "  " + LocalDateTime.now());
			
			if(positions.isEmpty()) {
				positions.add(newPosition);
				System.out.println("First position added for " + tradeBot.getSymbol() + " added at the price: " + newPosition.setScale(2, RoundingMode.DOWN));
				return;
			}
			
			BigDecimal lastAddedPosition = null;
			
			for (BigDecimal element : positions) {
				lastAddedPosition = element;
			}
			
			int comparison = newPosition.compareTo(lastAddedPosition);
			BigDecimal positionPercentage = lastAddedPosition.multiply(new BigDecimal(tradeBot.getOrderStep()/100));
			
			if (comparison < 0 && (positions.size() <= tradeBot.getCycleMaxOrders())) {
				BigDecimal decreasedPosition = lastAddedPosition.subtract(positionPercentage);
				int comparisonDecreaseed = newPosition.compareTo(decreasedPosition);
				if(comparisonDecreaseed < 0) {						
					System.out.println("---------------------------");
					System.out.println("BUYING!");
					System.out.println("Old Price: " + lastAddedPosition.setScale(2, RoundingMode.DOWN));
					System.out.println("New Price: " + newPosition.setScale(2, RoundingMode.DOWN));												
					positions.add(newPosition);
					System.out.println(tradeBot.getSymbol() + " Position it added, Set size is now: " + positions.size());
				}					
			}
			
			else if (comparison > 0 && !positions.isEmpty()) {
				BigDecimal increasedPosition = lastAddedPosition.add(positionPercentage);
				int comparisonIncreased = newPosition.compareTo(increasedPosition);
				if (comparisonIncreased > 0) {					
					LinkedList<BigDecimal> list = new LinkedList<>(positions);
					Iterator<BigDecimal> iterator = list.descendingIterator();
					System.out.println("In multi-check");
					while (iterator.hasNext()) {
						BigDecimal position = iterator.next();
						BigDecimal increasedPositionLoop = position.add(positionPercentage);
						int comparisonIncreasedPosition = newPosition.compareTo(increasedPositionLoop);
						if (comparisonIncreasedPosition > 0) {
							System.out.println("---------------------------");
							System.out.println("SELLING!");
							System.out.println("Old Price: " + position.setScale(2, RoundingMode.DOWN));
							System.out.println("New Price: " + newPosition.setScale(2, RoundingMode.DOWN));
							positions.remove(position);
							System.out.println(tradeBot.getSymbol() + " Position it removed, Set size is now: " + positions.size());
						} else {
							return;
						}
					}
				}
			}
			

		} catch (BinanceConnectorException e) {
			System.err.println((String) String.format("fullErrMessage: %s", e.getMessage()));
		} catch (BinanceClientException e) {
			System.err.println((String) String.format("fullErrMessage: %s \nerrMessage: %s \nerrCode: %d \nHTTPStatusCode: %d",
				e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode()));
		}
	}
}

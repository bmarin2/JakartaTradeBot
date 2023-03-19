package com.tradebot.service;

import com.binance.connector.client.exceptions.BinanceClientException;
import com.binance.connector.client.exceptions.BinanceConnectorException;
import com.binance.connector.client.impl.SpotClientImpl;
import com.tradebot.binance.SpotClientConfig;
import com.tradebot.configuration.OrdersParams;
import com.tradebot.db.OrderDB;
import com.tradebot.model.OrderSide;
import com.tradebot.model.OrderTracker;
import com.tradebot.model.TradeBot;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
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
	
	Map<Long, BigDecimal> positions = new LinkedHashMap<>();
	
	@Override
	public void run() {
		
		try {
			//String result = spotClientImpl.createTrade().newOrder(OrdersParams.getParams(tradeBot.getSymbol(), OrderSide.BUY, tradeBot.getQuoteOrderQty(), timeStamp));
			String result = spotClientImpl.createMarket().tickerSymbol(OrdersParams.getTickerSymbolParams(tradeBot.getSymbol()));
			
			JSONObject jsonObject = new JSONObject(result);
			
			BigDecimal newPosition = new BigDecimal(jsonObject.getString("price"));
			
			System.out.println("Checking the price for " + tradeBot.getSymbol() + " at the price of: " + newPosition.setScale(2, RoundingMode.HALF_DOWN) + "  " + LocalDateTime.now());
			
			if (positions.isEmpty()) {
				createBuyOrder(newPosition);
				System.out.println("First position added for " + tradeBot.getSymbol() + " added at the price: " + newPosition.setScale(2, RoundingMode.HALF_DOWN));
				return;
			}
			
			Long lastAddedKey = null;

			for (Map.Entry<Long, BigDecimal> entry : positions.entrySet()) {
				lastAddedKey = entry.getKey();
			}
			
			int comparison = newPosition.compareTo(positions.get(lastAddedKey));
			BigDecimal positionPercentage = positions.get(lastAddedKey).multiply(new BigDecimal(tradeBot.getOrderStep()/100));
			
			// BUY
			if (comparison < 0 && (positions.size() <= tradeBot.getCycleMaxOrders())) {
				BigDecimal decreasedPosition = positions.get(lastAddedKey).subtract(positionPercentage);
				int comparisonDecreaseed = newPosition.compareTo(decreasedPosition);
				if(comparisonDecreaseed < 0) {						
					createBuyOrder(newPosition);
					System.out.println(tradeBot.getSymbol() + " Position it added, Set size is now: " + positions.size());
				}					
			}
			
			// SELL
			else if (comparison > 0 && !positions.isEmpty()) {
				BigDecimal increasedPosition = positions.get(lastAddedKey).add(positionPercentage);
				int comparisonIncreased = newPosition.compareTo(increasedPosition);
				if (comparisonIncreased > 0) {
					List<Map.Entry<Long, BigDecimal>> reverseList = new ArrayList<>(positions.entrySet());
					Collections.reverse(reverseList);
					List<Long> tempOrders = new ArrayList<>();
					for (Map.Entry<Long, BigDecimal> position : reverseList) {
						BigDecimal increasedPositionLoop = position.getValue().add(positionPercentage);
						int comparisonIncreasedPosition = newPosition.compareTo(increasedPositionLoop);
						if (comparisonIncreasedPosition > 0) {
							tempOrders.add(position.getKey());
						} else {
							break;
						}
					}					
					if(!tempOrders.isEmpty()) {
						long timeStamp = System.currentTimeMillis();
						String orderResult = spotClientImpl.createTrade().newOrder(OrdersParams.getOrderParams(
								tradeBot.getSymbol(),
								OrderSide.SELL,
								tradeBot.getQuoteOrderQty() * tempOrders.size(),
								timeStamp));
						
						JSONObject orderResultJson = new JSONObject(orderResult);
						
						// update orders in DB
						for (Long id : tempOrders) {
							OrderTracker order = OrderDB.getOneOrder(id);
							order.setSell(true);
							order.setSellPrice(newPosition);
							order.setSellDate(LocalDateTime.now());
							order.setSellOrderId(orderResultJson.getLong("orderId"));
							// update method...
						}						
					}
				}
			}

		} catch (BinanceConnectorException e) {
			System.err.println((String) String.format("fullErrMessage: %s", e.getMessage()));
		} catch (BinanceClientException e) {
			System.err.println((String) String.format("fullErrMessage: %s \nerrMessage: %s \nerrCode: %d \nHTTPStatusCode: %d",
			e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode()));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private void createBuyOrder(BigDecimal newPosition) throws Exception {
		long timeStamp = System.currentTimeMillis();
		String orderResult = spotClientImpl.createTrade().newOrder(OrdersParams.getOrderParams(
			   tradeBot.getSymbol(),
			   OrderSide.BUY,
			   tradeBot.getQuoteOrderQty(),
			   timeStamp));

		JSONObject orderResultJson = new JSONObject(orderResult);

		OrderTracker order = new OrderTracker();
		order.setBuyPrice(newPosition);
		order.setTradebot_id(tradeBot.getId());
		order.setBuyOrderId(orderResultJson.getLong("orderId"));

		long order_id = OrderDB.addOrder(order);
		positions.put(order_id, newPosition);
	}
}

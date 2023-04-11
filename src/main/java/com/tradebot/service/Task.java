package com.tradebot.service;

import com.binance.connector.client.impl.SpotClientImpl;
import com.tradebot.binance.SpotClientConfig;
import com.tradebot.configuration.OrdersParams;
import com.tradebot.db.ErrorTrackerDB;
import com.tradebot.db.OrderDB;
import com.tradebot.model.BotDTO;
import com.tradebot.model.ErrorTracker;
import com.tradebot.model.OrderSide;
import com.tradebot.model.OrderTracker;
import com.tradebot.model.TradeBot;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
	
	Map<Long, BigDecimal> positions;
	
	private boolean stopBotCycle;
	
	public Task(TradeBot tradeBot) throws Exception{
		spotClientImpl = SpotClientConfig.spotClientSignTest();
		this.tradeBot = tradeBot;
		positions = convertOrdersToMap(tradeBot);
	}	
	
	@Override
	public void run() {
		
		try {
			String result = spotClientImpl.createMarket().tickerSymbol(OrdersParams.getTickerSymbolParams(tradeBot.getSymbol()));
			JSONObject jsonObject = new JSONObject(result);			
			BigDecimal newPosition = new BigDecimal(jsonObject.getString("price"));
			
			if (BotExtraInfo.containsInfo(tradeBot.getTaskId())) {
				BotDTO botDTO = BotExtraInfo.getInfo(tradeBot.getTaskId());
				botDTO.setLastPrice(newPosition);
				if(botDTO.isStopCycle() && !stopBotCycle) {
					stopBotCycle = true;
				} 
				else if(!botDTO.isStopCycle() && stopBotCycle) {
					stopBotCycle = false;
				}
				botDTO.setLastCheck(new Date());
				BotExtraInfo.putInfo(tradeBot.getTaskId(), botDTO);
			} else {
				BotExtraInfo.putInfo(tradeBot.getTaskId(), new BotDTO(newPosition, false, new Date()));
			}

			if (positions.isEmpty()) {
				if(!stopBotCycle) {
					createBuyOrder(newPosition);
				}
				return;
			}
			
			Long lastAddedKey = null;

			for (Map.Entry<Long, BigDecimal> entry : positions.entrySet()) {
				lastAddedKey = entry.getKey();
			}
			
			int comparison = newPosition.compareTo(positions.get(lastAddedKey));
			BigDecimal positionPercentage = positions.get(lastAddedKey).multiply(new BigDecimal(tradeBot.getOrderStep()/100));
			
			// BUY
			if (comparison < 0 && (positions.size() < tradeBot.getCycleMaxOrders()) && !stopBotCycle) {
				BigDecimal decreasedPosition = positions.get(lastAddedKey).subtract(positionPercentage);
				int comparisonDecreaseed = newPosition.compareTo(decreasedPosition);
				if(comparisonDecreaseed < 0) {
					createBuyOrder(newPosition);
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
					
//					if(tempOrders.size() > 1) {
//						telegramBot.sendMessage("BINGO! " + tradeBot.getSymbol() + " " + tradeBot.getTaskId()
//							   + "Selling " + tempOrders.size() + " orders at once!");
//					}
					
					long timeStamp = System.currentTimeMillis();
					String orderResult = spotClientImpl.createTrade().newOrder(OrdersParams.getOrderParams(
							tradeBot.getSymbol(),
							OrderSide.SELL,
							tradeBot.getQuoteOrderQty() * tempOrders.size(),
							timeStamp));

					JSONObject orderResultJson = new JSONObject(orderResult);

					// update orders in DB and remove from map
					for (Long id : tempOrders) {
						OrderTracker order = OrderDB.getOneOrder(id);
						order.setSell(true);
						order.setSellPrice(newPosition);
						order.setSellDate(LocalDateTime.now());
						order.setSellOrderId(orderResultJson.getLong("orderId"));

						BigDecimal difference = newPosition.subtract(order.getBuyPrice());
						BigDecimal purchasedAmount = new BigDecimal(tradeBot.getQuoteOrderQty()).divide(order.getBuyPrice(), 8, RoundingMode.HALF_DOWN);
						BigDecimal earnings = difference.multiply(purchasedAmount);							
						order.setProfit(earnings);							
						OrderDB.updateOrder(order);
						positions.remove(id);

//							telegramBot.sendMessage("Sell\n" + tradeBot.getSymbol() + " " + tradeBot.getTaskId()
//								   + "\nquoteQty: " + tradeBot.getQuoteOrderQty()
//								   + "\nbuy price: " + order.getBuyPrice().setScale(2, RoundingMode.HALF_DOWN)
//								   + "\nsell price: " + newPosition.setScale(2, RoundingMode.HALF_DOWN)
//								   + "\nProfit: " + earnings.setScale(2, RoundingMode.HALF_DOWN));
					}
					if (positions.isEmpty() && !stopBotCycle) {
						createBuyOrder(newPosition);
					}
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			try {
				ErrorTracker errorTracker = new ErrorTracker();
				errorTracker.setErrorTimestamp(LocalDateTime.now());
				errorTracker.setErrorMessage(ex.getMessage());
				errorTracker.setTradebot_id(tradeBot.getId());
				ErrorTrackerDB.addError(errorTracker);
				
//				telegramBot.sendMessage("Error! " + tradeBot.getSymbol() + " " + tradeBot.getTaskId()
//					   + "\nMessage: " + ex.getMessage());
			} catch (Exception e) {
				e.printStackTrace();
			}
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
		
//		telegramBot.sendMessage("Buy\n" + tradeBot.getSymbol() + " " + tradeBot.getTaskId()
//			   + "\nquoteQty: " + tradeBot.getQuoteOrderQty()
//			   + "\nBuy price: " + order.getBuyPrice().setScale(2, RoundingMode.HALF_DOWN));
		
	}

	private Map<Long, BigDecimal> convertOrdersToMap(TradeBot bot) throws Exception {
		List<OrderTracker> orders = OrderDB.getOrdersFromBot(false, bot.getId());
		Map<Long, BigDecimal> map = new LinkedHashMap<>();
		for (OrderTracker order : orders) {
			map.put(order.getId(), order.getBuyPrice());
		}
		return map;
	}
}

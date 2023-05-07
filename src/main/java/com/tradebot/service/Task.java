package com.tradebot.service;

import com.binance.connector.client.exceptions.BinanceClientException;
import com.binance.connector.client.exceptions.BinanceConnectorException;
import com.binance.connector.client.exceptions.BinanceServerException;
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
import org.json.JSONObject;

public class Task implements Runnable {

     private TradeBot tradeBot;

     private SpotClientImpl spotClientImpl;

     private Map<Long, BigDecimal> positions;

     private boolean stopBotCycle;

     private final TelegramBot telegramBot;
	
	private LocalDateTime notifyTime;
	
     public Task(TradeBot tradeBot) throws Exception {
          this.spotClientImpl = SpotClientConfig.spotClientSignTest();
          this.tradeBot = tradeBot;
          this.positions = convertOrdersToMap(tradeBot);
          this.telegramBot = new TelegramBot();
		notifyTime = LocalDateTime.now();
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
                    if (botDTO.isStopCycle() && !stopBotCycle) {
                         stopBotCycle = true;
                    } else if (!botDTO.isStopCycle() && stopBotCycle) {
                         stopBotCycle = false;
                    }
                    botDTO.setLastCheck(new Date());
                    BotExtraInfo.putInfo(tradeBot.getTaskId(), botDTO);
               } else {
                    BotExtraInfo.putInfo(tradeBot.getTaskId(), new BotDTO(newPosition, false, new Date(), false));
               }

               if (positions.isEmpty()) {
                    if (!stopBotCycle) {
                         createBuyOrder(newPosition);
                    }
                    return;
               }

               Long lastAddedKey = null;

               for (Map.Entry<Long, BigDecimal> entry : positions.entrySet()) {
                    lastAddedKey = entry.getKey();
               }

               int comparison = newPosition.compareTo(positions.get(lastAddedKey));
               BigDecimal positionPercentage = positions.get(lastAddedKey).multiply(new BigDecimal(tradeBot.getOrderStep() / 100));

               // BUY
               if (comparison < 0 && (positions.size() < tradeBot.getCycleMaxOrders()) && !stopBotCycle) {
                    BigDecimal decreasedPosition = positions.get(lastAddedKey).subtract(positionPercentage);
                    int comparisonDecreaseed = newPosition.compareTo(decreasedPosition);
                    if (comparisonDecreaseed < 0) {
                         createBuyOrder(newPosition);
                    }
               }
			// STOP LOSS
			else if (comparison < 0 && (positions.size() >= tradeBot.getCycleMaxOrders())) {
				List<Long> tempOrdersStopLoss = new ArrayList<>();
				for (Map.Entry<Long, BigDecimal> position : positions.entrySet()) {
					BigDecimal positionPercentageStopLoss = position.getValue().multiply(new BigDecimal(tradeBot.getStopLoss() / 100));
					BigDecimal decreasedPositionStopLoss = position.getValue().subtract(positionPercentageStopLoss);
					int comparisonDecreaseedStopLoss = newPosition.compareTo(decreasedPositionStopLoss);
					if (comparisonDecreaseedStopLoss < 0) {
						tempOrdersStopLoss.add(position.getKey());
					} else {
						break;
					}
				}

				if(!tempOrdersStopLoss.isEmpty()) {
					
					if (LocalDateTime.now().compareTo(notifyTime) > 0) {
						
						telegramBot.sendMessage("Stop Loss triggered " + tradeBot.getSymbol() + " " + tradeBot.getTaskId() + 
							   " Price bellow " + tradeBot.getStopLoss() + "%");

						BotDTO botDTO = BotExtraInfo.getInfo(tradeBot.getTaskId());						
						if(!botDTO.isStopLossTriggered()) {
							botDTO.setStopLossTriggered(true);
							BotExtraInfo.putInfo(tradeBot.getTaskId(), botDTO);
						}
						
						notifyTime = notifyTime.plusHours(1);						
					}
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
					
					createSellOrder(newPosition, tempOrders);
					
					BotDTO botDTO = BotExtraInfo.getInfo(tradeBot.getTaskId());
					if (botDTO.isStopLossTriggered()) {
						botDTO.setStopLossTriggered(false);
						BotExtraInfo.putInfo(tradeBot.getTaskId(), botDTO);
					}
					
                         if (positions.isEmpty() && !stopBotCycle) {
                              createBuyOrder(newPosition);
                         }
                    }
               }

          } catch (BinanceConnectorException e) {
               e.printStackTrace();
               try {
                    addErrorMessage("(Binance Connector Exception) " + e.getMessage(), tradeBot.getId());
               } catch (Exception ex) {
                    ex.printStackTrace();
               }
          } catch (BinanceClientException e) {
               e.printStackTrace();
               try {
                    addErrorMessage("(Binance Client Exception) " + e.getErrMsg() + " " + e.getErrorCode() + " " + e.getHttpStatusCode(), tradeBot.getId());
                    telegramBot.sendMessage("Binance Client Exception\n" + e.getErrMsg() + " " + e.getErrorCode() + " " + e.getHttpStatusCode());
               } catch (Exception ex) {
                    ex.printStackTrace();
               }
          } catch (BinanceServerException e) {
               e.printStackTrace();
               try {
                    addErrorMessage("(Binance Server Exception) " + e.getMessage(), tradeBot.getId());
                    telegramBot.sendMessage("Binance Server Exception\n" + e.getMessage());
               } catch (Exception ex) {
                    ex.printStackTrace();
               }
          } catch (Exception e) {
               e.printStackTrace();
               try {
                    addErrorMessage(e.getMessage(), tradeBot.getId());
               } catch (Exception ex) {
                    ex.printStackTrace();
               }
          }
     }

     private void createBuyOrder(BigDecimal newPosition) throws Exception {
          long timeStamp = System.currentTimeMillis();
          String orderResult = spotClientImpl.createTrade().newOrder(OrdersParams.getOrderParams(
                  tradeBot.getSymbol(),
                  OrderSide.BUY,
                  new BigDecimal(tradeBot.getQuoteOrderQty()),
                  timeStamp));

          JSONObject orderResultJson = new JSONObject(orderResult);

          OrderTracker order = new OrderTracker();
          order.setBuyPrice(newPosition);
          order.setTradebot_id(tradeBot.getId());
          order.setBuyOrderId(orderResultJson.getLong("orderId"));
          long order_id = OrderDB.addOrder(order);
          positions.put(order_id, newPosition);
     }

     private Map<Long, BigDecimal> convertOrdersToMap(TradeBot bot) throws Exception {
          List<OrderTracker> orders = OrderDB.getOrdersFromBot(false, bot.getId());
          Map<Long, BigDecimal> map = new LinkedHashMap<>();
          for (OrderTracker order : orders) {
               map.put(order.getId(), order.getBuyPrice());
          }
          return map;
     }

     private void addErrorMessage(String message, long tradeBotId) throws Exception {
          ErrorTracker errorTracker = new ErrorTracker();
          errorTracker.setErrorTimestamp(LocalDateTime.now());
          errorTracker.setErrorMessage(message);
          errorTracker.setTradebot_id(tradeBotId);
          ErrorTrackerDB.addError(errorTracker);
     }
	
	private void createSellOrder(BigDecimal newPosition, List<Long> tempOrders) throws Exception {
		BigDecimal quoteSum = BigDecimal.ZERO;

		for (Long tempOrder : tempOrders) {
			BigDecimal temp = (new BigDecimal(tradeBot.getQuoteOrderQty()).divide(positions.get(tempOrder), 8, RoundingMode.DOWN)).multiply(newPosition);
			quoteSum = quoteSum.add(temp.setScale(8, RoundingMode.DOWN));
		}
		System.out.println("quoteSum " + quoteSum);
		long timeStamp = System.currentTimeMillis();
		String orderResult = spotClientImpl.createTrade().newOrder(OrdersParams.getOrderParams(
			   tradeBot.getSymbol(),
			   OrderSide.SELL,
			   quoteSum,
			   timeStamp));

		JSONObject orderResultJson = new JSONObject(orderResult);

		// update orders in DB and remove from map
		for (Long id : tempOrders) {
			OrderTracker order = OrderDB.getOneOrder(id);
			order.setSell(true);
			order.setSellPrice(newPosition);
			order.setSellDate(LocalDateTime.now());
			order.setSellOrderId(orderResultJson.getLong("orderId"));
			BigDecimal temp = (new BigDecimal(tradeBot.getQuoteOrderQty()).divide(positions.get(id), 8, RoundingMode.DOWN)).multiply(newPosition);
			BigDecimal earnings = temp.subtract(new BigDecimal(tradeBot.getQuoteOrderQty()));
			order.setProfit(earnings.setScale(8, RoundingMode.DOWN));
			OrderDB.updateOrder(order);
			positions.remove(id);
		}
	}
}

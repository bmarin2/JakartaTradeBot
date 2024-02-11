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
import com.tradebot.model.PositionDTO;
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

public class SpotTask implements Runnable {

     private TradeBot tradeBot;

     private SpotClientImpl spotClientImpl;

     private Map<Long, PositionDTO> positions;

     private boolean stopBotCycle;

     private final TelegramBot telegramBot;
	
     public SpotTask(TradeBot tradeBot) throws Exception {
          this.spotClientImpl = SpotClientConfig.spotClientSignTest();
          this.tradeBot = tradeBot;
          this.positions = convertOrdersToMap(tradeBot);
          this.telegramBot = new TelegramBot();
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

				if (tradeBot.getPriceGridLimit() > 0.0) {
					if (newPosition.doubleValue() > tradeBot.getPriceGridLimit() && !botDTO.isStopCycle()) {
						botDTO.setStopCycle(true);

						telegramBot.sendMessage("Price over grid limit, stopping buying\n"
							   + tradeBot.getSymbol() + " " + tradeBot.getTaskId());

					} else if (newPosition.doubleValue() < tradeBot.getPriceGridLimit() && botDTO.isStopCycle()) {

						telegramBot.sendMessage("Price under grid limit, continuing buying\n"
							   + tradeBot.getSymbol() + " " + tradeBot.getTaskId());

						botDTO.setStopCycle(false);
					}
				}

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
                    } else {
                         return;
                    }
               }

               if (tradeBot.isEnableStopLoss()) {
                    checkStopLoss(newPosition);
               }

               Long lastAddedKey = null;

               for (Map.Entry<Long, PositionDTO> entry : positions.entrySet()) {
                    lastAddedKey = entry.getKey();
               }
			
               int comparison = newPosition.compareTo(positions.get(lastAddedKey).getBuyPrice());
               BigDecimal positionPercentage = positions.get(lastAddedKey).getBuyPrice().multiply(new BigDecimal(tradeBot.getOrderStep() / 100));

               // BUY
               if (comparison < 0 && (positions.size() < tradeBot.getCycleMaxOrders()) && !stopBotCycle) {
                    BigDecimal decreasedPosition = positions.get(lastAddedKey).getBuyPrice().subtract(positionPercentage);
                    int comparisonDecreaseed = newPosition.compareTo(decreasedPosition);
                    if (comparisonDecreaseed < 0) {
                         createBuyOrder(newPosition);
                    }
               }
               // SELL
               else if (comparison > 0 && !positions.isEmpty()) {
                    BigDecimal increasedPosition = positions.get(lastAddedKey).getBuyPrice().add(positionPercentage);
                    int comparisonIncreased = newPosition.compareTo(increasedPosition);
                    if (comparisonIncreased > 0) {
                         List<Map.Entry<Long, PositionDTO>> reverseList = new ArrayList<>(positions.entrySet());
                         Collections.reverse(reverseList);
                         List<Long> tempOrders = new ArrayList<>();
                         for (Map.Entry<Long, PositionDTO> position : reverseList) {
                              BigDecimal increasedPositionLoop = position.getValue().getBuyPrice().add(positionPercentage);
                              int comparisonIncreasedPosition = newPosition.compareTo(increasedPositionLoop);
                              if (comparisonIncreasedPosition > 0) {
                                   tempOrders.add(position.getKey());
                              } else {
                                   break;
                              }
                         }
					
					createSellOrder(newPosition, tempOrders);
					
					BotDTO botDTO = BotExtraInfo.getInfo(tradeBot.getTaskId());
					if (botDTO.isStopLossWarningTriggered()) {
						botDTO.setStopLossWarningTriggered(false);
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
          order.setStopLossPrice(calcStopLossPrice(newPosition));
          long order_id = OrderDB.addOrder(order);
		PositionDTO positionDTO = new PositionDTO(order.getBuyPrice(), order.getStopLossPrice());
          positions.put(order_id, positionDTO);
     }
	
	private BigDecimal calcStopLossPrice(BigDecimal pos) {
		BigDecimal positionPercentageStopLoss = pos.multiply(new BigDecimal(tradeBot.getStopLoss() / 100));
		return pos.subtract(positionPercentageStopLoss);
	}

     private Map<Long, PositionDTO> convertOrdersToMap(TradeBot bot) throws Exception {
          List<OrderTracker> orders = OrderDB.getOrdersFromBot(false, bot.getId());
          Map<Long, PositionDTO> map = new LinkedHashMap<>();
          for (OrderTracker order : orders) {
			PositionDTO positionDTO = new PositionDTO(order.getBuyPrice(), order.getStopLossPrice());
               map.put(order.getId(), positionDTO);
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
               if (tradeBot.isProfitBase()) {
                    BigDecimal temp = new BigDecimal(tradeBot.getQuoteOrderQty());
                    quoteSum = quoteSum.add(temp.setScale(8, RoundingMode.DOWN));
               } else {
                    BigDecimal temp = (new BigDecimal(tradeBot.getQuoteOrderQty()).divide(positions.get(tempOrder).getBuyPrice(), 8, RoundingMode.DOWN)).multiply(newPosition);
                    quoteSum = quoteSum.add(temp.setScale(8, RoundingMode.DOWN));
               }
		}

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
			BigDecimal temp = (new BigDecimal(tradeBot.getQuoteOrderQty()).divide(positions.get(id).getBuyPrice(), 8, RoundingMode.DOWN)).multiply(newPosition);
			BigDecimal earnings = temp.subtract(new BigDecimal(tradeBot.getQuoteOrderQty()));
			order.setProfit(earnings.setScale(8, RoundingMode.DOWN));
			OrderDB.updateOrder(order);
			positions.remove(id);
		}
	}
	
	private void checkStopLoss(BigDecimal newPosition) throws Exception {

		List<Long> tempOrdersStopLoss = new ArrayList<>();

		for (Map.Entry<Long, PositionDTO> position : positions.entrySet()) {
			if (newPosition.compareTo(position.getValue().getStopLossPrice()) < 0) {
				tempOrdersStopLoss.add(position.getKey());
				
				BotDTO botDTO = BotExtraInfo.getInfo(tradeBot.getTaskId());
				if (!botDTO.isStopLossWarningTriggered()) {
					botDTO.setStopLossWarningTriggered(true);
					BotExtraInfo.putInfo(tradeBot.getTaskId(), botDTO);
				}
			}
			else {
				break;
			}
		}
		
		if (!tempOrdersStopLoss.isEmpty()) {
			createSellOrder(newPosition, tempOrdersStopLoss);
			telegramBot.sendMessage("Sold at loss of " + tradeBot.getStopLoss() + "% Number of orders sold: "
				   + tempOrdersStopLoss.size() + " " + tradeBot.getSymbol() + " " + tradeBot.getTaskId());			
		}
	}
}

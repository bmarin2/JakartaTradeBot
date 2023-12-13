package com.tradebot.service;

import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.tradebot.binance.UMFuturesClientConfig;
import com.tradebot.configuration.FuturesOrderParams;
import com.tradebot.db.AlarmDB;
import com.tradebot.enums.PositionSide;
import com.tradebot.model.Alarm;
import com.tradebot.model.FuturesBot;
import com.tradebot.model.OrderSide;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;

// Take profit version
public class FuturesTaskTwoCrossTP implements Runnable {

     private FuturesBot futuresBot;
     private UMFuturesClientImpl umFuturesClientImpl;
     private final TelegramBot telegramBot;

     private boolean currentCross; // cross: fast -> slow
     private boolean currentCrossBig; // cross: fask -> third

     private String currentSPOrder = "";
     private String currentTPOrder = "";
     private PositionSide currentPositionSide;
     private Double entryPrice;

     public FuturesTaskTwoCrossTP(FuturesBot futuresBot) {
          this.futuresBot = futuresBot;
          umFuturesClientImpl = UMFuturesClientConfig.futuresSignedTest();
          initDemas();
          currentSPOrder = initStopLossOrder();
          currentTPOrder = initTakeProfitOrder();
          currentPositionSide = initExistingPosition();
          System.out.println("init side: " + currentPositionSide);
          if (currentPositionSide != PositionSide.NONE) {
               entryPrice = getEntryPrice();
               System.out.println("init entry price; " + entryPrice);
          }

          this.telegramBot = new TelegramBot();
     }

     @Override
     public void run() {
          if (!currentSPOrder.isEmpty()) {
               if (getOrderStatus(currentSPOrder).equals("FILLED")) {

                    System.out.println("SP Triggered");

                    try {
                         telegramBot.sendMessage("Stop Loss triggered " + futuresBot.getSymbol() + "\n"
					+ "Realized PNL: " + getRealizedPNL(currentSPOrder));
                    } catch (Exception e) {
                         e.printStackTrace();
                    }

                    cancelOrder(currentTPOrder);
				currentPositionSide = PositionSide.NONE;
				currentSPOrder = "";
               }
          }

          Alarm alarm = null;

          try {
               alarm = AlarmDB.getOneAlarm(futuresBot.getDemaAlertTaskId());
          } catch (Exception e) {
               e.printStackTrace();
          }

		System.out.println("SP order: " + currentSPOrder);
          System.out.println("TP order: " + currentTPOrder);
		System.out.println("----------------");
          System.out.println("DB Cross: " + alarm.getCrosss());
          System.out.println("cross: " + currentCross);
		System.out.println("----------------");
		System.out.println("DB CrossBig: " + alarm.getCrosssBig());
          System.out.println("crossBig: " + currentCrossBig);
		System.out.println("----------------");
          System.out.println("pos: " + currentPositionSide.toString());
          
          // ====================================================================================
		
		if (currentCrossBig != alarm.getCrosssBig()) {

			currentCrossBig = alarm.getCrosssBig();
			System.out.println("cross2 is now: " + currentCrossBig);
			
			if (currentPositionSide == PositionSide.NONE) {
				if (currentCrossBig) {

					try {
						telegramBot.sendMessage("Entering SHORT trade from cross BIG " + getTime());
					} catch (IOException e) {
						e.printStackTrace();
					}

					createShortOrder();
					entryPrice = getEntryPrice();
					createStopLoss(OrderSide.BUY, calctulateSP(PositionSide.SHORT));
                         createTakeProfit(OrderSide.BUY, calctulateTP(PositionSide.SHORT));
					currentPositionSide = PositionSide.SHORT;

				} else {
					try {
						telegramBot.sendMessage("Entering LONG trade from cross BIG " + getTime());
					} catch (IOException e) {
						e.printStackTrace();
					}

					createLongOrder();
					entryPrice = getEntryPrice();
					createStopLoss(OrderSide.SELL, calctulateSP(PositionSide.LONG));
                         createTakeProfit(OrderSide.SELL, calctulateTP(PositionSide.LONG));
					currentPositionSide = PositionSide.LONG;
				}
			}			
		}
          
          if (currentCross != alarm.getCrosss()) {
               
               currentCross = alarm.getCrosss();
               System.out.println("cross1 is now: " + currentCross);
			

               if (alarm.getCrosss()) { // if crossed DOWN
                    if (currentPositionSide == PositionSide.SHORT) {
                         return;
                    }

                    if (currentPositionSide == PositionSide.LONG) {
                         // take profit
                         String orderId = createShortOrder();
					
					try {
						double pnl = getRealizedPNL(orderId);
						telegramBot.sendMessage("Taking profit from LONG " + getTime() + "\n "
							   + "Realized PNL: " + String.format("%.2f", pnl) + "USDT");
					} catch (Exception e) {
						e.printStackTrace();
					}

                         // cancel old SP
                         if (!currentSPOrder.isEmpty()) {
                              if (getOrderStatus(currentSPOrder).equals("NEW")) {
                                   cancelOrder(currentSPOrder);
                                   currentSPOrder = "";
                              }
                         }

                         // cancel old TP
                         if (!currentTPOrder.isEmpty()) {
                              if (getOrderStatus(currentTPOrder).equals("NEW")) {
                                   cancelOrder(currentTPOrder);
                                   currentTPOrder = "";
                              }
                         }

					currentPositionSide = PositionSide.NONE;
                    }
				
				if (currentCrossBig) {
					try {
						telegramBot.sendMessage("Entering SHORT trade " + getTime());
					} catch (IOException e) {
						e.printStackTrace();
					}

					createShortOrder();
					entryPrice = getEntryPrice();
					createStopLoss(OrderSide.BUY, calctulateSP(PositionSide.SHORT));
                         createTakeProfit(OrderSide.BUY, calctulateTP(PositionSide.SHORT));
					currentPositionSide = PositionSide.SHORT;
				}				


               } else { // if crossed UP
                    if (currentPositionSide == PositionSide.LONG) {
                         return;
                    }
                    if (currentPositionSide == PositionSide.SHORT) {
					// take profit
					String orderId = createLongOrder();

					try {
						double pnl = getRealizedPNL(orderId);
						telegramBot.sendMessage("Taking profit from SHORT " + getTime() + "\n "
							   + "Realized PNL: " + String.format("%.2f", pnl) + "USDT ");
					} catch (Exception e) {
						e.printStackTrace();
					}                         

                         // cancel old SP
                         if (!currentSPOrder.isEmpty()) {
                              if (getOrderStatus(currentSPOrder).equals("NEW")) {
                                   cancelOrder(currentSPOrder);
                                   currentSPOrder = "";
                              }
                         }

                         // cancel old TP
                         if (!currentTPOrder.isEmpty()) {
                              if (getOrderStatus(currentTPOrder).equals("NEW")) {
                                   cancelOrder(currentTPOrder);
                                   currentTPOrder = "";
                              }
                         }

					currentPositionSide = PositionSide.NONE;
                    }
				
				if (!currentCrossBig) {
					try {
						telegramBot.sendMessage("Entering LONG trade " + getTime());
					} catch (IOException e) {
						e.printStackTrace();
					}

					createLongOrder();
					entryPrice = getEntryPrice();
					createStopLoss(OrderSide.SELL, calctulateSP(PositionSide.LONG));
                         createTakeProfit(OrderSide.SELL, calctulateTP(PositionSide.LONG));
					currentPositionSide = PositionSide.LONG;
				}
               }
          }
     }

     // ==========================================================================================     

     private String createLongOrder() {
          return createOrder(OrderSide.BUY);          
     }

     private String createShortOrder() {
          return createOrder(OrderSide.SELL);        
     }

     private String createOrder(OrderSide orderSide) {
          long timeStamp = System.currentTimeMillis();

          String orderResult = umFuturesClientImpl.account().newOrder(
                  FuturesOrderParams.getOrderParams(futuresBot.getSymbol(), orderSide,
                          OrderSide.BOTH, futuresBot.getQuantity(), timeStamp)
          );

          JSONObject jsonResult = new JSONObject(orderResult);
          String orderId = jsonResult.optString("orderId");

          System.out.println("Order created " + orderSide.toString() + " ID: " + orderId);
		
		return orderId;
     }
     
     private void createTakeProfit(OrderSide orderSide, String stopPrice) {
          System.out.println("stopPrice TP: " + stopPrice);
          long timeStamp = System.currentTimeMillis();

          String orderResult = umFuturesClientImpl.account().newOrder(
                  FuturesOrderParams.getTakeProfitParams(futuresBot.getSymbol(),
                          orderSide, OrderSide.BOTH, futuresBot.getQuantity(),
                          stopPrice, timeStamp)
          );
          System.out.println("orderResult TP: " + orderResult);
          JSONObject jsonResult = new JSONObject(orderResult);
          currentTPOrder = jsonResult.optString("orderId");
          System.out.println("Take Profit Order ID: " + currentTPOrder);
     }

     private void createStopLoss(OrderSide orderSide, String stopPrice) {
          long timeStamp = System.currentTimeMillis();

          String orderResult = umFuturesClientImpl.account().newOrder(
                  FuturesOrderParams.getStopLossParams(futuresBot.getSymbol(),
                          orderSide, OrderSide.BOTH, futuresBot.getQuantity(),
                          stopPrice, timeStamp)
          );

          JSONObject jsonResult = new JSONObject(orderResult);
          currentSPOrder = jsonResult.optString("orderId");
          System.out.println("SP Order ID: " + currentSPOrder);
     }

     private void cancelOrder(String orderId) {
          long timeStamp = System.currentTimeMillis();
          umFuturesClientImpl.account().cancelOrder(
                  FuturesOrderParams.getCancelOrderParams(futuresBot.getSymbol(),
                          orderId, timeStamp)
          );
     }

     private String getOrderStatus(String orderId) {
          long timeStamp = System.currentTimeMillis();
          String orderResult = umFuturesClientImpl.account().queryOrder(
                  FuturesOrderParams.getQueryOrderParams(futuresBot.getSymbol(),
                          orderId, timeStamp)
          );
          JSONObject jsonResult = new JSONObject(orderResult);
          String status = jsonResult.optString("status");
          return status;
     }

     private Double getTickerPrice() {
          String result = umFuturesClientImpl.market().tickerSymbol(
                  FuturesOrderParams.getTickerParams(futuresBot.getSymbol())
          );
          JSONObject jsonResult = new JSONObject(result);
          return jsonResult.optDouble("price");
     }

     private String calctulateSP(PositionSide positionSide) {
          Double price = entryPrice;
          Double percent = (futuresBot.getStopLoss() / 100) * price;
          Double result = 0.0;
          if (positionSide == PositionSide.SHORT) {
               result = price + percent;
          } else if (positionSide == PositionSide.LONG){
               result = price - percent;
          }
          String resultFormated = String.format("%.2f", result);
          return resultFormated;
     }
     
     private String calctulateTP(PositionSide positionSide) {
          Double price = entryPrice;
          Double percent = (0.6 / 100) * price;
          Double result = 0.0;
          if (positionSide == PositionSide.SHORT) {
               result = price - percent;
          } else if (positionSide == PositionSide.LONG) {
               result = price + percent;
          }
          String resultFormated = String.format("%.2f", result);
          return resultFormated;
     }

     private void initDemas() {
          try {
               Alarm alarm = AlarmDB.getOneAlarm(futuresBot.getDemaAlertTaskId());
               currentCross = alarm.getCrosss();
               currentCrossBig = alarm.getCrosssBig();
          } catch (Exception e) {
               e.printStackTrace();
          }
          System.out.println("init currentCross: " + currentCross);
     }

     private PositionSide initExistingPosition() {
          long timeStamp = System.currentTimeMillis();
          String jsonResult = umFuturesClientImpl.account().positionInformation(
                  FuturesOrderParams.getParams(futuresBot.getSymbol(), timeStamp)
          );
          JSONArray positions = new JSONArray(jsonResult);
          JSONObject positionObject = positions.getJSONObject(0);
          System.out.println("price: " + positionObject.optString("entryPrice"));
          if (!positionObject.optString("entryPrice").equals("0.00000000")) {
               String side = fetchPositionSide();
               if (side.equals("BUY")) {
                    return PositionSide.LONG;
               } else if (side.equals("SELL")){
                    return PositionSide.SHORT;
               }
          }
          return PositionSide.NONE;
     }
     
     private String fetchPositionSide() {
          long timeStamp = System.currentTimeMillis();
          String jsonResult = umFuturesClientImpl.account().accountTradeList(
                  FuturesOrderParams.getParams(futuresBot.getSymbol(), timeStamp));

          JSONArray positions = new JSONArray(jsonResult);
          JSONObject tradeObject = positions.getJSONObject(positions.length() - 1);

          return tradeObject.optString("side");
     }
	
	private double getRealizedPNL(String orderId) {
		long timeStamp = System.currentTimeMillis();
          String jsonResult = umFuturesClientImpl.account().accountTradeList(
                  FuturesOrderParams.getQueryOrderParams(futuresBot.getSymbol(), orderId, timeStamp));

		JSONArray tradeList = new JSONArray(jsonResult);
		JSONObject tradeObject = tradeList.getJSONObject(0);
		
          return tradeObject.optDouble("realizedPnl");
	}

     private String initStopLossOrder() {
          String orderId = "";

          long timeStamp = System.currentTimeMillis();
          String jsonResult = umFuturesClientImpl.account().currentAllOpenOrders(
                  FuturesOrderParams.getParams(futuresBot.getSymbol(), timeStamp));

          JSONArray orders = new JSONArray(jsonResult);

          for (int i = 0; i < orders.length(); i++) {
               JSONObject orderObject = orders.getJSONObject(i);

               if ("STOP_MARKET".equals(orderObject.optString("type"))) {
                    orderId = Long.toString(orderObject.optLong("orderId"));
                    break;
               }
          }
          System.out.println("init SP order: " + orderId);
          return orderId;
     }
     
     private String initTakeProfitOrder() {
          String orderId = "";

          long timeStamp = System.currentTimeMillis();
          String jsonResult = umFuturesClientImpl.account().currentAllOpenOrders(
                  FuturesOrderParams.getParams(futuresBot.getSymbol(), timeStamp));

          JSONArray orders = new JSONArray(jsonResult);

          for (int i = 0; i < orders.length(); i++) {
               JSONObject orderObject = orders.getJSONObject(i);

               if ("TAKE_PROFIT".equals(orderObject.optString("type"))) {
                    orderId = Long.toString(orderObject.optLong("orderId"));
                    break;
               }
          }
          System.out.println("init TP order: " + orderId);
          return orderId;
     }

     private double getEntryPrice() {
          long timeStamp = System.currentTimeMillis();
          String jsonResult = umFuturesClientImpl.account().positionInformation(
                  FuturesOrderParams.getParams(futuresBot.getSymbol(), timeStamp)
          );
          JSONObject positionObject = new JSONArray(jsonResult).getJSONObject(0);
          return positionObject.optDouble("entryPrice");
     }

	private String getTime() {
		LocalDateTime currentDateTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		return currentDateTime.format(formatter);
	}

	private String getPercentageIncrease(double entry, double increase) {
		return String.format("%.2f", (increase / entry) * 100);
	}
}

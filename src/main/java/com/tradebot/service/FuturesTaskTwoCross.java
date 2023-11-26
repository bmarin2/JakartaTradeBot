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

public class FuturesTaskTwoCross implements Runnable {

     private FuturesBot futuresBot;
     private UMFuturesClientImpl umFuturesClientImpl;
     private final TelegramBot telegramBot;

     private boolean currentCross; // cross: fast -> slow
     private boolean currentCrossBig; // cross: fask -> third

     private String currentSPOrder = "";
     private PositionSide currentPositionSide;
     private Double entryPrice;

     public FuturesTaskTwoCross(FuturesBot futuresBot) {
          this.futuresBot = futuresBot;
          umFuturesClientImpl = UMFuturesClientConfig.futuresSignedTest();
          initDemas();
          currentSPOrder = initStopLossOrder();
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
                    currentPositionSide = PositionSide.NONE;
                    currentSPOrder = "";
                    System.out.println("SP Triggered");

                    try {
                         telegramBot.sendMessage("Stop Loss triggered " + futuresBot.getSymbol());
                    } catch (Exception e) {
                         e.printStackTrace();
                    }
               }
          }

          Alarm alarm = null;

          try {
               alarm = AlarmDB.getOneAlarm(futuresBot.getDemaAlertTaskId());
          } catch (Exception e) {
               e.printStackTrace();
          }
		
		System.out.println("Time: " + getTime());
		System.out.println("SP order: " + currentSPOrder);
		System.out.println("----------------");
          System.out.println("DB Cross: " + alarm.getCrosss());
          System.out.println("cross: " + currentCross);
		System.out.println("----------------");
		System.out.println("DB CrossBig: " + alarm.getCrosssBig());
          System.out.println("crossBig: " + currentCrossBig);
		System.out.println("----------------");
          System.out.println("pos: " + currentPositionSide.toString());
          
          if (currentPositionSide != PositionSide.NONE && !isDistantFromEntryPrice()) {
               System.out.println("\nNOT DISTANT FROM ENTRY PRICE!");
               currentCross = alarm.getCrosss();
               currentCrossBig = alarm.getCrosssBig();
               return;
          }

          System.out.println("\nContinuing...");

          
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
					
					try {
						double profit = getUnrealizedProfit();
						telegramBot.sendMessage("Taking profit from LONG " + getTime() + "\n "
							   + "Profit: " + String.format("%.2f", profit) + "USDT "
							   + "(" + getPercentageIncrease(entryPrice, profit) + "%)");
					} catch (Exception e) {
						e.printStackTrace();
					}
					
                         // take profit
                         createShortOrder();

                         // cancel old SP
                         if (!currentSPOrder.isEmpty()) {
                              if (getOrderStatus(currentSPOrder).equals("NEW")) {
                                   cancelOrder(currentSPOrder);
                                   currentSPOrder = "";
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
					currentPositionSide = PositionSide.SHORT;
				}				


               } else { // if crossed UP
                    if (currentPositionSide == PositionSide.LONG) {
                         return;
                    }
                    if (currentPositionSide == PositionSide.SHORT) {
					
					try {
						double profit = getUnrealizedProfit();
						telegramBot.sendMessage("Taking profit from SHORT " + getTime() + "\n "
							   + "Profit: " + String.format("%.2f", profit) + "USDT ");
					} catch (Exception e) {
						e.printStackTrace();
					}
					
                         createLongOrder(); // take profit

                         // cancel old SP
                         if (!currentSPOrder.isEmpty()) {
                              if (getOrderStatus(currentSPOrder).equals("NEW")) {
                                   cancelOrder(currentSPOrder);
                                   currentSPOrder = "";
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
					currentPositionSide = PositionSide.LONG;
				}
               }
          }
     }

     // ==========================================================================================     

     private void createLongOrder() {
          createOrder(OrderSide.BUY);          
     }

     private void createShortOrder() {
          createOrder(OrderSide.SELL);        
     }

     private void createOrder(OrderSide orderSide) {
          long timeStamp = System.currentTimeMillis();

          String orderResult = umFuturesClientImpl.account().newOrder(
                  FuturesOrderParams.getOrderParams(futuresBot.getSymbol(), orderSide,
                          OrderSide.BOTH, futuresBot.getQuantity(), timeStamp)
          );

          JSONObject jsonResult = new JSONObject(orderResult);
          String orderId = jsonResult.optString("orderId");
          String price = jsonResult.optString("price");

          System.out.println("Order created " + orderSide.toString() + " ID: " + orderId);
          System.out.println("price: " + price);
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
          
          if (!positionObject.optString("entryPrice").equals("0.0")) {
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

     private double getEntryPrice() {
          long timeStamp = System.currentTimeMillis();
          String jsonResult = umFuturesClientImpl.account().positionInformation(
                  FuturesOrderParams.getParams(futuresBot.getSymbol(), timeStamp)
          );
          JSONObject positionObject = new JSONArray(jsonResult).getJSONObject(0);
          return positionObject.optDouble("entryPrice");
     }
	
	private double getUnrealizedProfit() {
          long timeStamp = System.currentTimeMillis();
          String jsonResult = umFuturesClientImpl.account().positionInformation(
                  FuturesOrderParams.getParams(futuresBot.getSymbol(), timeStamp)
          );
          JSONObject positionObject = new JSONArray(jsonResult).getJSONObject(0);
          return positionObject.optDouble("unRealizedProfit");
     }
     
     private boolean isDistantFromEntryPrice() {
          double newPrice = getTickerPrice();
          switch (currentPositionSide) {
               case LONG:
                    return newPrice > entryPrice;
               case SHORT:
                    return newPrice < entryPrice;
               case NONE:
                    return true;
               default:
                    return true;
          }    
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

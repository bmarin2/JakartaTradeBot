package com.tradebot.service;

import com.binance.connector.futures.client.exceptions.BinanceClientException;
import com.binance.connector.futures.client.exceptions.BinanceConnectorException;
import com.binance.connector.futures.client.exceptions.BinanceServerException;
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

public class FuturesTaskOneCrossBorder implements Runnable {

     private FuturesBot futuresBot;
     private UMFuturesClientImpl umFuturesClientImpl;
     private final TelegramBot telegramBot;

     private boolean currentCross; // cross: fast -> slow

     private String currentSPOrder = "";
     private PositionSide currentPositionSide;
     private Double entryPrice;
     private Double borderPrice;
     private boolean borderCrossed;     

     public FuturesTaskOneCrossBorder(FuturesBot futuresBot) {
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
               String status = getOrderStatus(currentSPOrder);
               if (status.equals("FILLED") || status.equals("CANCELED")) {
                    currentPositionSide = PositionSide.NONE;
                    currentSPOrder = "";
                    clearFields();
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
          System.out.println("DB Cross: " + alarm.getCrosss());
          System.out.println("cross: " + currentCross);
          System.out.println("pos: " + currentPositionSide.toString());
          System.out.println("----");
          System.out.println("entry price: " + entryPrice);
          System.out.println("border crossed: " + borderCrossed);
          System.out.println("border price: " + borderPrice);
          
          
          if (currentPositionSide == PositionSide.LONG) {
               Double currentPrice = getTickerPrice();
               if (currentPrice > borderPrice && !borderCrossed) {
                    System.out.println("Border crossed");
                    borderCrossed = true;
               } else if (currentPrice < borderPrice && borderCrossed) {
                    // take profit
                    String orderId = createShortOrder();                   
                    
                    try {
                         double profit = getRealizedPNL(orderId);
                         telegramBot.sendMessage("Border crossed on LONG, OrederId: " + orderId + " " + getTime() + "\n "
                                 + "Profit: " + profit + "USDT ");
                    } catch (Exception e) {
                         e.printStackTrace();
                    }

                    clearFields();
                    cancelCurrentSPOrder();                    
                    currentPositionSide = PositionSide.NONE;

               }
          } else if (currentPositionSide == PositionSide.SHORT) {
               Double currentPrice = getTickerPrice();
               if (currentPrice < borderPrice && !borderCrossed) {
                    System.out.println("Border crossed");
                    borderCrossed = true;
               } else if (currentPrice > borderPrice && borderCrossed) {
                    // take profit
                    String orderId = createLongOrder();

                    try {
                         double profit = getRealizedPNL(orderId);
                         telegramBot.sendMessage("Border crossed on SHORT, OrederId: " + orderId + " " + getTime() + "\n "
                                 + "Profit: " + profit + "USDT ");
                    } catch (Exception e) {
                         e.printStackTrace();
                    }

                    clearFields();
                    cancelCurrentSPOrder();                    
                    currentPositionSide = PositionSide.NONE;

               }
          }

          System.out.println("\nContinuing...");


          // ====================================================================================          

          if (currentCross != alarm.getCrosss()) {
               
               currentCross = alarm.getCrosss();
               System.out.println("cross is now: " + currentCross);

               if (alarm.getCrosss()) { // if crossed DOWN
                    if (currentPositionSide == PositionSide.SHORT) {
                         return;
                    }

                    if (currentPositionSide == PositionSide.LONG) {
                         
                         String orderId = createShortOrder();
					
					try {
						double profit = getRealizedPNL(orderId);
						telegramBot.sendMessage("Realized profit from LONG, OrederId: " + orderId + " " + getTime() + "\n "
							   + "Profit: " + profit + "USDT ");
					} catch (Exception e) {
						e.printStackTrace();
					}
                         
                         clearFields();
                         cancelCurrentSPOrder();
                    }
				
				try {
					telegramBot.sendMessage("Entering SHORT trade " + getTime());
				} catch (IOException e) {
					e.printStackTrace();
				}
				
                    createShortOrder();
                    entryPrice = getEntryPrice();
                    borderPrice = calculateBorderPrice(PositionSide.SHORT);
                    createStopLoss(OrderSide.BUY, calctulateSP(PositionSide.SHORT));
                    currentPositionSide = PositionSide.SHORT;

               } else { // if crossed UP
                    if (currentPositionSide == PositionSide.LONG) {
                         return;
                    }
                    if (currentPositionSide == PositionSide.SHORT) {
                         
                         String orderId = createLongOrder(); // take profit
					
					try {
						double profit = getRealizedPNL(orderId);
						telegramBot.sendMessage("Taking profit from SHORT, OrederId: " + orderId + " " + getTime() + "\n "
							   + "Profit: " + profit + "USDT ");
					} catch (Exception e) {
						e.printStackTrace();
					}
                         
                         clearFields();
                         cancelCurrentSPOrder();
                    }
				
				try {
					telegramBot.sendMessage("Entering LONG trade " + getTime());
				} catch (IOException e) {
					e.printStackTrace();
				}
				
                    createLongOrder();
                    entryPrice = getEntryPrice();
                    borderPrice = calculateBorderPrice(PositionSide.LONG);
                    createStopLoss(OrderSide.SELL, calctulateSP(PositionSide.LONG));
                    currentPositionSide = PositionSide.LONG;
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
          String orderResult = "";
          String orderId = "";

          try {
               long timeStamp = System.currentTimeMillis();
               orderResult = umFuturesClientImpl.account().newOrder(
                       FuturesOrderParams.getOrderParams(futuresBot.getSymbol(), orderSide,
                               OrderSide.BOTH, futuresBot.getQuantity(), timeStamp)
               );
          } catch (BinanceConnectorException e) {
               System.out.println("BinanceConnectorException");
               e.printStackTrace();
          } catch (BinanceClientException e) {
               System.out.println("BinanceClientException");
               e.printStackTrace();
          } catch (BinanceServerException e) {
               System.out.println("BinanceServerException");
               e.printStackTrace();
          }

          JSONObject jsonResult = new JSONObject(orderResult);
          orderId = jsonResult.optString("orderId");

          System.out.println("Order created " + orderSide.toString() + " ID: " + orderId);

          return orderId;
     }

     private void createStopLoss(OrderSide orderSide, String stopPrice) {
          String orderResult = "";

          try {               
               long timeStamp = System.currentTimeMillis();
               orderResult = umFuturesClientImpl.account().newOrder(
                       FuturesOrderParams.getStopLossParams(futuresBot.getSymbol(),
                               orderSide, OrderSide.BOTH, futuresBot.getQuantity(),
                               stopPrice, timeStamp)
               );
          } catch (BinanceConnectorException e) {
               System.out.println("BinanceConnectorException");
               e.printStackTrace();
          } catch (BinanceClientException e) {
               System.out.println("BinanceClientException");
               e.printStackTrace();
          } catch (BinanceServerException e) {
               System.out.println("BinanceServerException");
               e.printStackTrace();
          }
          
          JSONObject jsonResult = new JSONObject(orderResult);
          currentSPOrder = jsonResult.optString("orderId");
          System.out.println("SP Order ID: " + currentSPOrder);          
     }

     private void cancelOrder(String orderId) {        
          try {
               long timeStamp = System.currentTimeMillis();
               umFuturesClientImpl.account().cancelOrder(
                       FuturesOrderParams.getCancelOrderParams(futuresBot.getSymbol(),
                               orderId, timeStamp)
               );
          } catch (BinanceConnectorException e) {
               System.out.println("BinanceConnectorException");
               e.printStackTrace();
          } catch (BinanceClientException e) {
               System.out.println("BinanceClientException");
               e.printStackTrace();
          } catch (BinanceServerException e) {
               System.out.println("BinanceServerException");
               e.printStackTrace();
          }          
     }
     
     private void cancelCurrentSPOrder() {
          if (!currentSPOrder.isEmpty()) {
               if (getOrderStatus(currentSPOrder).equals("NEW")) {
                    cancelOrder(currentSPOrder);
                    currentSPOrder = "";
               }
          }
     }

     private String getOrderStatus(String orderId) {
          String orderResult = "";

          try {
               long timeStamp = System.currentTimeMillis();
               orderResult = umFuturesClientImpl.account().queryOrder(
                       FuturesOrderParams.getQueryOrderParams(futuresBot.getSymbol(),
                               orderId, timeStamp)
               );
          } catch (BinanceConnectorException e) {
               System.out.println("BinanceConnectorException");
               e.printStackTrace();
          } catch (BinanceClientException e) {
               System.out.println("BinanceClientException");
               e.printStackTrace();
          } catch (BinanceServerException e) {
               System.out.println("BinanceServerException");
               e.printStackTrace();
          }
          
          JSONObject jsonResult = new JSONObject(orderResult);
          String status = jsonResult.optString("status");
          return status; 
     }

     private Double getTickerPrice() {
          String result = "";

          try {               
               result = umFuturesClientImpl.market().tickerSymbol(
                       FuturesOrderParams.getTickerParams(futuresBot.getSymbol())
               );
          } catch (BinanceConnectorException e) {
               System.out.println("BinanceConnectorException");
               e.printStackTrace();
          } catch (BinanceClientException e) {
               System.out.println("BinanceClientException");
               e.printStackTrace();
          } catch (BinanceServerException e) {
               System.out.println("BinanceServerException");
               e.printStackTrace();
          }
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
          } catch (Exception e) {
               e.printStackTrace();
          }
          System.out.println("init currentCross: " + currentCross);
     }

     private PositionSide initExistingPosition() {
          String jsonResult = "";

          try {
               long timeStamp = System.currentTimeMillis();
               jsonResult = umFuturesClientImpl.account().positionInformation(
                       FuturesOrderParams.getParams(futuresBot.getSymbol(), timeStamp)
               );

          } catch (BinanceConnectorException e) {
               System.out.println("BinanceConnectorException");
               e.printStackTrace();
          } catch (BinanceClientException e) {
               System.out.println("BinanceClientException");
               e.printStackTrace();
          } catch (BinanceServerException e) {
               System.out.println("BinanceServerException");
               e.printStackTrace();
          }

          JSONArray positions = new JSONArray(jsonResult);
          JSONObject positionObject = positions.getJSONObject(0);

          String price = positionObject.optString("entryPrice");

          if (!price.equals("0.0") && !price.equals("0.00000000")) {
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
          String jsonResult = "";

          try {               
               long timeStamp = System.currentTimeMillis();
               jsonResult = umFuturesClientImpl.account().accountTradeList(
                       FuturesOrderParams.getParams(futuresBot.getSymbol(), timeStamp));

          } catch (BinanceConnectorException e) {
               System.out.println("BinanceConnectorException");
               e.printStackTrace();
          } catch (BinanceClientException e) {
               System.out.println("BinanceClientException");
               e.printStackTrace();
          } catch (BinanceServerException e) {
               System.out.println("BinanceServerException");
               e.printStackTrace();
          }

          JSONArray positions = new JSONArray(jsonResult);
          if (positions.length() > 0) {
               JSONObject tradeObject = positions.getJSONObject(positions.length() - 1);
               return tradeObject.optString("side");
          } else {
               return "none";
          }
          
     }

     private String initStopLossOrder() {
          String orderId = "";
          String jsonResult = "";
          
          try {
               long timeStamp = System.currentTimeMillis();
               jsonResult = umFuturesClientImpl.account().currentAllOpenOrders(
                       FuturesOrderParams.getParams(futuresBot.getSymbol(), timeStamp));

          } catch (BinanceConnectorException e) {
               System.out.println("BinanceConnectorException");
               e.printStackTrace();
          } catch (BinanceClientException e) {
               System.out.println("BinanceClientException");
               e.printStackTrace();
          } catch (BinanceServerException e) {
               System.out.println("BinanceServerException");
               e.printStackTrace();
          }

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
          String jsonResult = "";
          
          try {
               long timeStamp = System.currentTimeMillis();
               jsonResult = umFuturesClientImpl.account().positionInformation(
                       FuturesOrderParams.getParams(futuresBot.getSymbol(), timeStamp)
               );
          } catch (BinanceConnectorException e) {
               System.out.println("BinanceConnectorException");
               e.printStackTrace();
          } catch (BinanceClientException e) {
               System.out.println("BinanceClientException");
               e.printStackTrace();
          } catch (BinanceServerException e) {
               System.out.println("BinanceServerException");
               e.printStackTrace();
          }
          
          JSONObject positionObject = new JSONArray(jsonResult).getJSONObject(0);
          return positionObject.optDouble("entryPrice");
     }

	private String getTime() {
		LocalDateTime currentDateTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		return currentDateTime.format(formatter);
	}

     private double getRealizedPNL(String orderId) {
          String jsonResult = "";
          
          try {               
               long timeStamp = System.currentTimeMillis();
               jsonResult = umFuturesClientImpl.account().accountTradeList(
                       FuturesOrderParams.getQueryOrderParams(futuresBot.getSymbol(), orderId, timeStamp)
               );
          } catch (BinanceConnectorException e) {
               System.out.println("BinanceConnectorException");
               e.printStackTrace();
          } catch (BinanceClientException e) {
               System.out.println("BinanceClientException");
               e.printStackTrace();
          } catch (BinanceServerException e) {
               System.out.println("BinanceServerException");
               e.printStackTrace();
          }

          JSONArray tradeList = new JSONArray(jsonResult);
          JSONObject tradeObject = tradeList.getJSONObject(0);

          return tradeObject.optDouble("realizedPnl");
     }
     
     private double calculateBorderPrice(PositionSide positionSide) {
          Double price = entryPrice;
          Double percent = (0.5 / 100) * price;
          Double result = 0.0;
          
          if (positionSide == PositionSide.SHORT) {
               result = price - percent;
          } else if (positionSide == PositionSide.LONG) {
               result = price + percent;
          }
          return result;
     }
     
     private void clearFields() {
          entryPrice = null;
          borderCrossed = false;
          borderPrice = null;
     }
}

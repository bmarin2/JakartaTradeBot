package com.tradebot.service;

import com.binance.connector.futures.client.exceptions.BinanceClientException;
import com.binance.connector.futures.client.exceptions.BinanceConnectorException;
import com.binance.connector.futures.client.exceptions.BinanceServerException;
import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.tradebot.binance.UMFuturesClientConfig;
import com.tradebot.configuration.FuturesOrderParams;
import com.tradebot.db.MACDAlarmDB;
import com.tradebot.enums.ChartMode;
import com.tradebot.enums.PositionSide;
import com.tradebot.model.FuturesBot;
import com.tradebot.model.MACDAlarm;
import com.tradebot.model.OrderSide;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;

public class FuturesTaskMACDCross implements Runnable {

     private FuturesBot futuresBot;
     private UMFuturesClientImpl umFuturesClientImpl;
     private final TelegramBot telegramBot;

     private boolean currentMACDCross;

     private String currentSLOrder = "";
     private PositionSide currentPositionSide;
     private Double entryPrice;
     private Double borderPrice;
     private boolean borderCrossed;
	private MACDAlarm macdAlarm;

     public FuturesTaskMACDCross(FuturesBot futuresBot) {
          this.futuresBot = futuresBot;
		umFuturesClientImpl = initChartMode(futuresBot.getChartMode());
          currentMACDCross = initMACDCross();
          currentSLOrder = initStopLossOrder();
          currentPositionSide = initExistingPosition();
          System.out.println("init side: " + currentPositionSide);
          if (currentPositionSide != PositionSide.NONE) {
               entryPrice = getEntryPrice();
               System.out.println("init entry price; " + entryPrice);
               if (currentPositionSide == PositionSide.SHORT) {
                    borderPrice = calculateBorderPrice(PositionSide.SHORT);
                    System.out.println("border price; " + borderPrice);
               } else if (currentPositionSide == PositionSide.LONG) {
                    borderPrice = calculateBorderPrice(PositionSide.LONG);
                    System.out.println("border price; " + borderPrice);
               }               
          }

          telegramBot = new TelegramBot();
     }

     @Override
     public void run() {
          if (!currentSLOrder.isEmpty()) {
               String status = getOrderStatus(currentSLOrder);
               if (status.equals("FILLED") || status.equals("CANCELED")) {
                    currentPositionSide = PositionSide.NONE;
                    currentSLOrder = "";
                    clearFields();
                    System.out.println("SP Triggered");

                    try {
                         telegramBot.sendMessage("Stop Loss triggered " + futuresBot.getSymbol());
                    } catch (Exception e) {
                         e.printStackTrace();
                    }
               }
          }

		// --- Check TP ---

          if (currentPositionSide == PositionSide.LONG) {
               if (isDistantFromBorderPrice() && !borderCrossed) {
                    System.out.println("Border crossed");
                    borderCrossed = true;
               } else if (!isDistantFromBorderPrice() && borderCrossed) {
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
               if (isDistantFromBorderPrice() && !borderCrossed) {
                    System.out.println("Border crossed");
                    borderCrossed = true;
               } else if (!isDistantFromBorderPrice() && borderCrossed) {
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

		System.out.println("Time: " + getTime());
		System.out.println("SP order: " + currentSLOrder);
		System.out.println("DB Cross: " + checkMacdCross());
		System.out.println("MACD cross: " + currentMACDCross);
		System.out.println("pos: " + currentPositionSide.toString());
		System.out.println("----");
		System.out.println("entry price: " + entryPrice);
		System.out.println("border crossed: " + borderCrossed);
		System.out.println("border price: " + borderPrice);
		System.out.println("--");
		
		if (currentPositionSide != PositionSide.NONE && !isDistantFromBorderPrice()) {
			currentMACDCross = checkMacdCross();
			System.out.println("\nNOT DISTANT FROM BORDER PRICE!");
			return;
		}		

          System.out.println("\nContinuing...");

          // ====================================================================================

          if (currentMACDCross != checkMacdCross()) {

			// update macdAlarm object
			try {
				macdAlarm = MACDAlarmDB.getOneAlarm(futuresBot.getDemaAlertTaskId());
			} catch (Exception e) {
				e.printStackTrace();
			}

               currentMACDCross = macdAlarm.getMacdCrosss();
               System.out.println("cross is now: " + currentMACDCross);

               if (currentMACDCross) { // if crossed DOWN
                    if (currentPositionSide == PositionSide.SHORT) {
                         return;
                    }

                    if (currentPositionSide == PositionSide.LONG) {
                         
                         String orderId = createShortOrder(); // take profit
					
					try {
						double profit = getRealizedPNL(orderId);
						telegramBot.sendMessage("Realized profit from LONG, OrederId: " + orderId + " " + getTime() + "\n "
							   + "Profit: " + profit + "USDT ");
					} catch (Exception e) {
						e.printStackTrace();
					}
                         
                         clearFields();
                         cancelCurrentSPOrder();
					currentPositionSide = PositionSide.NONE;
                    }

				if (currentPositionSide == PositionSide.NONE && macdAlarm.getGoodForEntry()) {

					try {
						telegramBot.sendMessage("Entering SHORT trade " + getTime());
					} catch (IOException e) {
						e.printStackTrace();
					}

					createShortOrder();
					entryPrice = getEntryPrice();
					borderPrice = calculateBorderPrice(PositionSide.SHORT);
					createStopLossOrder(OrderSide.BUY, calculateSL(PositionSide.SHORT));
					currentPositionSide = PositionSide.SHORT;
				}
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
					currentPositionSide = PositionSide.NONE;
                    }

				if (currentPositionSide == PositionSide.NONE && macdAlarm.getGoodForEntry()) {

					try {
						telegramBot.sendMessage("Entering LONG trade " + getTime());
					} catch (IOException e) {
						e.printStackTrace();
					}

					createLongOrder();
					entryPrice = getEntryPrice();
					borderPrice = calculateBorderPrice(PositionSide.LONG);
					createStopLossOrder(OrderSide.SELL, calculateSL(PositionSide.LONG));
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
          String orderResult = "";
          String orderId = "";

          try {
               long timeStamp = System.currentTimeMillis();
               orderResult = umFuturesClientImpl.account().newOrder(
                       FuturesOrderParams.getOrderParams(futuresBot.getSymbol(), orderSide,
                               OrderSide.BOTH, futuresBot.getQuantity(), timeStamp)
               );
          } catch (BinanceConnectorException e) {
               sendErrorMsg("BinanceConnectorException", e.getMessage());
               e.printStackTrace();
          } catch (BinanceClientException e) {
			sendErrorMsg("BinanceClientException", e.getMessage());
               e.printStackTrace();
          } catch (BinanceServerException e) {
			sendErrorMsg("BinanceServerException", e.getMessage());
               e.printStackTrace();
          }

          JSONObject jsonResult = new JSONObject(orderResult);
          orderId = jsonResult.optString("orderId");

          System.out.println("Order created " + orderSide.toString() + " ID: " + orderId);

          return orderId;
     }

     private void createStopLossOrder(OrderSide orderSide, String stopPrice) {
          String orderResult = "";

          try {               
               long timeStamp = System.currentTimeMillis();
               orderResult = umFuturesClientImpl.account().newOrder(
                       FuturesOrderParams.getStopLossParams(futuresBot.getSymbol(),
                               orderSide, OrderSide.BOTH, futuresBot.getQuantity(),
                               stopPrice, timeStamp)
               );
          } catch (BinanceConnectorException e) {
               sendErrorMsg("BinanceConnectorException", e.getMessage());
               e.printStackTrace();
          } catch (BinanceClientException e) {
			sendErrorMsg("BinanceClientException", e.getMessage());
               e.printStackTrace();
          } catch (BinanceServerException e) {
			sendErrorMsg("BinanceServerException", e.getMessage());
               e.printStackTrace();
          }
          
          JSONObject jsonResult = new JSONObject(orderResult);
          currentSLOrder = jsonResult.optString("orderId");
          System.out.println("SP Order ID: " + currentSLOrder);          
     }

     private void cancelOrder(String orderId) {
          try {
               long timeStamp = System.currentTimeMillis();
               umFuturesClientImpl.account().cancelOrder(
                       FuturesOrderParams.getCancelOrderParams(futuresBot.getSymbol(),
                               orderId, timeStamp)
               );
          } catch (BinanceConnectorException e) {
               sendErrorMsg("BinanceConnectorException", e.getMessage());
               e.printStackTrace();
          } catch (BinanceClientException e) {
			sendErrorMsg("BinanceClientException", e.getMessage());
               e.printStackTrace();
          } catch (BinanceServerException e) {
			sendErrorMsg("BinanceServerException", e.getMessage());
               e.printStackTrace();
          }
     }
     
     private void cancelCurrentSPOrder() {
          if (!currentSLOrder.isEmpty()) {
               if (getOrderStatus(currentSLOrder).equals("NEW")) {
                    cancelOrder(currentSLOrder);
                    currentSLOrder = "";
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
               sendErrorMsg("BinanceConnectorException", e.getMessage());
               e.printStackTrace();
          } catch (BinanceClientException e) {
			sendErrorMsg("BinanceClientException", e.getMessage());
               e.printStackTrace();
          } catch (BinanceServerException e) {
			sendErrorMsg("BinanceServerException", e.getMessage());
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
               sendErrorMsg("BinanceConnectorException", e.getMessage());
               e.printStackTrace();
          } catch (BinanceClientException e) {
			sendErrorMsg("BinanceClientException", e.getMessage());
               e.printStackTrace();
          } catch (BinanceServerException e) {
			sendErrorMsg("BinanceServerException", e.getMessage());
               e.printStackTrace();
          }
          JSONObject jsonResult = new JSONObject(result);
          return jsonResult.optDouble("price");
     }

     private String calculateSL(PositionSide positionSide) {
          Double price = entryPrice;
          Double result = 0.0;
          if (positionSide == PositionSide.SHORT) {
               result = price + macdAlarm.getLastAtr() * futuresBot.getStopLoss();
          } else if (positionSide == PositionSide.LONG){
               result = price - macdAlarm.getLastAtr() * futuresBot.getStopLoss();
          }
          String resultFormated = String.format("%.2f", result);
          return resultFormated;
     }

     private boolean initMACDCross() {
          try {
               return MACDAlarmDB.getMacdCross(futuresBot.getDemaAlertTaskId());
          } catch (Exception e) {
               e.printStackTrace();
			return false;
          }
     }

     private PositionSide initExistingPosition() {
          String jsonResult = "";

          try {
               long timeStamp = System.currentTimeMillis();
               jsonResult = umFuturesClientImpl.account().positionInformation(
                       FuturesOrderParams.getParams(futuresBot.getSymbol(), timeStamp)
               );
          } catch (BinanceConnectorException e) {
               sendErrorMsg("BinanceConnectorException", e.getMessage());
               e.printStackTrace();
          } catch (BinanceClientException e) {
			sendErrorMsg("BinanceClientException", e.getMessage());
               e.printStackTrace();
          } catch (BinanceServerException e) {
			sendErrorMsg("BinanceServerException", e.getMessage());
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
                       FuturesOrderParams.getParams(futuresBot.getSymbol(), timeStamp)
			);
          } catch (BinanceConnectorException e) {
               sendErrorMsg("BinanceConnectorException", e.getMessage());
               e.printStackTrace();
          } catch (BinanceClientException e) {
			sendErrorMsg("BinanceClientException", e.getMessage());
               e.printStackTrace();
          } catch (BinanceServerException e) {
			sendErrorMsg("BinanceServerException", e.getMessage());
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
                       FuturesOrderParams.getParams(futuresBot.getSymbol(), timeStamp)
			);
          } catch (BinanceConnectorException e) {
               sendErrorMsg("BinanceConnectorException", e.getMessage());
               e.printStackTrace();
          } catch (BinanceClientException e) {
			sendErrorMsg("BinanceClientException", e.getMessage());
               e.printStackTrace();
          } catch (BinanceServerException e) {
			sendErrorMsg("BinanceServerException", e.getMessage());
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
               sendErrorMsg("BinanceConnectorException", e.getMessage());
               e.printStackTrace();
          } catch (BinanceClientException e) {
			sendErrorMsg("BinanceClientException", e.getMessage());
               e.printStackTrace();
          } catch (BinanceServerException e) {
			sendErrorMsg("BinanceServerException", e.getMessage());
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
               sendErrorMsg("BinanceConnectorException", e.getMessage());
               e.printStackTrace();
          } catch (BinanceClientException e) {
			sendErrorMsg("BinanceClientException", e.getMessage());
               e.printStackTrace();
          } catch (BinanceServerException e) {
			sendErrorMsg("BinanceServerException", e.getMessage());
               e.printStackTrace();
          }

          JSONArray tradeList = new JSONArray(jsonResult);
          JSONObject tradeObject = tradeList.getJSONObject(0);

          return tradeObject.optDouble("realizedPnl");
     }

     private double calculateBorderPrice(PositionSide positionSide) {
          Double price = entryPrice;
          Double result = 0.0;
          
          if (positionSide == PositionSide.SHORT) {
               result = price - macdAlarm.getLastAtr() * futuresBot.getStopLoss() * futuresBot.getTakeProfit();
          } else if (positionSide == PositionSide.LONG) {
               result = price + macdAlarm.getLastAtr() * futuresBot.getStopLoss() * futuresBot.getTakeProfit();
          }
          return result;
     }

     private void clearFields() {
          entryPrice = null;
          borderCrossed = false;
          borderPrice = null;
     }

	private boolean isDistantFromBorderPrice() {
		double newPrice = getTickerPrice();
		switch (currentPositionSide) {
			case LONG:
				return newPrice > borderPrice;
			case SHORT:
				return newPrice < borderPrice;
			case NONE:
				return true;
			default:
				return true;
		}
	}
	
	private UMFuturesClientImpl initChartMode(ChartMode chartMode) {
		switch (chartMode) {
			case FUTURES_BASE_URL_PROD:
				return UMFuturesClientConfig.futuresBaseURLProd();
			case FUTURES_BASE_URL_TEST:
				return UMFuturesClientConfig.futuresBaseURLTest();
			case FUTURES_SIGNED_PROD:
				return UMFuturesClientConfig.futuresSignedProd();
			case FUTURES_SIGNED_TEST:
				return UMFuturesClientConfig.futuresSignedTest();
			default:
				return null;
		}
	}
	
	private void sendErrorMsg(String type, String msg) {
		try {
			telegramBot.sendMessage("Futures exception " + futuresBot.getSymbol() + "\n"
				   + type + "\n"  + msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private boolean checkMacdCross() {
		try {
			return MACDAlarmDB.getMacdCross(futuresBot.getDemaAlertTaskId());
		} catch (Exception e) {
			e.printStackTrace();
          }
		return false;
	}
}

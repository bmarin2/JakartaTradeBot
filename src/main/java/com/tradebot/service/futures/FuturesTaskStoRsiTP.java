package com.tradebot.service.futures;

import com.binance.connector.futures.client.exceptions.BinanceClientException;
import com.binance.connector.futures.client.exceptions.BinanceConnectorException;
import com.binance.connector.futures.client.exceptions.BinanceServerException;
import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.tradebot.binance.UMFuturesClientConfig;
import com.tradebot.configuration.FuturesOrderParams;
import com.tradebot.db.AlarmDB;
import com.tradebot.enums.ChartMode;
import com.tradebot.enums.PositionSide;
import com.tradebot.model.Alarm;
import com.tradebot.model.FuturesBot;
import com.tradebot.model.OrderSide;
import com.tradebot.service.TelegramBot;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONObject;

public class FuturesTaskStoRsiTP implements Runnable {

     private FuturesBot futuresBot;
     private UMFuturesClientImpl umFuturesClientImpl;
     private final TelegramBot telegramBot;

     private boolean currentCross;

     private String currentSLOrder = "";
	private String currentTPOrder = "";
     private PositionSide currentPositionSide;
     private Double entryPrice;
	private Alarm alarm;
	private int counter;

     public FuturesTaskStoRsiTP(FuturesBot futuresBot) {
          this.futuresBot = futuresBot;
		umFuturesClientImpl = initChartMode(futuresBot.getChartMode());
          currentCross = checkCross();
          currentSLOrder = initStopLossOrder();
		currentTPOrder = initTakeProfitOrder();
          currentPositionSide = initExistingPosition();
          System.out.println("init side: " + currentPositionSide);
          if (currentPositionSide != PositionSide.NONE) {
               entryPrice = getEntryPrice();
               System.out.println("init entry price; " + entryPrice);
               try {
                    alarm = AlarmDB.getOneAlarm(futuresBot.getDemaAlertTaskId());
               } catch (Exception ex) {
                    ex.printStackTrace();
               }               
          }

          telegramBot = new TelegramBot();
     }

     @Override
     public void run() {
		System.out.println("-- from run " + new Date());
		
		if (currentPositionSide != PositionSide.NONE) {

			if (counter == 1) {

				if (!currentSLOrder.isEmpty()) {
					String status = getOrderStatus(currentSLOrder);
					if (status.equals("FILLED") || status.equals("CANCELED")) {
						currentPositionSide = PositionSide.NONE;
						double pnl = getRealizedPNL(currentSLOrder);
						currentSLOrder = "";
						cancelOrder(currentTPOrder);
						currentTPOrder = "";
						entryPrice = null;
						System.out.println("SP Triggered");

						try {

							telegramBot.sendMessage("Stop Loss triggered " + futuresBot.getSymbol() + "\n"
								   + "PNL: " + pnl);
						} catch (Exception e) {
							e.printStackTrace();
						}
						return;
					}
				}

				if (!currentTPOrder.isEmpty()) {
					String status = getOrderStatus(currentTPOrder);
					if (status.equals("FILLED")) {
						currentPositionSide = PositionSide.NONE;
						double pnl = getRealizedPNL(currentTPOrder);
						currentTPOrder = "";
						cancelOrder(currentSLOrder);
						currentSLOrder = "";
						entryPrice = null;
						System.out.println("TP Triggered");

						try {

							telegramBot.sendMessage("Take Profit triggered " + futuresBot.getSymbol() + "\n"
								   + "PNL: " + pnl);
						} catch (Exception e) {
							e.printStackTrace();
						}
						return;
					}
				}

				counter = 0;

			} else {
				counter++;
			}
		}

		if (currentPositionSide != PositionSide.NONE) {
			System.out.println("Time: " + getTime());
			System.out.println("SL order: " + currentSLOrder);
			System.out.println("TP order: " + currentTPOrder);
			System.out.println("DB Cross: " + checkCross());
			System.out.println("Cross: " + currentCross);
			System.out.println("pos: " + currentPositionSide.toString());
			System.out.println("----");
			System.out.println("entry price: " + entryPrice);
			System.out.println("--");
		}		

          // ====================================================================================

          if (currentCross != checkCross()) {

			// update
			try {
				alarm = AlarmDB.getOneAlarm(futuresBot.getDemaAlertTaskId());
			} catch (Exception e) {
				e.printStackTrace();
			}

               currentCross = alarm.getCrosss();
               System.out.println("cross is now: " + currentCross);

               if (currentCross) { // if crossed DOWN
                    if (currentPositionSide == PositionSide.SHORT) {
                         return;
                    }

				if (currentPositionSide == PositionSide.NONE && alarm.getGoodForEntry()) {

					try {
						telegramBot.sendMessage("Entering SHORT trade " + getTime());
					} catch (IOException e) {
						e.printStackTrace();
					}

					createShortOrder();
					entryPrice = getEntryPrice();
					createStopLossOrder(OrderSide.BUY, calculateSL(PositionSide.SHORT));
					createTakeProfit(OrderSide.BUY, calculateTP(PositionSide.SHORT));
					currentPositionSide = PositionSide.SHORT;
				}
               } else { // if crossed UP
                    if (currentPositionSide == PositionSide.LONG) {
                         return;
                    }

				if (currentPositionSide == PositionSide.NONE && alarm.getGoodForEntry()) {

					try {
						telegramBot.sendMessage("Entering LONG trade " + getTime());
					} catch (IOException e) {
						e.printStackTrace();
					}

					createLongOrder();
					entryPrice = getEntryPrice();
					createStopLossOrder(OrderSide.SELL, calculateSL(PositionSide.LONG));
					createTakeProfit(OrderSide.SELL, calculateTP(PositionSide.LONG));
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
               sendErrorMsg("BinanceConnectorException createOrder", e.getMessage());
               e.printStackTrace();
			return "";
          } catch (BinanceClientException e) {
			sendErrorMsg("BinanceClientException createOrder", e.getMessage());
               e.printStackTrace();
			return "";
          } catch (BinanceServerException e) {
			sendErrorMsg("BinanceServerException createOrder", e.getMessage());
               e.printStackTrace();
			return "";
          }

          JSONObject jsonResult = new JSONObject(orderResult);
          orderId = jsonResult.optString("orderId");

          System.out.println("Order created " + orderSide.toString() + " ID: " + orderId);

          return orderId;
     }

     private void createStopLossOrder(OrderSide orderSide, double stopPrice) {
          String orderResult = "";
		System.out.println("createding SL Order");
          try {
			System.out.println("start of try");
               long timeStamp = System.currentTimeMillis();
               orderResult = umFuturesClientImpl.account().newOrder(
                       FuturesOrderParams.getStopLossParams(futuresBot.getSymbol(),
                               orderSide, OrderSide.BOTH, futuresBot.getQuantity(),
                               stopPrice, timeStamp)
               );
			System.out.println("end of try");
          } catch (BinanceConnectorException e) {
               sendErrorMsg("BinanceConnectorException createStopLossOrder", e.getMessage());
               e.printStackTrace();
			return;
          } catch (BinanceClientException e) {
			sendErrorMsg("BinanceClientException createStopLossOrder", e.getMessage());
               e.printStackTrace();
			return;
          } catch (BinanceServerException e) {
			sendErrorMsg("BinanceServerException createStopLossOrder", e.getMessage());
               e.printStackTrace();
			return;
          }
          
          JSONObject jsonResult = new JSONObject(orderResult);
          currentSLOrder = jsonResult.optString("orderId");
          System.out.println("SP Order ID: " + currentSLOrder);
     }

	private void createTakeProfit(OrderSide orderSide, double stopPrice) {
          System.out.println("stopPrice TP: " + stopPrice);
          long timeStamp = System.currentTimeMillis();
		
		 String orderResult = "";
		
		try {
			orderResult = umFuturesClientImpl.account().newOrder(
				   FuturesOrderParams.getTakeProfitParams(futuresBot.getSymbol(),
						 orderSide, OrderSide.BOTH, futuresBot.getQuantity(),
						 stopPrice, timeStamp)
			);			
		} catch (BinanceConnectorException e) {
               sendErrorMsg("BinanceConnectorException create take profit", e.getMessage());
               e.printStackTrace();
			return;
          } catch (BinanceClientException e) {
			sendErrorMsg("BinanceClientException create take profit", e.getMessage());
               e.printStackTrace();
			return;
          } catch (BinanceServerException e) {
			sendErrorMsg("BinanceServerException create take profit", e.getMessage());
               e.printStackTrace();
			return;
          }
          System.out.println("orderResult TP: " + orderResult);
          JSONObject jsonResult = new JSONObject(orderResult);
          currentTPOrder = jsonResult.optString("orderId");
          System.out.println("Take Profit Order ID: " + currentTPOrder);
     }

     private void cancelOrder(String orderId) {
          try {
               long timeStamp = System.currentTimeMillis();
               umFuturesClientImpl.account().cancelOrder(
                       FuturesOrderParams.getCancelOrderParams(futuresBot.getSymbol(),
                               orderId, timeStamp)
               );
          } catch (BinanceConnectorException e) {
               sendErrorMsg("BinanceConnectorException cancel order", e.getMessage());
               e.printStackTrace();
			return;
          } catch (BinanceClientException e) {
			sendErrorMsg("BinanceClientException cancel order", e.getMessage());
               e.printStackTrace();
			return;
          } catch (BinanceServerException e) {
			sendErrorMsg("BinanceServerException cancel order", e.getMessage());
               e.printStackTrace();
			return;
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
               sendErrorMsg("BinanceConnectorException getOrderStatus", e.getMessage());
               e.printStackTrace();
			return "";
          } catch (BinanceClientException e) {
			System.out.println("getErrMsg: " + e.getErrMsg());
			sendErrorMsg("BinanceClientException getOrderStatus", e.getMessage());
               e.printStackTrace();
			return "";
          } catch (BinanceServerException e) {
			sendErrorMsg("BinanceServerException getOrderStatus", e.getMessage());
               e.printStackTrace();
			return "";
          }
          
          JSONObject jsonResult = new JSONObject(orderResult);
          String status = jsonResult.optString("status");
          return status; 
     }

//     private Double getTickerPrice() {
//          String result = "";
//
//          try {               
//               result = umFuturesClientImpl.market().tickerSymbol(
//                       FuturesOrderParams.getTickerParams(futuresBot.getSymbol())
//               );
//          } catch (BinanceConnectorException e) {
//               sendErrorMsg("BinanceConnectorException", e.getMessage());
//               e.printStackTrace();
//          } catch (BinanceClientException e) {
//			sendErrorMsg("BinanceClientException", e.getMessage());
//               e.printStackTrace();
//          } catch (BinanceServerException e) {
//			sendErrorMsg("BinanceServerException", e.getMessage());
//               e.printStackTrace();
//          }
//          JSONObject jsonResult = new JSONObject(result);
//          return jsonResult.optDouble("price");
//     }

     private double calculateSL(PositionSide positionSide) {
          Double price = entryPrice;
          Double result = 0.0;
          if (positionSide == PositionSide.SHORT) {
               result = price + alarm.getAtr() * futuresBot.getStopLoss();
          } else if (positionSide == PositionSide.LONG){
               result = price - alarm.getAtr() * futuresBot.getStopLoss();
          }
          String resultFormated = String.format("%.2f", result).replace(',', '.');
          return Double.parseDouble(resultFormated);
     }

	private double calculateTP(PositionSide positionSide) {
          Double price = entryPrice;
          Double result = 0.0;
          if (positionSide == PositionSide.SHORT) {
               result = price - alarm.getAtr() * futuresBot.getTakeProfit();
          } else if (positionSide == PositionSide.LONG){
               result = price + alarm.getAtr() * futuresBot.getTakeProfit();
          }
          String resultFormated = String.format("%.2f", result).replace(',', '.');
          return Double.parseDouble(resultFormated);
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
	
	private String initTakeProfitOrder() {
          String orderId = "";

          long timeStamp = System.currentTimeMillis();
          String jsonResult = umFuturesClientImpl.account().currentAllOpenOrders(
                  FuturesOrderParams.getParams(futuresBot.getSymbol(), timeStamp));

          JSONArray orders = new JSONArray(jsonResult);

          for (int i = 0; i < orders.length(); i++) {
               JSONObject orderObject = orders.getJSONObject(i);

               if ("TAKE_PROFIT_MARKET".equals(orderObject.optString("type"))) {
                    orderId = Long.toString(orderObject.optLong("orderId"));
                    break;
               }
          }
          System.out.println("init TP order: " + orderId);
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
	
	private boolean checkCross() {
		try {
			return AlarmDB.getAlarmCross(futuresBot.getDemaAlertTaskId());
		} catch (Exception e) {
			e.printStackTrace();
          }
		return false;
	}
}

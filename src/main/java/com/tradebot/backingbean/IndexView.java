package com.tradebot.backingbean;

import com.binance.connector.client.impl.SpotClientImpl;
import com.tradebot.binance.SpotClientConfig;
import com.tradebot.configuration.OrdersParams;
import com.tradebot.db.ErrorTrackerDB;
import com.tradebot.db.OrderDB;
import com.tradebot.db.TradeBotDB;
import com.tradebot.model.BotDTO;
import com.tradebot.model.ErrorTracker;
import com.tradebot.model.OrderSide;
import com.tradebot.model.OrderTracker;
import com.tradebot.model.TradeBot;
import com.tradebot.service.BotExtraInfo;
import com.tradebot.service.TaskService;
import com.tradebot.service.SpotTask;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import org.json.JSONArray;
import org.json.JSONObject;
import org.primefaces.PrimeFaces;
import org.primefaces.context.PrimeRequestContext;

@Named
@ViewScoped
@Data
public class IndexView implements Serializable {

	@Inject
	private TaskService taskService;

	private List<TradeBot> bots;
	
	private List<OrderTracker> botOrders;
	
	private TradeBot selectedTradeBot;
	
	private OrderTracker selectedOrder;

	private TradeBot secondTradeBot;

	private boolean shouldEditBot;
	
	private SpotClientImpl spotClientImpl;
	
	private List<String> orderJsonString;
	
	private JSONArray balances;
	
	private List<ErrorTracker> errors;
        
     private long currentErrorBotId;
	
	private BigDecimal checkedPrice;
	
	@PostConstruct
	private void init() {
		selectedTradeBot = new TradeBot();
		try {
			bots = TradeBotDB.getAllTradeBots();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		spotClientImpl = SpotClientConfig.spotClientSignTest();

		getAccountInfoAll();

//          if (!isBotRunning("reportTask")) {
//               ReportTask reportTask = new ReportTask();
//               taskService.addTask("reportTask", reportTask,
//                       1,
//                       24,
//                       TimeUnit.HOURS
//               );
//          }
	}
	
	public void getOrderDetails(String symbol, long orderId) {
		long timeStamp = System.currentTimeMillis();
		String temp = spotClientImpl.createTrade().getOrder(OrdersParams.getOrder(symbol, orderId, timeStamp));		
		JSONObject json = new JSONObject(temp);		
		List<String> list = new ArrayList<>();
		String[] lines = json.toString(2).split("\\r?\\n");
		list.addAll(Arrays.asList(lines));
		orderJsonString = list;
	}
	
	public int queryTotalOrdersCount(long botId) throws Exception {
		return OrderDB.getOrderCount(botId, true, true);
	}
	
	public int querySoldOrdersCount(long botId) throws Exception {
		return OrderDB.getOrderCount(botId, false, true);
	}
	
	public int queryUnsoldOrdersCount(long botId) throws Exception {
		return OrderDB.getOrderCount(botId, false, false);
	}
	
	public String getBotProfit(long botId) throws Exception {
		BigDecimal temp = OrderDB.getTadeBotProfits(botId);
		String val = "";
		if(temp != null){
			val = temp.setScale(2, RoundingMode.HALF_DOWN).toString();
			if (temp.compareTo(BigDecimal.ZERO) < 0) {
				return val;
			} else {
				return "+"+val;
			}			
		}
		return "0";
	}
	
	public void getAccountInfoAll() {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("timestamp", System.currentTimeMillis());

		try {
			String temp = spotClientImpl.createTrade().account(parameters);
			JSONObject jsonObj = new JSONObject(temp);
			JSONArray balancesArray = jsonObj.getJSONArray("balances");
			balances = balancesArray;
		} catch (Exception e) {
			System.out.println("error in spot test");
		}

	}
	
	public String getBalance(String symbol) {
		String formattedNumber = null;
		for (int i = 0; i < balances.length(); i++) {
			JSONObject balance = balances.getJSONObject(i);
			if (balance.getString("asset").equals(symbol.replace("USDT", ""))) {
				String freeValue = balance.getString("free");
				double number = Double.parseDouble(freeValue);
				formattedNumber = String.format("%.2f", number);
				break;
			}
		}
		return formattedNumber;
	}
	
	public String getBalanceUSDT() {
		String formattedNumber = null;
		if (balances == null) {
			return null;
		}
		for (int i = 0; i < balances.length(); i++) {
			JSONObject balance = balances.getJSONObject(i);
			if (balance.getString("asset").equals("USDT")) {
				String freeValue = balance.getString("free");
				double number = Double.parseDouble(freeValue);
				formattedNumber = String.format("%.2f", number);
				break;
			}
		}
		return formattedNumber;
	}

	public void updateBot() {
		try {
			if (shouldEditBot) {
				TradeBotDB.updateTradeBot(selectedTradeBot);
				addMessage("Bot Updated", "");
			} else {
				long bot_id = TradeBotDB.addBot(selectedTradeBot);
				selectedTradeBot.setId(bot_id);
				addMessage("New bot added", "");
				// addTask();
			}
			bots = TradeBotDB.getAllTradeBots();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		PrimeFaces.current().executeScript("PF('manageBot').hide()");
	}
	
	public void getBotErrors(long botId) throws Exception {
                currentErrorBotId = botId;
		errors = ErrorTrackerDB.getTradeBotErrors(botId, true);
	}
	
	public int getErrorsSize(long botId) throws Exception {
		List<ErrorTracker> errorsTmp = ErrorTrackerDB.getTradeBotErrors(botId, true);
		return errorsTmp.size();
	}
	
	public void acknowledgeError(ErrorTracker err) throws Exception {
		ErrorTrackerDB.updateError(err);
		getBotErrors(err.getTradebot_id());
	}
        
     public void acknowledgeAllBotErrors() throws Exception {
		ErrorTrackerDB.updateAllBotErrors(currentErrorBotId);
		getBotErrors(currentErrorBotId);
	}
     
     public void acknowledgeAllErrors() throws Exception {
		ErrorTrackerDB.updateAllErrors();
		getBotErrors(currentErrorBotId);
	}
	
	public void getBotOrders(long botId) throws Exception {
		botOrders = OrderDB.getOrdersFromBot(true, botId);
	}

	public int getRunningTasksNumber() {
		return taskService.getScheduledTasks().size();
	}

	public void newBot() {
		shouldEditBot = false;
		selectedTradeBot = new TradeBot();
	}

	public void editBot() {
		shouldEditBot = true;
	}

	public boolean isBotRunning(String taskId) {
		return taskService.getScheduledTasks().containsKey(taskId);
	}

	public TimeUnit[] getUnits() {
		return TimeUnit.values();
	}

	public void removeTask() {
		taskService.removeTask(selectedTradeBot.getTaskId());
		addMessage("Task removed", "Bot " + selectedTradeBot.getTaskId());
	}

	public void addTask() throws Exception {
		SpotTask spotTask = new SpotTask(selectedTradeBot);

		taskService.addTask(selectedTradeBot.getTaskId(),
			   spotTask,
			   selectedTradeBot.getInitialDelay(),
			   selectedTradeBot.getDelay(),
			   selectedTradeBot.getTimeUnit()
		);
		addMessage("Task added", "Bot " + selectedTradeBot.getTaskId());
	}

	private void addMessage(String summary, String msg) {
		FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, msg);
		FacesContext.getCurrentInstance().addMessage(null, message);
	}
	
	public void prices() {
		Map<String, String> pricesToReturn = new HashMap<>();
		
		for (Map.Entry<String, BotDTO> entry : BotExtraInfo.getMap().entrySet()) {
			pricesToReturn.put(entry.getKey(), entry.getValue().getLastPrice().setScale(2, RoundingMode.HALF_DOWN).toString());
		}
		PrimeRequestContext.getCurrentInstance().getCallbackParams()
			   .put("returnedValue", new JSONObject(pricesToReturn).toString());
	}
	
	public void lastChecks() {
		Map<String, String> lastChecksToReturn = new HashMap<>();
		SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		for (Map.Entry<String, BotDTO> entry : BotExtraInfo.getMap().entrySet()) {
			lastChecksToReturn.put(entry.getKey(), timeFormat.format(entry.getValue().getLastCheck()));
		}
		PrimeRequestContext.getCurrentInstance().getCallbackParams()
			   .put("returnedValue", new JSONObject(lastChecksToReturn).toString());
	}
	
	public void cycleStates() {
		
		//this is for receiving the map from js, don't need it for now
		//Map<String, String[]> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterValuesMap();
		//String[] paramValues = params.get("taskParam");
		
		Map<String, Boolean> mapToReturn = new HashMap<>();
		
		for (Map.Entry<String, BotDTO> entry : BotExtraInfo.getMap().entrySet()) {
			mapToReturn.put(entry.getKey(), entry.getValue().isStopCycle());
		}
		
		JSONObject jsonObject = new JSONObject(mapToReturn);
		String jsonString = jsonObject.toString();

		PrimeRequestContext.getCurrentInstance().getCallbackParams()
			   .put("returnedValue", jsonString);		
	}
	
	public void updateCycleState() {
		String taskId = FacesContext.getCurrentInstance().
			getExternalContext().getRequestParameterMap().get("taskId");

		String stopCycle = FacesContext.getCurrentInstance().
			getExternalContext().getRequestParameterMap().get("stopCycle");

		String onlyRemove = FacesContext.getCurrentInstance().
			getExternalContext().getRequestParameterMap().get("onlyRemove");

		if (Boolean.parseBoolean(onlyRemove)) {
			if (BotExtraInfo.containsInfo(taskId)) {
				BotExtraInfo.getMap().remove(taskId);
			}
			return;
		}

		if (BotExtraInfo.containsInfo(taskId)) {
			BotDTO botDTO = BotExtraInfo.getInfo(taskId);
			if(botDTO.isStopCycle() == true && Boolean.parseBoolean(stopCycle) == true) {
				return;
			}
			botDTO.setStopCycle(Boolean.parseBoolean(stopCycle));
			BotExtraInfo.putInfo(taskId, botDTO);
			addMessage("Stop Cycle updated to " + Boolean.valueOf(stopCycle), "Bot " + taskId);
		} else {			
			BotExtraInfo.putInfo(taskId, new BotDTO(BigDecimal.ZERO, Boolean.parseBoolean(stopCycle), new Date(0), false));
			addMessage("Stop Cycle added and updated to " + Boolean.valueOf(stopCycle), "Bot " + taskId);
		}
		
	}
	
	public void runningStates() {
		List<String> listToReturn = new ArrayList<>();		
		for (String key : taskService.getScheduledTasks().keySet()) {
			listToReturn.add(key);
		}
		PrimeRequestContext.getCurrentInstance().getCallbackParams()
			   .put("returnedValue", new JSONArray(listToReturn).toString());
	}
	
	public void updateRunningState() throws Exception {
		String taskId = FacesContext.getCurrentInstance().
			getExternalContext().getRequestParameterMap().get("taskId");
		
		String runState = FacesContext.getCurrentInstance().
			getExternalContext().getRequestParameterMap().get("runState");

		if(Boolean.parseBoolean(runState)) {
			if(taskService.getScheduledTasks().containsKey(taskId)){
				return;
			}
			TradeBot bot = TradeBotDB.getOneTradeBot(taskId);
			SpotTask task = new SpotTask(bot);
			taskService.addTask(bot.getTaskId(),
				   task,
				   bot.getInitialDelay(),
				   bot.getDelay(),
				   bot.getTimeUnit()
			);
			addMessage("Task added", "Bot " + bot.getTaskId());
		} else {
			if(taskService.getScheduledTasks().containsKey(taskId)) {
				taskService.removeTask(taskId);
				addMessage("Task " + taskId + " removed", "");
			}
		}
	}
	
	public boolean getStopLossStatus(String taskId) {
		if(BotExtraInfo.containsInfo(taskId)) {
			BotDTO botDTO = BotExtraInfo.getInfo(taskId);
				return botDTO.isStopLossWarningTriggered();
		}
		return false;
	}
	
	public void createSellOrder(TradeBot bot, OrderTracker order, BigDecimal newPrice) throws Exception {
		long timeStamp = System.currentTimeMillis();

		BigDecimal quoteQty = BigDecimal.ZERO;		

		if (bot.isProfitBase()) {
			BigDecimal temp = new BigDecimal(bot.getQuoteOrderQty());
			quoteQty = quoteQty.add(temp.setScale(8, RoundingMode.DOWN));
		} else {
			BigDecimal temp = (new BigDecimal(bot.getQuoteOrderQty()).divide(order.getBuyPrice(), 8, RoundingMode.DOWN)).multiply(newPrice);
			quoteQty = quoteQty.add(temp.setScale(8, RoundingMode.DOWN));
		}

		String orderResult = spotClientImpl.createTrade().newOrder(OrdersParams.getOrderParams(
			   bot.getSymbol(),
			   OrderSide.SELL,
			   quoteQty.setScale(8, RoundingMode.DOWN),
			   timeStamp));
		
		JSONObject orderResultJson = new JSONObject(orderResult);
		
		OrderTracker orderSave = order;
		
		orderSave.setSell(true);
		orderSave.setSellPrice(newPrice);
		orderSave.setSellDate(LocalDateTime.now());
		orderSave.setSellOrderId(orderResultJson.getLong("orderId"));
		BigDecimal temp = (new BigDecimal(bot.getQuoteOrderQty()).divide(orderSave.getBuyPrice(), 8, RoundingMode.DOWN)).multiply(newPrice);
		BigDecimal earnings = temp.subtract(new BigDecimal(bot.getQuoteOrderQty()));
		orderSave.setProfit(earnings.setScale(8, RoundingMode.DOWN));
		OrderDB.updateOrder(orderSave);
	}

	public void checkPrice(String symbol) {
		String result = spotClientImpl.createMarket().tickerSymbol(OrdersParams.getTickerSymbolParams(symbol));
          JSONObject jsonObject = new JSONObject(result);
          checkedPrice = new BigDecimal(jsonObject.getString("price"));
	}

	public void deleteBot(int id) throws Exception {
		OrderDB.removeOrders(id);
		ErrorTrackerDB.removeErrors(id);
		TradeBotDB.deleteTradeBot(id);
		addMessage("Bot Removed", "");
	}

     public void deleteBotOrders(int id) throws Exception {
		OrderDB.removeOrders(id);
		addMessage("Orders deleted", "");
	}

	public int fetchOrderCount(int id) throws Exception {
		return OrderDB.getOrderCount(id, false, false);
	}

	public void createSellAllOrders(TradeBot bot, BigDecimal newPrice) throws Exception {
		List<OrderTracker> orders = OrderDB.getOrdersFromBot(false, bot.getId());

		for (OrderTracker order : orders) {
			createSellOrder(bot, order, newPrice);
		}
		addMessage("Orders sold for bot " + bot.getSymbol() + " " + bot.getTaskId(), "");
	}

	public void createSellOrdersFromAllBots() throws Exception {
		List<TradeBot> bots = TradeBotDB.getAllTradeBots();
		for (TradeBot bot : bots) {
			checkPrice(bot.getSymbol());
			createSellAllOrders(bot,checkedPrice);
		}
	}
}

package com.tradebot.backingbean;

import com.binance.connector.client.impl.SpotClientImpl;
import com.tradebot.binance.SpotClientConfig;
import com.tradebot.configuration.OrdersParams;
import com.tradebot.db.ErrorTrackerDB;
import com.tradebot.db.OrderDB;
import com.tradebot.db.TradeBotDB;
import com.tradebot.model.ErrorTracker;
import com.tradebot.model.OrderTracker;
import com.tradebot.model.TradeBot;
import com.tradebot.service.Task;
import com.tradebot.service.TaskService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import org.json.JSONObject;
import org.primefaces.PrimeFaces;

@Named
@ViewScoped
@Data
public class IndexView implements Serializable {

	@Inject
	private TaskService taskService;

	private List<TradeBot> bots;
	
	private List<OrderTracker> botOrders;
	
	private TradeBot selectedTradeBot;

	private TradeBot secondTradeBot;

	private boolean shouldEditBot;
	
	private SpotClientImpl spotClientImpl;
	
	private List<String> orderJsonString;
	
	private List<String> accountInfoJsonString;
	
	private List<ErrorTracker> errors;
	
	@PostConstruct
	private void init() {
		selectedTradeBot = new TradeBot();
		try {
			bots = TradeBotDB.getAllTradeBots();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		spotClientImpl = SpotClientConfig.spotClientSignTest();
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
	
	public BigDecimal getBotProfit(long botId) throws Exception {
		BigDecimal temp = OrderDB.getTadeBotProfits(botId);
		return temp.setScale(2, RoundingMode.HALF_DOWN);
	}
	
	public void getAccountInfo() {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("timestamp", System.currentTimeMillis());
		
		String temp = spotClientImpl.createTrade().account(parameters);
		JSONObject json = new JSONObject(temp);		
		List<String> list = new ArrayList<>();
		String[] lines = json.toString(2).split("\\r?\\n");
		list.addAll(Arrays.asList(lines));
		accountInfoJsonString = list;
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
				addTask();
			}
			bots = TradeBotDB.getAllTradeBots();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		PrimeFaces.current().executeScript("PF('manageBot').hide()");
	}
	
	public void getBotErrors(long botId) throws Exception {
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
		Task task = new Task(selectedTradeBot);

		taskService.addTask(selectedTradeBot.getTaskId(),
			   task,
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
}

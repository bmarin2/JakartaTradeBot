package com.tradebot.backingbean;

import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.tradebot.binance.UMFuturesClientConfig;
import com.tradebot.configuration.FuturesOrderParams;
import com.tradebot.db.FuturesBotDB;
import com.tradebot.enums.ChartMode;
import com.tradebot.enums.FutresStrategy;
import com.tradebot.model.FuturesAccountBalance;
import com.tradebot.model.FuturesBot;
import com.tradebot.model.OrderSide;
import com.tradebot.service.FuturesTaskMACDCross;
import com.tradebot.service.FuturesTaskOneCross;
import com.tradebot.service.FuturesTaskOneCrossBorder;
import com.tradebot.service.FuturesTaskTwoCross;
import com.tradebot.service.FuturesTaskTwoCrossTP;
import com.tradebot.service.TaskService;
import com.tradebot.service.futures.FuturesTaskStoRsi;
import com.tradebot.service.futures.FuturesTaskStoRsiTP;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import org.json.JSONArray;
import org.json.JSONObject;
import org.primefaces.context.PrimeRequestContext;

@Named
@ViewScoped
@Data
public class FuturesView implements Serializable {
	
	@Inject
	private TaskService taskService;

	private List<FuturesBot> bots;
	
	private FuturesAccountBalance futuresAccountBalanceUSDT;
	private FuturesBot selectedTradeBot;
	private boolean shouldEditBot;
	private UMFuturesClientImpl umFuturesClientImpl;
	
	@PostConstruct
	private void init() {
		selectedTradeBot = new FuturesBot();
		umFuturesClientImpl = UMFuturesClientConfig.futuresSignedTest();
		futuresAccountBalanceUSDT = getUSDTBalance();
		try {
			bots = FuturesBotDB.getAllFuturesBots();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
	}
	
	public void updateBot() {
		try {
			if (shouldEditBot) {
				FuturesBotDB.updateFuturesBot(selectedTradeBot);
				addMessage("Bot Updated", "");
			} else {
				long bot_id = FuturesBotDB.addFuturesBot(selectedTradeBot);
				selectedTradeBot.setId(bot_id);
				addMessage("New bot added", "");
				// addTask();
			}
			bots = FuturesBotDB.getAllFuturesBots();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public TimeUnit[] getUnits() {
		return TimeUnit.values();
	}
	
	public FutresStrategy[] getStrategies() {
		return FutresStrategy.values();
	}
	
	public ChartMode[] getChartModes() {
		return ChartMode.values();
	}

	private void addMessage(String summary, String msg) {
		FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, msg);
		FacesContext.getCurrentInstance().addMessage(null, message);
	}

	public void newBot() {
		shouldEditBot = false;
		selectedTradeBot = new FuturesBot();
	}

	public void editBot() {
		shouldEditBot = true;
	}
	
	public void createLongOrder() {
		createOrder(OrderSide.BUY);
	}
	
	public void createShortOrder() {
		createOrder(OrderSide.SELL);
	}

	public void createOrder(OrderSide orderSide) {
		long timeStamp = System.currentTimeMillis();
		
		String orderResult = umFuturesClientImpl.account().newOrder(
				FuturesOrderParams.getOrderParams("LTCUSDT", orderSide, OrderSide.BOTH, 1.0, timeStamp)
		);
		
		System.out.println("Order Result:");
		System.out.println(orderResult);
	}
	
	public void createSLOrderSell() {
		createStopLossOrder(OrderSide.SELL);
	}
	
	public void createSLOrderBuy() {
		createStopLossOrder(OrderSide.BUY);
	}
	
	public void createStopLossOrder(OrderSide orderSide) {
		long timeStamp = System.currentTimeMillis();

		String orderResult = umFuturesClientImpl.account().newOrder(
//			   FuturesOrderParams.getOrderParams("LTCUSDT", orderSide, OrderSide.BOTH, 0.73, timeStamp)
			   
			FuturesOrderParams.getStopLossParams("LTCUSDT",
					orderSide, OrderSide.BOTH, 0.73, 70.12, timeStamp)
		);

		System.out.println("Order Result:");
		System.out.println(orderResult);
	}
	
	public String redirectToFuturesDetails(String id) {
		return "/futuresDetails.xhtml?faces-redirect=true&botid=" + id;
	}
	
	public FuturesAccountBalance getUSDTBalance() {
		long timeStamp = System.currentTimeMillis();
		String jsonResult = umFuturesClientImpl.account().futuresAccountBalance(
				FuturesOrderParams.getAccuntInfoParams(timeStamp));
		
		FuturesAccountBalance futuresAccountBalance = new FuturesAccountBalance();
		JSONArray accounts = new JSONArray(jsonResult);

		for (int i = 0; i < accounts.length(); i++) {
			JSONObject accountObject = accounts.getJSONObject(i);

			if (accountObject.optString("asset").equals("USDT")) {
				futuresAccountBalance.setAccountAlias(accountObject.optString("accountAlias"));
				futuresAccountBalance.setAsset(accountObject.optString("asset"));
				futuresAccountBalance.setBalance(accountObject.optString("balance"));
				futuresAccountBalance.setCrossWalletBalance(accountObject.optString("crossWalletBalance"));
				futuresAccountBalance.setCrossUnPnl(accountObject.optString("crossUnPnl"));
				futuresAccountBalance.setAvailableBalance(accountObject.optString("availableBalance"));
				futuresAccountBalance.setMaxWithdrawAmount(accountObject.optString("maxWithdrawAmount"));
				futuresAccountBalance.setMarginAvailable(accountObject.optBoolean("marginAvailable"));
				futuresAccountBalance.setUpdateTime(accountObject.optLong("updateTime"));
				break;
			}
		}
		return futuresAccountBalance;
	}
	
	public void updateRunningState() throws Exception {
		String taskId = FacesContext.getCurrentInstance().
			   getExternalContext().getRequestParameterMap().get("taskId");

		String runState = FacesContext.getCurrentInstance().
			   getExternalContext().getRequestParameterMap().get("runState");

		if (Boolean.parseBoolean(runState)) {
			if (taskService.getScheduledTasks().containsKey(taskId)) {
				return;
			}
			FuturesBot bot = FuturesBotDB.getOneTradeBot(taskId);
			
			Runnable task = null;
			
			if (bot.getFutresDemaStrategy() == FutresStrategy.ONE_CROSS) {
				task = new FuturesTaskOneCross(bot);  

			} else if (bot.getFutresDemaStrategy() == FutresStrategy.TWO_CROSS){
				task = new FuturesTaskTwoCross(bot);

			} else if (bot.getFutresDemaStrategy() == FutresStrategy.TWO_CROSS_TAKE_PROFIT){
				task = new FuturesTaskTwoCrossTP(bot);

			} else if (bot.getFutresDemaStrategy() == FutresStrategy.ONE_CROSS_BORDER){
				task = new FuturesTaskOneCrossBorder(bot);

			} else if (bot.getFutresDemaStrategy() == FutresStrategy.MACD_CROSS){
				task = new FuturesTaskMACDCross(bot);

			} else if (bot.getFutresDemaStrategy() == FutresStrategy.STOCH_RSI){
				task = new FuturesTaskStoRsi(bot);

			} else if (bot.getFutresDemaStrategy() == FutresStrategy.STOCH_RSI_TP){
				task = new FuturesTaskStoRsiTP(bot);
			}

			taskService.addTask(bot.getTaskId(),
				   task,
				   bot.getInitialDelay(),
				   bot.getDelay(),
				   bot.getTimeUnit()
			);
			addMessage("Task added", "Bot " + bot.getTaskId());
			

		} else {
			if (taskService.getScheduledTasks().containsKey(taskId)) {
				taskService.removeTask(taskId);
				addMessage("Task " + taskId + " removed", "");
			}
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
}

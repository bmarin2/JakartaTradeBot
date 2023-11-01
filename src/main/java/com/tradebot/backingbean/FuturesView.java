package com.tradebot.backingbean;

import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.tradebot.binance.UMFuturesClientConfig;
import com.tradebot.configuration.FuturesOrderParams;
import com.tradebot.db.FuturesBotDB;
import com.tradebot.model.AccountTradeList;
import com.tradebot.model.FuturesAccountBalance;
import com.tradebot.model.FuturesBot;
import com.tradebot.model.OrderSide;
import com.tradebot.model.PositionSide;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import org.json.JSONArray;
import org.json.JSONObject;

@Named
@ViewScoped
@Data
public class FuturesView implements Serializable {
	
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
				FuturesOrderParams.getOrderParams("LTCUSDT", orderSide, PositionSide.BOTH, 0.73, timeStamp)		
		);
		
		System.out.println("Order Result:");
		System.out.println(orderResult);
	}
	
	public void getAccountTradeList() {
		
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
}

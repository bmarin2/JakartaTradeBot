package com.tradebot.backingbean;

import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.tradebot.binance.UMFuturesClientConfig;
import com.tradebot.configuration.FuturesOrderParams;
import com.tradebot.db.FuturesBotDB;
import com.tradebot.model.AccountTradeList;
import com.tradebot.model.FuturesBot;
import com.tradebot.model.PositionInformation;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Data;
import org.json.JSONArray;
import org.json.JSONObject;

@Named
@ViewScoped
@Data
public class FuturesDetailsView implements Serializable {
	
	//private List<FutureOrder> orders;
	private UMFuturesClientImpl umFuturesClientImpl;
	private List<PositionInformation> positionList;
	private long botid;
	private FuturesBot futuresBot;
	private List<AccountTradeList> accountTradeList;
	
	@PostConstruct
	private void init() {
		umFuturesClientImpl = UMFuturesClientConfig.futuresSignedTest();
		
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();
		botid = Long.valueOf(externalContext.getRequestParameterMap().get("botid"));
		
		try {
			futuresBot = FuturesBotDB.getOneFuturesBot(botid);
			positionList = getPositionInformations();
			accountTradeList = getAccountTradeList();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
//		try {
//			orders = FuturesBotDB.getAllFuturesBots();
//		} catch (Exception ex) {
//			ex.printStackTrace();
//		}
		
	}
	
	public List<PositionInformation> getPositionInformations() {
		long timeStamp = System.currentTimeMillis();
		String jsonResult = umFuturesClientImpl.account().positionInformation(
			   FuturesOrderParams.getPositionInformationParams(futuresBot.getSymbol(), timeStamp)
		);
		
		List<PositionInformation> positionList = new ArrayList<>();

		JSONArray positions = new JSONArray(jsonResult);

		for (int i = 0; i < positions.length(); i++) {
			JSONObject positionObject = positions.getJSONObject(i);

			PositionInformation positionInfo = new PositionInformation();

			positionInfo.setEntryPrice(positionObject.optString("entryPrice"));
			positionInfo.setBreakEvenPrice(positionObject.optString("breakEvenPrice"));
			positionInfo.setMarginType(positionObject.optString("marginType"));
			positionInfo.setIsAutoAddMargin(positionObject.optString("isAutoAddMargin"));
			positionInfo.setIsolatedMargin(positionObject.optString("isolatedMargin"));
			positionInfo.setLeverage(positionObject.optString("leverage"));
			positionInfo.setLiquidationPrice(positionObject.optString("liquidationPrice"));
			positionInfo.setMarkPrice(positionObject.optString("markPrice"));
			positionInfo.setMaxNotionalValue(positionObject.optString("maxNotionalValue"));
			positionInfo.setPositionAmt(positionObject.optString("positionAmt"));
			positionInfo.setNotional(positionObject.optString("notional"));
			positionInfo.setIsolatedWallet(positionObject.optString("isolatedWallet"));
			positionInfo.setSymbol(positionObject.optString("symbol"));
			positionInfo.setUnRealizedProfit(positionObject.optString("unRealizedProfit"));
			positionInfo.setPositionSide(positionObject.optString("positionSide"));
			positionInfo.setUpdateTime(positionObject.optLong("updateTime"));

			positionList.add(positionInfo);
		}
		return positionList;
	}	
	
	public List<AccountTradeList> getAccountTradeList() {
		long timeStamp = System.currentTimeMillis();		
		String jsonResult = umFuturesClientImpl.account().accountTradeList(
				FuturesOrderParams.getAccountTradeListParams(futuresBot.getSymbol(), timeStamp));			   
		
		List<AccountTradeList> accountTradeList = new ArrayList<>();
		JSONArray positions = new JSONArray(jsonResult);
		
		for (int i = 0; i < positions.length(); i++) {
			JSONObject tradeObject = positions.getJSONObject(i);
			AccountTradeList accountList = new AccountTradeList();
			
			accountList.setBuyer(tradeObject.optBoolean("buyer"));
			accountList.setCommission(tradeObject.optString("commission"));
			accountList.setCommissionAsset(tradeObject.optString("commissionAsset"));
			accountList.setId(tradeObject.optLong("id"));
			accountList.setMaker(tradeObject.optBoolean("maker"));
			accountList.setOrderId(tradeObject.optLong("orderId"));
			accountList.setPrice(tradeObject.optString("price"));
			accountList.setQty(tradeObject.optString("qty"));
			accountList.setQuoteQty(tradeObject.optString("quoteQty"));
			accountList.setRealizedPnl(tradeObject.optString("realizedPnl"));
			accountList.setSide(tradeObject.optString("side"));
			accountList.setPositionSide(tradeObject.optString("positionSide"));
			accountList.setSymbol(tradeObject.optString("symbol"));
			accountList.setTime(tradeObject.optLong("time"));
			
			accountTradeList.add(accountList);
		}
		Collections.reverse(accountTradeList);
		return accountTradeList;
	}
}

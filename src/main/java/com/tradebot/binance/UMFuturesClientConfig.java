package com.tradebot.binance;

import com.binance.connector.futures.client.impl.UMFuturesClientImpl;

public class UMFuturesClientConfig {
		
	public static UMFuturesClientImpl futuresClientOnlyBaseURLProd() {
		return new UMFuturesClientImpl();
	}
}

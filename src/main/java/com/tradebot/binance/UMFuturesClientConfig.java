package com.tradebot.binance;

import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.tradebot.configuration.ConfigParams;

public class UMFuturesClientConfig {
		
	public static UMFuturesClientImpl futuresBaseURLProd() {
		return new UMFuturesClientImpl();
	}
	
	public static UMFuturesClientImpl futuresSignedProd() {
		return new UMFuturesClientImpl(ConfigParams.F_API_KEY, 
								 ConfigParams.F_SECRET_KEY);
	}
	
	public static UMFuturesClientImpl futuresBaseURLTest() {
		return new UMFuturesClientImpl(ConfigParams.F_TESTNET_BASE_URL);
	}
	
	public static UMFuturesClientImpl futuresSignedTest() {
		return new UMFuturesClientImpl(ConfigParams.F_TESTNET_API_KEY,
								 ConfigParams.F_TESTNET_SECRET_KEY,
								 ConfigParams.F_TESTNET_BASE_URL);
	}
}

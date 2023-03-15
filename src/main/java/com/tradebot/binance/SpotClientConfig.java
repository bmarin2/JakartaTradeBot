package com.tradebot.binance;

import com.binance.connector.client.impl.SpotClientImpl;
import com.tradebot.configuration.ConfigParams;

public class SpotClientConfig {

	public static SpotClientImpl spotClientSignTest() {	
		return new SpotClientImpl(ConfigParams.TESTNET_API_KEY, 
				ConfigParams.TESTNET_SECRET_KEY,
				ConfigParams.TESTNET_BASE_URL
		);
	}
	
	public static SpotClientImpl spotClientSignProd() {		
		return new SpotClientImpl(ConfigParams.API_KEY, 
				ConfigParams.SECRET_KEY,
				ConfigParams.BASE_URL
		);
	}
	
	// only base url, for ticker price Testnet
	public static SpotClientImpl spotClientOnlyBaseURLTest() {	
		return new SpotClientImpl(ConfigParams.TESTNET_BASE_URL);
	}
	
	// only base url, for ticker price Prod
	public static SpotClientImpl spotClientOnlyBaseURLProd() {
		return new SpotClientImpl(ConfigParams.BASE_URL);
	}


}

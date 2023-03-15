package com.tradebot.configuration;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public final class ConfigParams {
	
	public static final String BASE_URL = "https://api.binance.com";
	public static final String API_KEY = "";
	public static final String SECRET_KEY = ""; // Unnecessary if PRIVATE_KEY_PATH is used
	public static final String PRIVATE_KEY_PATH = ""; // Key must be PKCS#8 standard
	
	public static final String TESTNET_BASE_URL = "https://testnet.binance.vision";
	public static final String TESTNET_API_KEY = "l8oRKwsLbEUkyY3gNBN7AnP7QCNIIXogKapvyh2Hwc4EAyWN02eywktbX8NDVqzf";
	public static final String TESTNET_SECRET_KEY = "tAjps8kr24NhTmupWJxOH8GTWETWFs9gLKilnTwHfsOyA8LsdpV8XOYuC8HzLCFO";
	public static final String TESTNET_PRIVATE_KEY_PATH = "";
	
}
package com.tradebot.configuration;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public final class ConfigParams {

	// Spot prod
	public static final String BASE_URL = "https://api.binance.com";
	public static final String API_KEY = "";
	public static final String SECRET_KEY = ""; // Unnecessary if PRIVATE_KEY_PATH is used
	public static final String PRIVATE_KEY_PATH = ""; // Key must be PKCS#8 standard

	// Spot test
	public static final String TESTNET_BASE_URL = "https://testnet.binance.vision";
	public static final String TESTNET_API_KEY = "l8oRKwsLbEUkyY3gNBN7AnP7QCNIIXogKapvyh2Hwc4EAyWN02eywktbX8NDVqzf";
	public static final String TESTNET_SECRET_KEY = "tAjps8kr24NhTmupWJxOH8GTWETWFs9gLKilnTwHfsOyA8LsdpV8XOYuC8HzLCFO";
	public static final String TESTNET_PRIVATE_KEY_PATH = "";
	
	// Futures prod
	public static final String F_API_KEY = "";
	public static final String F_SECRET_KEY = "";

	// Futures test
	public static final String F_TESTNET_BASE_URL = "https://testnet.binancefuture.com";
	public static final String F_TESTNET_API_KEY = "4c1c95c92f0377de7039f2ba03909520315885bc2168fe70e23acf4a5f15155d";
	public static final String F_TESTNET_SECRET_KEY = "a026a08f2a16de806a62623dc676eae623d830b279ad261e754b0f72d923b72d";
}
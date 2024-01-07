package com.tradebot.enums;

import java.util.EnumSet;

public enum ChartMode {
	SPOT_BASE_URL_PROD,
	SPOT_SIGNED_PROD,
	SPOT_BASE_URL_TEST,
	SPOT_SIGNED_TEST,
	FUTURES_BASE_URL_PROD,
	FUTURES_SIGNED_PROD,
	FUTURES_BASE_URL_TEST,
	FUTURES_SIGNED_TEST;

	public static final EnumSet<ChartMode> FUTURES_LIST = EnumSet.of(
		   ChartMode.FUTURES_BASE_URL_PROD,
		   ChartMode.FUTURES_SIGNED_PROD,
		   ChartMode.FUTURES_BASE_URL_TEST,
		   ChartMode.FUTURES_SIGNED_TEST	
	);
}

package com.tradebot.enums;

import java.util.EnumSet;

public enum Environment {
	SPOT_BASE_URL_PROD,
	SPOT_SIGNED_PROD,
	SPOT_BASE_URL_TEST,
	SPOT_SIGNED_TEST,
	FUTURES_BASE_URL_PROD,
	FUTURES_SIGNED_PROD,
	FUTURES_BASE_URL_TEST,
	FUTURES_SIGNED_TEST;

	public static final EnumSet<Environment> FUTURES_LIST = EnumSet.of(Environment.FUTURES_BASE_URL_PROD,
		   Environment.FUTURES_SIGNED_PROD,
		   Environment.FUTURES_BASE_URL_TEST,
		   Environment.FUTURES_SIGNED_TEST	
	);
}

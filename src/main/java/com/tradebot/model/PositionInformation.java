package com.tradebot.model;

import java.io.Serializable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PositionInformation implements Serializable {
	private String entryPrice;
	private String breakEvenPrice;
	private String marginType;
	private String isAutoAddMargin;
	private String isolatedMargin;
	private String leverage;
	private String liquidationPrice;
	private String markPrice;
	private String maxNotionalValue;
	private String positionAmt;
	private String notional;
	private String isolatedWallet;
	private String symbol;
	private String unRealizedProfit;
	private String positionSide;
	private long updateTime;
}

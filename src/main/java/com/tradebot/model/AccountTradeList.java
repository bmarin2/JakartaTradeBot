package com.tradebot.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AccountTradeList {
	private boolean buyer;
	private String commission;
	private String commissionAsset;
	private long id;
	private boolean maker;
	private long orderId;
	private String price;
	private String qty;
	private String quoteQty;
	private String realizedPnl;
	private String side;
	private String positionSide;
	private String symbol;
	private long time;
}

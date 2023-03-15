package com.tradebot.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderTracker {
	
	private long id;

	private String symbol;

	private String side;

	private long quoteOrderQty;

	private long timestamp;

	private long orderId;	
}

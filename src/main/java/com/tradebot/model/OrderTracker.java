package com.tradebot.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderTracker {
	
	private long id;
	
	private Boolean sell = false;
	
	private BigDecimal buyPrice;
	
	private BigDecimal sellPrice;
	
	private BigDecimal profit;

	private LocalDateTime buyDate = LocalDateTime.now();
	
	private LocalDateTime sellDate;	

	private long buyOrderId;
	
	private long sellOrderId;
	
	private long tradebot_id;
}

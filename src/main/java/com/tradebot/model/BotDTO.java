package com.tradebot.model;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class BotDTO {
	private boolean stopCycle;
	private BigDecimal lastPrice;
	
	public BotDTO(BigDecimal lastPrice, boolean stopCycle) {
		this.lastPrice = lastPrice;
		this.stopCycle = stopCycle;
	}
}

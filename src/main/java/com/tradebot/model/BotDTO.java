package com.tradebot.model;

import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

@Data
public class BotDTO {
	private boolean stopCycle;
	private BigDecimal lastPrice;
	private Date lastCheck;
	
	public BotDTO(BigDecimal lastPrice, boolean stopCycle, Date lastCheck) {
		this.lastPrice = lastPrice;
		this.stopCycle = stopCycle;
		this.lastCheck = lastCheck;
	}
}

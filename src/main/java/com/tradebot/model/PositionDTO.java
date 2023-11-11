package com.tradebot.model;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class PositionDTO {
	private BigDecimal buyPrice;
	private BigDecimal stopLossPrice;

	public PositionDTO(BigDecimal buyPrice, BigDecimal stopLossPrice) {
		this.buyPrice = buyPrice;
		this.stopLossPrice = stopLossPrice;
	}	
}

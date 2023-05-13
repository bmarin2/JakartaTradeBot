package com.tradebot.model;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class PositionDTO {
	private BigDecimal buyPrice;
	private BigDecimal stopLossPrice;
	private BigDecimal stopLossWarningPrice;

	public PositionDTO(BigDecimal buyPrice, BigDecimal stopLossPrice, BigDecimal stopLossWarningPrice) {
		this.buyPrice = buyPrice;
		this.stopLossPrice = stopLossPrice;
		this.stopLossWarningPrice = stopLossWarningPrice;
	}	
}

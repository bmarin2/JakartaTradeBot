package com.tradebot.model;

import com.tradebot.util.TaskCodeGeneratorService;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TradeBot implements Serializable {
	
	private long id;

	private String symbol;
	
	private LocalDateTime createdDate = LocalDateTime.now();
	
	private String taskId = TaskCodeGeneratorService.generateRandomString();
	
	private Integer quoteOrderQty = 10;
	
	private Integer cycleMaxOrders = 5;
	
	private Double orderStep = 0.6;
	
	private String description;
	
	private Integer initialDelay = 1;
		
	private Integer delay = 5;
	
	private TimeUnit timeUnit = TimeUnit.MINUTES;
}

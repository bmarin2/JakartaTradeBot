package com.tradebot.model;

import com.tradebot.enums.FutresDemaStrategy;
import com.tradebot.util.TaskCodeGeneratorService;
import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FuturesBot implements Serializable {
	
	private long id;

	private String symbol;
	
	private Date createdDate = new Date();
	
	private String taskId = TaskCodeGeneratorService.generateRandomString();
	
	private Double quantity;
	
	private String description;
	
	private Integer initialDelay = 1;
		
	private Integer delay = 5;
	
	private TimeUnit timeUnit = TimeUnit.MINUTES;
	
	private Double stopLoss = 10.0;
	
	private String demaAlertTaskId;
	
	private FutresDemaStrategy futresDemaStrategy = FutresDemaStrategy.ONE_CROSS;
}
package com.tradebot.model;

import com.tradebot.enums.Environment;
import com.tradebot.enums.FutresStrategy;
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
	private Double stopLoss = 0.5;
	private Double takeProfit = 0.5;
	private FutresStrategy futresStrategy = FutresStrategy.FUTURES_RSI2;
	private Environment environment = Environment.FUTURES_SIGNED_TEST;

	private String intervall = "5m";
	private String intervall2 = "1h";
}
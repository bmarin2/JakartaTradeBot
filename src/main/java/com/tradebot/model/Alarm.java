package com.tradebot.model;

import com.tradebot.util.TaskCodeGeneratorService;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Alarm implements Serializable {
	private long id;
	private String symbol;
	private String alarmId = TaskCodeGeneratorService.generateRandomString();
	private BigDecimal alarmPrice;
	private Integer initialDelay = 1;
	private Integer delay = 5;
	private TimeUnit timeUnit = TimeUnit.MINUTES;
	private String description;
	private Boolean msgSent = false;
	// for dema alerts
	private String intervall = "5m";
	private Integer fastDema = 10;
	private Integer slowDema = 20;
	private Boolean crosss = false;
}

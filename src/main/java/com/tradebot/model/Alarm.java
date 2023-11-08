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
	private Integer firstDema = 10;
	private Integer secondDema = 20;
	private Integer thirdDema = 200;
	private Boolean crosss = false;
	private Double currentFirstDema = 0.0;
	private Double currentSecondDema = 0.0;
	private Double currentThirdDema = 0.0;
	private Boolean crosssBig = false;
        private Double lastClosingCandle = 0.0;
}

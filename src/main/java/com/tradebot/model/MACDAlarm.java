package com.tradebot.model;

import com.tradebot.enums.ChartMode;
import com.tradebot.util.TaskCodeGeneratorService;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MACDAlarm implements Serializable {
	private long id;
	private String symbol;
	private String alarmId = TaskCodeGeneratorService.generateRandomString();
	private Integer initialDelay = 1;
	private Integer delay = 1;
	private TimeUnit timeUnit = TimeUnit.MINUTES;
	private String description;
	private String intervall = "1m";
	private Integer dema = 200;
	private Double currentEma = 0.0;
	private Boolean crosss = false;
	private Double macdLine = 0.0;
	private Double signalLine = 0.0;
	private Double lastClosingCandle = 0.0;
	private Double minGap = 0.0;
	private ChartMode chartMode = ChartMode.FUTURES;
}

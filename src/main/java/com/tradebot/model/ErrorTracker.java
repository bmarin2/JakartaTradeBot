package com.tradebot.model;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ErrorTracker {	
	private long id;
	private LocalDateTime errorTimestamp;
	private String errorMessage;
	private Boolean acknowledged;
	private long tradebot_id;	
}

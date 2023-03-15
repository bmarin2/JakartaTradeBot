package com.tradebot.model;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderTracker {
	
	private long id;

	private Integer side;

	private LocalDateTime createdDate;

	private long orderId;
	
	private long tradebot_id;
}

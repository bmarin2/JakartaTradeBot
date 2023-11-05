package com.tradebot.model;

import java.io.Serializable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FuturesOrder implements Serializable {	
	private long orderId;
	private String type;
	private String status;
	private long time;
}

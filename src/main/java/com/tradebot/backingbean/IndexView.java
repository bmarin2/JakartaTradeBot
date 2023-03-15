package com.tradebot.backingbean;

import com.tradebot.db.OrderDB;
import com.tradebot.db.TradeBotDB;
import com.tradebot.model.OrderSide;
import com.tradebot.model.OrderTracker;
import com.tradebot.model.TradeBot;
import com.tradebot.service.Task;
import com.tradebot.service.TaskService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Data;
import org.primefaces.PrimeFaces;

@Named
@ViewScoped
@Data
public class IndexView implements Serializable {

	@Inject
	private TaskService taskService;
	
	private List<TradeBot> bots;

	private TradeBot selectedTradeBot;

	@PostConstruct
	private void init() {
		selectedTradeBot = new TradeBot();
		try {
			bots = TradeBotDB.getAllTradeBots();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void updateBot() {
		Task task = new Task();
		task.setTradeBot(selectedTradeBot);
		
		taskService.addTask(selectedTradeBot.getTaskId(),
				task, 
				selectedTradeBot.getInitialDelay(),
				selectedTradeBot.getDelay(),
				selectedTradeBot.getTimeUnit());

		try {
			TradeBotDB.addBot(selectedTradeBot);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		System.out.println(selectedTradeBot);
		PrimeFaces.current().executeScript("PF('manageBot').hide()");
	}
	
	
	public void createOrder() {
		OrderTracker orderTracker = new OrderTracker();
		
		
		orderTracker.setSide(OrderSide.BUY.ordinal());
		orderTracker.setTradebot_id(1);
		orderTracker.setCreatedDate(LocalDateTime.now());
		orderTracker.setOrderId(123456789);
		try {
			OrderDB.addOrder(orderTracker);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void newBot() {
		selectedTradeBot = new TradeBot();
	}
	
	public boolean isBotRunning(String taskId) {
		return taskService.getScheduledTasks().containsKey(taskId);
	}

	public TimeUnit[] getUnits() {
		return TimeUnit.values();
	}

	public void confirmStop(){
		System.out.println("BOT STOPPED");
	}
}

package com.tradebot.backingbean;

import com.tradebot.db.TradeBotDB;
import com.tradebot.model.TradeBot;
import com.tradebot.service.Task;
import com.tradebot.service.TaskService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
		bots = getAllBots();
	}
	
	private List<TradeBot> getAllBots() {
		return null;
	}
	
	public void updateBot() {
		Task task = new Task();
		task.setTradeBot(selectedTradeBot);
		
		taskService.addTask(selectedTradeBot.getTaskId(),
				task, 
				selectedTradeBot.getInitialDelay(),
				selectedTradeBot.getDelay(),
				selectedTradeBot.getTimeUnit());

//		tradeBotRepository.saveAndFlush(selectedTradeBot);
		System.out.println(selectedTradeBot);
		PrimeFaces.current().executeScript("PF('manageBot').hide()");
//		PrimeFaces.current().ajax().update("form:messages", "form:dt-releases");
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

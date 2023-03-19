package com.tradebot.backingbean;

import com.tradebot.db.OrderDB;
import com.tradebot.db.TradeBotDB;
import com.tradebot.model.OrderSide;
import com.tradebot.model.OrderTracker;
import com.tradebot.model.TradeBot;
import com.tradebot.service.Task;
import com.tradebot.service.TaskService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDateTime;
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

	private TradeBot secondTradeBot;

	private boolean shouldEditBot;

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
		try {

			if (shouldEditBot) {
				TradeBotDB.updateTradeBot(selectedTradeBot);
				addMessage("Bot Updated", "");
			} else {
				TradeBotDB.addBot(selectedTradeBot);
				addMessage("New bot added", "");
				addTask();
			}

			bots = TradeBotDB.getAllTradeBots();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		PrimeFaces.current().executeScript("PF('manageBot').hide()");
	}

	public int getRunningTasksNumber() {
		return taskService.getScheduledTasks().size();
	}

	public void newBot() {
		shouldEditBot = false;
		selectedTradeBot = new TradeBot();
	}

	public void editBot() {
		shouldEditBot = true;
	}

	public boolean isBotRunning(String taskId) {
		return taskService.getScheduledTasks().containsKey(taskId);
	}

	public TimeUnit[] getUnits() {
		return TimeUnit.values();
	}

	public void removeTask() {
		taskService.removeTask(selectedTradeBot.getTaskId());
		addMessage("Task removed", "Bot " + selectedTradeBot.getTaskId());
	}

	public void addTask() {
		Task task = new Task();
		task.setTradeBot(selectedTradeBot);

		taskService.addTask(selectedTradeBot.getTaskId(),
			   task,
			   selectedTradeBot.getInitialDelay(),
			   selectedTradeBot.getDelay(),
			   selectedTradeBot.getTimeUnit()
		);
		addMessage("Task added", "Bot " + selectedTradeBot.getTaskId());
	}

	private void addMessage(String summary, String msg) {
		FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, msg);
		FacesContext.getCurrentInstance().addMessage(null, message);
	}
}

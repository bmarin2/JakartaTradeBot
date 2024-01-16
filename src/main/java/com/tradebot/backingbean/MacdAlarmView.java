package com.tradebot.backingbean;

import com.tradebot.db.MACDAlarmDB;
import com.tradebot.service.TaskService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import lombok.Data;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.primefaces.PrimeFaces;
import org.primefaces.context.PrimeRequestContext;
import com.tradebot.enums.ChartMode;
import com.tradebot.model.MACDAlarm;
import com.tradebot.service.MACDCross;

@Named
@ViewScoped
@Data
public class MacdAlarmView implements Serializable {
	
	@Inject
	private TaskService taskService;
	
	private List<MACDAlarm> alarms;
	private MACDAlarm selectedAlarm;
	private boolean shouldEditAlarm;
	private BigDecimal checkedPrice;
	
	@PostConstruct
	private void init() {
		selectedAlarm = new MACDAlarm();
		try {
			alarms = MACDAlarmDB.getAllAlarms();
		} catch (Exception ex) {
			ex.printStackTrace();
		}		
	}
	
	public TimeUnit[] getUnits() {
		return TimeUnit.values();
	}
        
     public ChartMode[] getChartModes() {
		return ChartMode.values();
	}

	public void updateAlarm() {
		try {
			if (shouldEditAlarm) {
				MACDAlarmDB.editAlarm(selectedAlarm);
				addMessage("Alarm Updated", "");
			} else {
				long alarm_id = MACDAlarmDB.createAlarm(selectedAlarm);
				selectedAlarm.setId(alarm_id);
				addMessage("New MACD alarm added", "");
				// addTask();
			}
			alarms = MACDAlarmDB.getAllAlarms();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		PrimeFaces.current().executeScript("PF('manageAlarm').hide()");
	}
	
	private void addMessage(String summary, String msg) {
		FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, msg);
		FacesContext.getCurrentInstance().addMessage(null, message);
	}
	
	public void newAlarm() {
		shouldEditAlarm = false;
		selectedAlarm = new MACDAlarm();
	}

	public void editAlarm() {
		shouldEditAlarm = true;
	}

	public void updateRunningState() throws Exception {
		String taskId = FacesContext.getCurrentInstance().
			   getExternalContext().getRequestParameterMap().get("taskId");

		String runState = FacesContext.getCurrentInstance().
			   getExternalContext().getRequestParameterMap().get("runState");

		if (Boolean.parseBoolean(runState)) {
			if (taskService.getScheduledTasks().containsKey(taskId)) {
				return;
			}

			MACDAlarm mACDalarm = MACDAlarmDB.getOneAlarm(taskId);

			MACDCross task = new MACDCross(mACDalarm);

			taskService.addTask(mACDalarm.getAlarmId(),
				   task,
				   mACDalarm.getInitialDelay(),
				   mACDalarm.getDelay(),
				   mACDalarm.getTimeUnit()
			);

			addMessage("Alarm added", "id: " + mACDalarm.getAlarmId());


		} else {
			if (taskService.getScheduledTasks().containsKey(taskId)) {
				taskService.removeTask(taskId);
				addMessage("MACD Alarm " + taskId + " removed", "");
			}
		}
	}
	
	public void runningStates() {
		List<String> listToReturn = new ArrayList<>();
		for (String key : taskService.getScheduledTasks().keySet()) {
			listToReturn.add(key);
		}
		PrimeRequestContext.getCurrentInstance().getCallbackParams()
			   .put("returnedValue", new JSONArray(listToReturn).toString());
	}
	
	public boolean isBotRunning(String taskId) {
		return taskService.getScheduledTasks().containsKey(taskId);
	}
	
}

package com.tradebot.backingbean;

import com.binance.connector.client.impl.SpotClientImpl;
import com.tradebot.binance.SpotClientConfig;
import com.tradebot.configuration.OrdersParams;
import com.tradebot.db.AlarmDB;
import com.tradebot.enums.AlarmType;
import com.tradebot.model.Alarm;
import com.tradebot.service.AlarmTask;
import com.tradebot.service.DemaTwoCrossTask;
import com.tradebot.service.DemaOneCrossTask;
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
import org.json.JSONObject;
import org.primefaces.PrimeFaces;
import org.primefaces.context.PrimeRequestContext;
import com.tradebot.enums.ChartMode;
import com.tradebot.enums.EmaCrossStrategy;
import com.tradebot.service.StochRsiEma;
import com.tradebot.service.TemaOneCrossTask;
import com.tradebot.service.TemaTwoCrossTask;
import com.tradebot.service.ThreeBarsTask;

@Named
@ViewScoped
@Data
public class AlarmView implements Serializable {
	
	@Inject
	private TaskService taskService;
	
	private List<Alarm> alarms;
	private Alarm selectedAlarm;
	private boolean shouldEditAlarm;
	private BigDecimal checkedPrice;
	private SpotClientImpl spotClientImpl;
	private boolean demaAlert;
	private AlarmType currentAlarmType;
	
	@PostConstruct
	private void init() {
		selectedAlarm = new Alarm();
		try {
			alarms = AlarmDB.getAllAlarms();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		spotClientImpl = SpotClientConfig.spotClientSignTest();
		
	}
	
	public TimeUnit[] getUnits() {
		return TimeUnit.values();
	}
        
     public ChartMode[] getChartModes() {
		return ChartMode.values();
	}

	public EmaCrossStrategy[] getEmaCrossStrategies() {
		return EmaCrossStrategy.values();
	}

	public void updateAlarm() {
		try {
			if (shouldEditAlarm) {
				AlarmDB.editAlarm(selectedAlarm);
				addMessage("Alarm Updated", "");
			} else {
				long alarm_id = AlarmDB.createAlarm(selectedAlarm);
				selectedAlarm.setId(alarm_id);
				addMessage("New alarm added", "");
				// addTask();
			}
			alarms = AlarmDB.getAllAlarms();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		PrimeFaces.current().executeScript("PF('manageAlarm').hide()");
	}
	
	private void addMessage(String summary, String msg) {
		FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, msg);
		FacesContext.getCurrentInstance().addMessage(null, message);
	}
	
	public void newAlarm(String type) {
		shouldEditAlarm = false;
		selectedAlarm = new Alarm();
		updateAlarmType(type);
		selectedAlarm.setAlarmType(currentAlarmType);
	}

	public void editAlarm(String type) {
		shouldEditAlarm = true;
		updateAlarmType(type);
	}
	
	public void updateAlarmType(String type) {
		switch (type) {
			case "ALARM":
				currentAlarmType = AlarmType.ALARM;
				break;
			case "DEMA":
				currentAlarmType = AlarmType.DEMA;
				break;
			case "THREE_BARS":
				currentAlarmType = AlarmType.THREE_BARS;
				break;
			case "STOCH_RSI":
				currentAlarmType = AlarmType.STOCH_RSI;
				break;
		}
	}
	
	public BigDecimal checkPrice(String symbol) {
		String result = spotClientImpl.createMarket().tickerSymbol(OrdersParams.getTickerSymbolParams(symbol));
		JSONObject jsonObject = new JSONObject(result);
		return new BigDecimal(jsonObject.getString("price"));
	}
	
	public void updateRunningState() throws Exception {
		String taskId = FacesContext.getCurrentInstance().
			   getExternalContext().getRequestParameterMap().get("taskId");

		String runState = FacesContext.getCurrentInstance().
			   getExternalContext().getRequestParameterMap().get("runState");
		
		String alarmType = FacesContext.getCurrentInstance().
			   getExternalContext().getRequestParameterMap().get("alarmType");

		if (Boolean.parseBoolean(runState)) {
			if (taskService.getScheduledTasks().containsKey(taskId)) {
				return;
			}
			
			Alarm alarm = AlarmDB.getOneAlarm(taskId);
			
			if (alarmType.equals("DEMA")) {
				
				Runnable task = null;

				if (alarm.getFirstDema() == 0) {					
					if (alarm.getEmaCrossStrategy() == EmaCrossStrategy.ONE_CROSS_DEMA) {
						task = new DemaOneCrossTask(alarm);
					} else if(alarm.getEmaCrossStrategy() == EmaCrossStrategy.ONE_CROSS_TEMA) {
						task = new TemaOneCrossTask(alarm);
					}
				} else {
					if (alarm.getEmaCrossStrategy() == EmaCrossStrategy.TWO_CROSS_DEMA) {
						task = new DemaTwoCrossTask(alarm);
					} else if (alarm.getEmaCrossStrategy() == EmaCrossStrategy.TWO_CROSS_TEMA) {
						task = new TemaTwoCrossTask(alarm);
					}
				}
				
				taskService.addTask(alarm.getAlarmId(),
					   task,
					   alarm.getInitialDelay(),
					   alarm.getDelay(),
					   alarm.getTimeUnit()
				);

				addMessage("Alarm added", "id: " + alarm.getAlarmId());

			} else if (alarmType.equals("ALARM")) {
				BigDecimal currentPrice = checkPrice(alarm.getSymbol());

				boolean greater = false;

				if (currentPrice.compareTo(alarm.getAlarmPrice()) < 0) {
					greater = true;
				}

				AlarmTask alarmTask = new AlarmTask(alarm, greater);

				taskService.addTask(alarm.getAlarmId(),
					   alarmTask,
					   alarm.getInitialDelay(),
					   alarm.getDelay(),
					   alarm.getTimeUnit()
				);
				addMessage("Alarm added", "id: " + alarm.getAlarmId());

			} else if (alarmType.equals("THREE_BARS")) {

				ThreeBarsTask threeBarsTask = new ThreeBarsTask(alarm);
				
				taskService.addTask(alarm.getAlarmId(),
					   threeBarsTask,
					   alarm.getInitialDelay(),
					   alarm.getDelay(),
					   alarm.getTimeUnit()
				);

				addMessage("Three Bars alarm added", "id: " + alarm.getAlarmId());

			} else if (alarmType.equals("STOCH_RSI")) {

				StochRsiEma stochRsiEma = new StochRsiEma(alarm);
				
				taskService.addTask(alarm.getAlarmId(),
					   stochRsiEma,
					   alarm.getInitialDelay(),
					   alarm.getDelay(),
					   alarm.getTimeUnit()
				);

				addMessage("StochRsiEma alarm added", "id: " + alarm.getAlarmId());
			}
			

		} else {
			if (taskService.getScheduledTasks().containsKey(taskId)) {
				taskService.removeTask(taskId);
				AlarmDB.markMessageUnSent(taskId);
				addMessage("Alarm " + taskId + " removed", "");
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

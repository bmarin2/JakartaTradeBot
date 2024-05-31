package com.tradebot.backingbean;

import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.tradebot.binance.UMFuturesClientConfig;
import com.tradebot.service.futures.FuturesStoBacktest;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import lombok.Data;
import org.json.JSONArray;
import org.json.JSONException;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;

@Named
@ViewScoped
@Data
public class BacktestBean implements Serializable {
	
	private UMFuturesClientImpl umFuturesClientImpl;
	private BarSeries series;
	private List<String> files;

	@PostConstruct
	private void init() {
		umFuturesClientImpl = UMFuturesClientConfig.futuresBaseURLProd();

		series = new BaseBarSeriesBuilder().withName("myTestSeries").build();
		series.setMaximumBarCount(1440);
		
		files = new ArrayList<>();
		files.add(System.getProperty("user.home") + "/output-bars" + "/bars-5-5-2024.json");
		files.add(System.getProperty("user.home") + "/output-bars" + "/bars-6-5-2024.json");
		files.add(System.getProperty("user.home") + "/output-bars" + "/bars-7-5-2024.json");
		files.add(System.getProperty("user.home") + "/output-bars" + "/bars-8-5-2024.json");
		files.add(System.getProperty("user.home") + "/output-bars" + "/bars-9-5-2024.json");
		files.add(System.getProperty("user.home") + "/output-bars" + "/bars-10-5-2024.json");
		files.add(System.getProperty("user.home") + "/output-bars" + "/bars-11-5-2024.json");
		files.add(System.getProperty("user.home") + "/output-bars" + "/bars-12-5-2024.json");		
		files.add(System.getProperty("user.home") + "/output-bars" + "/bars-13-5-2024.json");
		files.add(System.getProperty("user.home") + "/output-bars" + "/bars-14-5-2024.json");
		files.add(System.getProperty("user.home") + "/output-bars" + "/bars-15-5-2024.json");
		files.add(System.getProperty("user.home") + "/output-bars" + "/bars-16-5-2024.json");
		files.add(System.getProperty("user.home") + "/output-bars" + "/bars-17-5-2024.json");
		files.add(System.getProperty("user.home") + "/output-bars" + "/bars-18-5-2024.json");
		files.add(System.getProperty("user.home") + "/output-bars" + "/bars-19-5-2024.json");
	}

	public void startBacktest() {
		System.out.println("Backtest started..\n\n");
		
		fillBarsFromFile(System.getProperty("user.home") + "/output-bars" + "/init-4-5-2024.json");

		FuturesStoBacktest futuresStoBacktest = new FuturesStoBacktest(series);

		for (String file : files) {
			
			JSONArray jsonArray = readJsonFile(file);

			for (int i = 0; i < jsonArray.length(); i++) {
				JSONArray candlestick = jsonArray.getJSONArray(i);

				long unixTimestampMillis = candlestick.getLong(6);
				Instant instant = Instant.ofEpochMilli(unixTimestampMillis);
				ZonedDateTime utcZonedDateTime = instant.atZone(ZoneId.of("UTC"));
				
				Bar bar = new BaseBar(Duration.ofMinutes(1), utcZonedDateTime,
					   candlestick.getString(1),
					   candlestick.getString(2),
					   candlestick.getString(3),
					   candlestick.getString(4),
					   candlestick.getString(5)
				);
				futuresStoBacktest.runner(bar);
			}
		}
		System.out.println("-");
		System.out.println("-");
		System.out.println("Win:   " + futuresStoBacktest.getWin());
		System.out.println("Lose:  " + futuresStoBacktest.getLose() + "\n");
		System.out.println("Total: " + (futuresStoBacktest.getLose() + futuresStoBacktest.getWin()) + "\n");

		double winPercentage = (double) futuresStoBacktest.getWin() 
			   / (futuresStoBacktest.getWin() + futuresStoBacktest.getLose()) * 100;
		
		System.out.println("-");
		System.out.println("Loss sum: " + futuresStoBacktest.getLossSum());
		System.out.println("Gain sum: " + futuresStoBacktest.getGainSum());

		System.out.println("Win percentage: " + winPercentage + " %");
	}

	public void fetchBarSeriesToFile() {
		LocalDateTime targetDateTime1 = LocalDateTime.of(2024, 5, 19, 0, 0); // <-- for this date
		LocalDateTime targetDateTime2 = LocalDateTime.of(2024, 5, 20, 0, 0);

		long startTime = targetDateTime1.toEpochSecond(ZoneOffset.UTC) * 1000;
		long endTime = targetDateTime2.toEpochSecond(ZoneOffset.UTC) * 1000;

		// 1440 bars covers 24h
		fetchBarSeriesToFile("LTCUSDT", "1m", 1440, startTime, endTime, targetDateTime1, "bars"); // bars / init
	}
	
	public void fetchBarSeriesToFile(String symbol, String interval, int limit, long startTime, long endTime, LocalDateTime localDate, String prefix) {
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", symbol);
		parameters.put("interval", interval);
		parameters.put("startTime", startTime);
		parameters.put("endTime", endTime);
		parameters.put("limit", limit);  

		String result = umFuturesClientImpl.market().klines(parameters);
		JSONArray jsonArray = new JSONArray(result);
		jsonArray.remove(jsonArray.length());

		// define home path
		String homeDirectory = System.getProperty("user.home");
		String homePath = homeDirectory + "/" + "output-bars" + "/" + prefix;

		// Save JSON array to a file
		try {
			FileWriter fileWriter = new FileWriter(homePath+"-"+localDate.getDayOfMonth()
					+"-"+localDate.getMonthValue()+"-"+localDate.getYear()+".json");
			fileWriter.write(jsonArray.toString());
			fileWriter.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public void fillBarsFromFile(String filePath) {
		JSONArray jsonArray = readJsonFile(filePath);

		for (int i = 0; i < jsonArray.length(); i++) {
			JSONArray candlestick = jsonArray.getJSONArray(i);

			long unixTimestampMillis = candlestick.getLong(6);
			Instant instant = Instant.ofEpochMilli(unixTimestampMillis);
			ZonedDateTime utcZonedDateTime = instant.atZone(ZoneId.of("UTC"));

			series.addBar(utcZonedDateTime,
				   candlestick.getString(1),
				   candlestick.getString(2),
				   candlestick.getString(3),
				   candlestick.getString(4),
				   candlestick.getString(5)
			);
		}
	}

	private JSONArray readJsonFile(String fileName) {
		JSONArray jsonArray = null;
		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName))) {
			StringBuilder stringBuilder = new StringBuilder();
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line);
			}
			jsonArray = new JSONArray(stringBuilder.toString());
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
		return jsonArray;
	}
}

package com.tradebot.backingbean;

import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.tradebot.binance.UMFuturesClientConfig;
import com.tradebot.data.BarsLTCUSDT;
import com.tradebot.service.futures.FuturesRSI2Strategy;
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
import java.util.LinkedHashMap;
import java.util.List;
import lombok.Data;
import org.json.JSONArray;
import org.json.JSONException;
import org.primefaces.PrimeFaces;
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
	private BarSeries series2;
	private List<String> files;
	private List<String> files2;
	
	// For fetching auto
	private int year = 2024;
	private int month = 4;
	private int startDay = 17;
	private int endDay = 29;
	private String saveLocation = "output-bars/ltc/2024/1h/apr/";
	
	private String pair = "LTCUSDT";
	private String timePeriod = "1h";
	private int numberOfBars = 24; // 1440 1min, 288 5min, 24 1h, 48 30m, 6 4h

	@PostConstruct
	private void init() {
		umFuturesClientImpl = UMFuturesClientConfig.futuresBaseURLProd();

		series = new BaseBarSeriesBuilder().withName("myTestSeries").build();
		series.setMaximumBarCount(288);
		
		series2 = new BaseBarSeriesBuilder().withName("myTestSeries2").build();
		series2.setMaximumBarCount(200);
		
		files = BarsLTCUSDT.getBars_5m_JAN_2024();
		files2 = BarsLTCUSDT.getBars_1h_JAN_2024();
	}
	
	public void fetchManual() {
		LocalDateTime targetDateTime1 = LocalDateTime.of(2024, 4, 30, 0, 0); // <-- for this date
		LocalDateTime targetDateTime2 = LocalDateTime.of(2024, 5, 1, 0, 0);

		long startTime = targetDateTime1.toEpochSecond(ZoneOffset.UTC) * 1000;
		long endTime = targetDateTime2.toEpochSecond(ZoneOffset.UTC) * 1000;

		fetchBarSeriesToFile(pair, timePeriod, numberOfBars, startTime, endTime, targetDateTime1, "bars");
	}

	public void startFetching() {		
		PrimeFaces.current().executeScript("PF('pollw').start()");
	}

	public void startBacktest() {
		System.out.println("Backtest started..\n\n");
//
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/5min/avg/" + "bars-31-8-2024.json", series);

//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/30m/avg/" + "bars-28-8-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/30m/avg/" + "bars-29-8-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/30m/avg/" + "bars-30-8-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/30m/avg/" + "bars-31-8-2024.json", series2);

//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/1h/avg/" + "bars-27-8-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/1h/avg/" + "bars-28-8-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/1h/avg/" + "bars-29-8-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/1h/avg/" + "bars-30-8-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/1h/avg/" + "bars-31-8-2024.json", series2);

//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/5min/sep/" + "bars-30-9-2024.json", series);
//
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/30m/sep/" + "bars-26-9-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/30m/sep/" + "bars-27-9-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/30m/sep/" + "bars-28-9-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/30m/sep/" + "bars-29-9-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/30m/sep/" + "bars-30-9-2024.json", series2);
//
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/1h/sep/" + "bars-26-9-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/1h/sep/" + "bars-27-9-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/1h/sep/" + "bars-28-9-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/1h/sep/" + "bars-29-9-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/1h/sep/" + "bars-30-9-2024.json", series2);

//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/5min/okt/" + "bars-31-10-2024.json", series);
//
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/30m/okt/" + "bars-27-10-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/30m/okt/" + "bars-28-10-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/30m/okt/" + "bars-29-10-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/30m/okt/" + "bars-30-10-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/30m/okt/" + "bars-31-10-2024.json", series2);

//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/1h/okt/" + "bars-27-10-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/1h/okt/" + "bars-28-10-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/1h/okt/" + "bars-29-10-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/1h/okt/" + "bars-30-10-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/btc/2024/1h/okt/" + "bars-31-10-2024.json", series2);

//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/ltc/2024/5min/apr/" + "bars-30-4-2024.json", series);
		
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/ltc/2024/1h/apr/" + "bars-26-4-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/ltc/2024/1h/apr/" + "bars-27-4-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/ltc/2024/1h/apr/" + "bars-28-4-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/ltc/2024/1h/apr/" + "bars-29-4-2024.json", series2);
//		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/ltc/2024/1h/apr/" + "bars-30-4-2024.json", series2);

		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/ltc/2023/5min/dec/" + "bars-31-12-2023.json", series);
		
		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/ltc/2023/1h/dec/" + "bars-27-12-2023.json", series2);
		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/ltc/2023/1h/dec/" + "bars-28-12-2023.json", series2);
		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/ltc/2023/1h/dec/" + "bars-29-12-2023.json", series2);
		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/ltc/2023/1h/dec/" + "bars-30-12-2023.json", series2);
		fillBarsFromFile(System.getProperty("user.home") + "/output-bars/ltc/2023/1h/dec/" + "bars-31-12-2023.json", series2);

//		FuturesRsiTrendStrategy futures = new FuturesRsiTrendStrategy(series, series2);
		FuturesRSI2Strategy futures = new FuturesRSI2Strategy(series, series2);
//		FuturesWilliamsKLStrategy futures = new FuturesWilliamsKLStrategy(series, series2);

		for (int i=0 ; i < files.size() ; i++) {
			
			int counter = 0;
			int counterSeries2 = 0;
			
			JSONArray jsonArray = readJsonFile(files.get(i));
			JSONArray jsonArray2 = readJsonFile(files2.get(i));

			for (int j = 0; j < jsonArray.length(); j++) {
				JSONArray candlestick = jsonArray.getJSONArray(j);
				JSONArray candlestick2 = jsonArray2.getJSONArray(counterSeries2);

				long unixTimestampMillis = candlestick.getLong(6);
				Instant instant = Instant.ofEpochMilli(unixTimestampMillis);
				ZonedDateTime utcZonedDateTime = instant.atZone(ZoneId.of("UTC"));
				
				Bar bar = new BaseBar(Duration.ofMinutes(5), utcZonedDateTime,  // < ******************
					   candlestick.getString(1),
					   candlestick.getString(2),
					   candlestick.getString(3),
					   candlestick.getString(4),
					   candlestick.getString(5)
				);
				
				if (counter == 0) {
					long unixTimestampMillis2 = candlestick2.getLong(6);
					Instant instant2 = Instant.ofEpochMilli(unixTimestampMillis2);
					ZonedDateTime utcZonedDateTime2 = instant2.atZone(ZoneId.of("UTC"));

					Bar bar2 = new BaseBar(Duration.ofHours(1), utcZonedDateTime2,  // < *******************
						   candlestick2.getString(1),
						   candlestick2.getString(2),
						   candlestick2.getString(3),
						   candlestick2.getString(4),
						   candlestick2.getString(5)
					);
					futures.runner2(bar2);
				}
				
				if (counter == 11) { // < ******************
					counter = 0;
					counterSeries2++;
				} else {
					counter++;
				}
				
				futures.runner(bar);
				
			}
		}
		
//		for (int i=0 ; i < files.size() ; i++) {			
//			JSONArray jsonArray = readJsonFile(files.get(i));
//
//			for (int j = 0; j < jsonArray.length(); j++) {
//				JSONArray candlestick = jsonArray.getJSONArray(j);
//
//				long unixTimestampMillis = candlestick.getLong(6);
//				Instant instant = Instant.ofEpochMilli(unixTimestampMillis);
//				ZonedDateTime utcZonedDateTime = instant.atZone(ZoneId.of("UTC"));
//				
//				Bar bar = new BaseBar(Duration.ofHours(1), utcZonedDateTime,
//					   candlestick.getString(1),
//					   candlestick.getString(2),
//					   candlestick.getString(3),
//					   candlestick.getString(4),
//					   candlestick.getString(5)
//				);				
//				futures.runner2(bar);				
//			}
//		}
		

		System.out.println("-");
		System.out.println("Win:   " + futures.getWin());
		System.out.println("Lose:  " + futures.getLose()+ "\n\n");
		System.out.println("diff:  " + (futures.getWin() - futures.getLose()) + "\n");
		double total = futures.getLose() + futures.getWin();
		System.out.println("Total: " + total + "\n");

		double winPercentage = (double) futures.getWin() 
			   / (futures.getWin() + futures.getLose()) * 100;
		
		System.out.println("-");
		System.out.println("Loss sum in $: " + futures.getLossSum());
		System.out.println("Gain sum in $: " + futures.getGainSum());
		
		double profit = futures.getGainSum() - futures.getLossSum();
		double profitWithFees = profit - (total * 0.03);

		System.out.println("Profit: " + profitWithFees + " (including fees $30 risk)");
		System.out.println("Win percentage: " + winPercentage + " %\n");
	}
	
	public void fetchAuto() {
		if(startDay < endDay + 1) {
			fetchBarSeriesToFile(year, month, startDay, startDay + 1);
			startDay++;
		} else {
			System.out.println("stopping..");
			PrimeFaces.current().executeScript("PF('pollw').stop()");
		}
	}

	public void fetchBarSeriesToFile(int year, int month, int startDay, int endDay) {
		LocalDateTime targetDateTime1 = LocalDateTime.of(year, month, startDay, 0, 0); // <-- for this date
		LocalDateTime targetDateTime2 = LocalDateTime.of(year, month, endDay, 0, 0);

		long startTime = targetDateTime1.toEpochSecond(ZoneOffset.UTC) * 1000;
		long endTime = targetDateTime2.toEpochSecond(ZoneOffset.UTC) * 1000;

		fetchBarSeriesToFile(pair, timePeriod, numberOfBars, startTime, endTime, targetDateTime1, "bars");
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
		String homePath = homeDirectory + "/" + saveLocation + prefix;

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

	public void fillBarsFromFile(String filePath, BarSeries s) {
		JSONArray jsonArray = readJsonFile(filePath);

		for (int i = 0; i < jsonArray.length(); i++) {
			JSONArray candlestick = jsonArray.getJSONArray(i);

			long unixTimestampMillis = candlestick.getLong(6);
			Instant instant = Instant.ofEpochMilli(unixTimestampMillis);
			ZonedDateTime utcZonedDateTime = instant.atZone(ZoneId.of("UTC"));

			s.addBar(utcZonedDateTime,
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

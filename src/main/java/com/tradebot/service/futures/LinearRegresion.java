package com.tradebot.service.futures;

import com.tradebot.enums.PositionSide;
import static com.tradebot.enums.PositionSide.LONG;
import static com.tradebot.enums.PositionSide.SHORT;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import lombok.Data;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.DoubleEMAIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.StochasticRSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNum;

@Data
public class LinearRegresion {

	private BarSeries series;
	private Bar currentBar;

	private Queue<Double> linearRegQueue = new LinkedList<>();

	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

	private double currentStopLossPrice;
	private double currentTakeProfitPrice;
	private PositionSide currentPositionSide;
	private final double stopLoss = 2.5;
	private final double takeProfit = 2.5; // consolidation 2.5 5.0
	private double entryPrice;
	private double atr;
	private boolean cross;
	private double K;
	private double D;
	private boolean stoOverbought;
	private boolean stoOversold;

	private int linearRegressionLength = 30;
	private double regAngleLong = 0.3;
	private double regAngleShort = -0.3;

//	private int dema1 = 9;
//	private int dema2 = 21;
	private int dema3 = 100;
//	private double currentDema1;
//	private double currentDema2;
	private double currentDema3;
//	private int ema1 = 9;
//	private int ema2 = 21;
//	private int ema3 = 200;
//	private double currentEma1;
//	private double currentEma2;
//	private double currentEma3;

	private int ma1 = 100;
	private double currentMa1;
	
	private double lossSum;
	private double gainSum;

	private int win;
	private int lose;
	
	private double lastDema;
	
	public LinearRegresion(BarSeries series) {
		this.series = series;
		currentPositionSide = PositionSide.NONE;
		//initFill();
	}

	public void runner(Bar bar) {
		series.addBar(bar);
		currentBar = series.getLastBar();

//		linearRegQueue.poll();
//
//		ClosePriceIndicator closePrices = new ClosePriceIndicator(series);
//		EMAIndicator ema3_tmp = new EMAIndicator(closePrices, ema3);
//		currentEma3 = ema3_tmp.getValue(ema3_tmp.getBarSeries().getEndIndex()).doubleValue();
//		SMAIndicator ma3_tmp = new SMAIndicator(closePrices, ma1);
//		currentMa1 = ma3_tmp.getValue(ma3_tmp.getBarSeries().getEndIndex()).doubleValue();
//		linearRegQueue.offer(currentBar.getClosePrice().doubleValue());
//		linearRegQueue.offer(currentMa1);

//		checkOrders();
		updateValues();
	}

	private void updateValues() {

//		Indicator sr = new StochasticRSIIndicator(series, 50);
//		SMAIndicator k = new SMAIndicator(sr, 3); // blue
//		SMAIndicator d = new SMAIndicator(k, 3); // yellow	
//
//		K = k.getValue(k.getBarSeries().getEndIndex()).doubleValue();
//		D = d.getValue(k.getBarSeries().getEndIndex()).doubleValue();

//		DoubleEMAIndicator dema1_tmp = new DoubleEMAIndicator(closePrices, dema1);
//		currentDema1 = dema1_tmp.getValue(dema1_tmp.getBarSeries().getEndIndex()).doubleValue();
//
//		DoubleEMAIndicator dema2_tmp = new DoubleEMAIndicator(closePrices, dema2);
//		currentDema2 = dema2_tmp.getValue(dema2_tmp.getBarSeries().getEndIndex()).doubleValue();
//		EMAIndicator ema1_tmp = new EMAIndicator(closePrices, ema1);
//		currentEma1 = ema1_tmp.getValue(ema1_tmp.getBarSeries().getEndIndex()).doubleValue();
//		EMAIndicator ema2_tmp = new EMAIndicator(closePrices, ema2);
//		currentEma2 = ema2_tmp.getValue(ema2_tmp.getBarSeries().getEndIndex()).doubleValue();
//		atr = new ATRIndicator(series, 14).getValue(series.getEndIndex()).doubleValue();

		// Regression angle
//		SimpleRegression regression = new SimpleRegression();
//
//		int counter = 0;
//
//		for (Double aDouble : linearRegQueue) {
//			regression.addData(counter, aDouble);
//			counter++;
//		}
//
//		double slope = regression.getSlope();
//		double angle = Math.toDegrees(Math.atan(slope));
		
//		if (angle > 0.5 && currentMarketType != MarketType.UPTREND) {
//			currentMarketType = MarketType.UPTREND;
//			System.out.println("Uptrend detected " + currentBar.getEndTime().format(formatter));
//		} else if (angle < -0.5 && currentMarketType != MarketType.DOWNTREND) {
//			currentMarketType = MarketType.DOWNTREND;
//			System.out.println("Downtrend detected " + currentBar.getEndTime().format(formatter));
//		} else if (angle < 0.5 && angle > -0.5 && currentMarketType != MarketType.CONSOLIDATION) {
//			currentMarketType = MarketType.CONSOLIDATION;
//			System.out.println("Consolidation detected " + currentBar.getEndTime().format(formatter));
//		}
		
//		System.out.println(currentBar.getClosePrice() + " angle " + angle + " " + currentBar.getEndTime().format(formatter));

		//calculateTrendLine();

		// DEMA
//		if (cross && currentEma1 > currentEma2) {
//			cross = false;
//
//			if (currentPositionSide == PositionSide.SHORT) {
//				if (currentBar.getClosePrice().isLessThan(DecimalNum.valueOf(entryPrice))) {
//					double gainS = entryPrice - currentBar.getClosePrice().doubleValue();
//					gainSum = gainSum + gainS;
//					resetValues();
//					win++;
//					System.out.println("WIN            " + currentBar.getEndTime().format(formatter));
//					System.out.println("------------");
//					return;
//				}
//			}
//
//			if (currentPositionSide == PositionSide.NONE && angle > 0.5) { //&& currentDema1 > increased
//				System.out.println("Entering LONG  " + currentBar.getEndTime().format(formatter) + " angle: " + angle);
//				prepareOrder(PositionSide.LONG);
//			}
//
//		} else if (!cross && currentEma1 < currentEma2) {
//			cross = true;
//
//			if (currentPositionSide == PositionSide.LONG) {
//				if (currentBar.getClosePrice().isGreaterThan(DecimalNum.valueOf(entryPrice))) {
//					double gainS2 = currentBar.getClosePrice().doubleValue() - entryPrice;
//					gainSum = gainSum + gainS2;
//					resetValues();
//					win++;
//					System.out.println("WIN            " + currentBar.getEndTime().format(formatter));
//					System.out.println("------------");
//					return;
//				}
//			}
//
//			if (currentPositionSide == PositionSide.NONE && angle < -0.5) { //&& currentDema1 < dencreased				
//				System.out.println("Entering SHORT " + currentBar.getEndTime().format(formatter) + " angle: " + angle);
//				prepareOrder(PositionSide.SHORT);
//			}
//		}

//		if (K > 0.85 && !stoOverbought) {
//			stoOverbought = true;
//		} else if (K < 0.9 && stoOverbought) {
//			stoOverbought = false;
//			if (currentPositionSide == PositionSide.NONE && angle < regAngleShort) { // && (K < 0.6 || D < 0.6)
//				System.out.println("-----");
//				System.out.println("Entering Short " + currentBar.getBeginTime().format(formatter));
//				prepareOrder(PositionSide.SHORT);
//				System.out.println("angle " + angle);
//			}
//		} else if (K < 0.15 && !stoOversold) {
//			stoOversold = true;
//		} else if (K > 0.1 && stoOversold) {
//			stoOversold = false;
//			if (currentPositionSide == PositionSide.NONE && angle > regAngleLong) { // && (K > 0.4 || D > 0.4)
//				System.out.println("-----");
//				System.out.println("Entering LONG " + currentBar.getBeginTime().format(formatter));
//				prepareOrder(PositionSide.LONG);
//				System.out.println("angle " + angle);
//			}
//		}

		


	}

	private void initFill() {
		BarSeries series1 = series.getSubSeries(0, series.getBarCount() - linearRegressionLength);
		BarSeries series2 = series.getSubSeries(series.getBarCount() - linearRegressionLength, series.getBarCount());

		for (int i = 0; i < series2.getBarCount(); i++) {
			Bar bar = series2.getBar(i);
			series1.addBar(bar);
			ClosePriceIndicator closePricesTmp = new ClosePriceIndicator(series1);
//			EMAIndicator ema3_tmp = new EMAIndicator(closePricesTmp, ema3);
			SMAIndicator ma3_tmp = new SMAIndicator(closePricesTmp, ma1);
			//double calculatedEma3 = ema3_tmp.getValue(ema3_tmp.getBarSeries().getEndIndex()).doubleValue();
			double calculatedMa3 = ma3_tmp.getValue(ma3_tmp.getBarSeries().getEndIndex()).doubleValue();
			linearRegQueue.offer(calculatedMa3);
		}

//		for (int i = 0; i < closePricesSub.getBarSeries().getBarCount(); i++) {
//			EMAIndicator ema3_tmp = new EMAIndicator(closePrices, ema3);
//			currentEma3 = ema3_tmp.getValue(ema3_tmp.getBarSeries().getEndIndex()).doubleValue();
//			//linearRegQueue.offer(closePricesSub.getValue(i).doubleValue());
//			linearRegQueue.offer(currentEma3);
//		}
	}

	public void calculateTrendLine() {
		SimpleRegression regression = new SimpleRegression();

		int counter = 0;
		for (Double aDouble : linearRegQueue) {
			regression.addData(counter, aDouble);
			counter++;
		}

		List<Double> trendLine = new ArrayList<>();

		for (int i = 0; i < linearRegQueue.size(); i++) {
			trendLine.add(regression.predict(i));
		}

		System.out.println("first: " + trendLine.get(0) + " last:  " + trendLine.get(trendLine.size() - 1));
		System.out.println("\n----------");

	}

	private void checkOrders() {
		if (currentPositionSide != PositionSide.NONE) {
			switch (currentPositionSide) {
				case LONG:
					if (currentBar.getLowPrice().doubleValue() < currentStopLossPrice) {
						System.out.println("LOSE           " + currentBar.getEndTime().format(formatter));
						System.out.println("------------");
						double lossL = entryPrice - currentStopLossPrice;
						lossSum = lossSum + lossL;
						resetValues();
						lose++;
					} else if (currentBar.getHighPrice().doubleValue() > currentTakeProfitPrice) {
						resetValues();
						win++;
						System.out.println("WIN            " + currentBar.getEndTime().format(formatter));
						System.out.println("------------");
					}
					break;
				case SHORT:
					if (currentBar.getHighPrice().doubleValue() > currentStopLossPrice) {
						System.out.println("LOSE           " + currentBar.getEndTime().format(formatter));
						System.out.println("------------");
						double lossS = currentStopLossPrice - entryPrice;
						lossSum = lossSum + lossS;
						resetValues();
						lose++;
					} else if (currentBar.getLowPrice().doubleValue() < currentTakeProfitPrice) {
						resetValues();
						win++;
						System.out.println("WIN            " + currentBar.getEndTime().format(formatter));
						System.out.println("------------");
					}
					break;
				default:
					break;
			}
		}
	}

	private void prepareOrder(PositionSide positionSide) {
		calculateSL(positionSide);
		calculateTP(positionSide);
		currentPositionSide = positionSide;
	}

	private void calculateSL(PositionSide positionSide) {
		entryPrice = currentBar.getClosePrice().doubleValue();
		Double result = 0.0;
		if (positionSide == PositionSide.SHORT) {
			result = entryPrice + atr * stopLoss;
		} else if (positionSide == PositionSide.LONG) {
			result = entryPrice - atr * stopLoss;
		}
		currentStopLossPrice = result;
	}

	private void calculateTP(PositionSide positionSide) {
		Double price = currentBar.getClosePrice().doubleValue();
		Double result = 0.0;
		if (positionSide == PositionSide.SHORT) {
			result = price - atr * takeProfit;
		} else if (positionSide == PositionSide.LONG) {
			result = price + atr * takeProfit;
		}
		currentTakeProfitPrice = result;
	}

	private void resetValues() {
		currentStopLossPrice = 0.0;
		currentTakeProfitPrice = 0.0;
		currentPositionSide = PositionSide.NONE;
	}
}

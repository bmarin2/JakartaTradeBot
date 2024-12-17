package com.tradebot.service.futures;

import com.tradebot.enums.PositionSide;
import static com.tradebot.enums.PositionSide.LONG;
import static com.tradebot.enums.PositionSide.SHORT;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import lombok.Data;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import java.util.Deque;
import java.util.ArrayDeque;
import org.ta4j.core.indicators.SMAIndicator;

@Data
public class FuturesRSI2StrategyTest {

	private double currentStopLossPrice;
	private double currentTakeProfitPrice;
	private PositionSide currentPositionSide;
//	private final double stopLoss = 2.0;
//	private final double takeProfit = 2.0;

	private BarSeries series;
	private BarSeries series2;

	private Bar currentBar;
	private Bar currentBar2;

	private ClosePriceIndicator closePriceIndicator;
	private ClosePriceIndicator closePriceIndicator2;

	private double atr;

	private int sma1 = 5;
	private int sma2 = 100;
	private double currentSma1;
	private double currentSma2;

	private int win;
	private int lose;

	private double lossSum;
	private double gainSum;

	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

	private double entryPrice;
	
	private double currentAdx;
	private double lastAdx;
	private boolean fallingAdx;
	
	private double currentRsi;
	private double currentRsi2;
	private double currentAw;
	
	Deque<Double> queue = new ArrayDeque<>(20);
	
	DecimalFormat df = new DecimalFormat("0.0000");
	
	private boolean inRange = true;

	public FuturesRSI2StrategyTest(BarSeries series, BarSeries series2) {
		currentPositionSide = PositionSide.NONE;
		this.series = series;
		this.series2 = series2;
	}
	
	public void runner2(Bar bar2) {
		series2.addBar(bar2);
		currentBar2 = series2.getLastBar();
		
		updateValues2();
	}
	
	public void runner(Bar bar) {
		series.addBar(bar);
		currentBar = series.getLastBar();
		
		checkOrders();
		updateValues();
		enterTrade();
	}

	private void checkOrders() {
		if (currentPositionSide != PositionSide.NONE) {
			switch (currentPositionSide) {
				case LONG:
					if (currentBar.getLowPrice().doubleValue() < currentStopLossPrice) {
						System.out.println("LOSE LONG           " + currentBar.getEndTime().format(formatter) + "\n");
						double lossL = entryPrice - currentStopLossPrice;
						double percentageIncrease = (lossL / entryPrice) * 100;
						double temp = (percentageIncrease / 100) * 30;
						lossSum += temp;
						resetValues();
						lose++;
					} 

//					else if (currentBar2.getClosePrice().doubleValue() > currentSma1 && currentBar2.getClosePrice().doubleValue() > entryPrice) {
//						System.out.println("WIN LONG          " + currentBar2.getEndTime().format(formatter));
//						System.out.println("close price: " + currentBar2.getClosePrice().doubleValue() + "\n");
//						double gain = currentBar2.getClosePrice().doubleValue() - entryPrice;
//						double percentageIncrease = (gain / entryPrice) * 100;
//						double temp = (percentageIncrease / 100) * 30;
//						gainSum += temp;
//						resetValues();
//						win++;
//					}
					
					else if (currentBar.getHighPrice().doubleValue() > currentTakeProfitPrice) {
						System.out.println("WIN LONG          " + currentBar.getEndTime().format(formatter) + "\n");
						double gain = currentTakeProfitPrice - entryPrice;
						double percentageIncrease = (gain / entryPrice) * 100;
						double temp = (percentageIncrease / 100) * 30;
						gainSum += temp;
						resetValues();
						win++;
					}
					
					break;
				case SHORT:
					if (currentBar.getHighPrice().doubleValue() > currentStopLossPrice) {
						System.out.println("LOSE SHORT           " + currentBar.getEndTime().format(formatter) + "\n");
						double lossS = currentStopLossPrice - entryPrice;
						double percentageIncrease = (lossS / entryPrice) * 100;
						double temp = (percentageIncrease / 100) * 30;
						lossSum += temp;
						resetValues();
						lose++;
					}

//					else if (currentBar2.getClosePrice().doubleValue() < currentSma1 && currentBar2.getClosePrice().doubleValue() < entryPrice) {
//						System.out.println("WIN SHORT          " + currentBar2.getEndTime().format(formatter));
//						System.out.println("close price: " + currentBar2.getClosePrice().doubleValue() + "\n");
//						double gain = entryPrice - currentBar2.getClosePrice().doubleValue();
//						double percentageIncrease = (gain / entryPrice) * 100;
//						double temp = (percentageIncrease / 100) * 30;
//						gainSum += temp;
//						resetValues();
//						win++;
//					}
					
					else if (currentBar.getLowPrice().doubleValue() < currentTakeProfitPrice) {
						System.out.println("WIN SHORT          " + currentBar.getEndTime().format(formatter) + "\n");
						double gain = entryPrice - currentTakeProfitPrice;
						double percentageIncrease = (gain / entryPrice) * 100;
						double temp = (percentageIncrease / 100) * 30;
						gainSum += temp;
						resetValues();
						win++;
					}
					
					break;
				default:
					break;
			}
		}
	}

	private void updateValues() {
		closePriceIndicator = new ClosePriceIndicator(series);
		atr = new ATRIndicator(series, 14).getValue(series.getEndIndex()).doubleValue();
		
		RSIIndicator rsiIndicator1 = new RSIIndicator(closePriceIndicator, 2);
		currentRsi = rsiIndicator1.getValue(rsiIndicator1.getBarSeries().getEndIndex()).doubleValue();

//		System.out.println(currentBar.getEndTime().format(formatter) + " rsi1: " + currentRsi + " --> rsi2: " + currentRsi2);
		
//		AwesomeOscillatorIndicator aw = new AwesomeOscillatorIndicator(series);
//		currentAw = aw.getValue(aw.getBarSeries().getEndIndex()).doubleValue();

//		ADXIndicator adxIndicator = new ADXIndicator(series2, 14);
//		currentAdx = adxIndicator.getValue(adxIndicator.getBarSeries().getEndIndex()).doubleValue();
//
//		if (currentAdx < lastAdx) {
//			fallingAdx = true;
//		} else if (currentAdx > lastAdx) {
//			fallingAdx = false;
//		}
//
//		lastAdx = currentAdx;
	}
	
	private void updateValues2() {
		closePriceIndicator2 = new ClosePriceIndicator(series2);
		
		RSIIndicator rsiIndicator2 = new RSIIndicator(closePriceIndicator2, 2);
		currentRsi2 = rsiIndicator2.getValue(rsiIndicator2.getBarSeries().getEndIndex()).doubleValue();

		if (queue.size() == 15) {
			queue.removeFirst();
		}

		queue.addLast(currentBar2.getClosePrice().doubleValue());

		SMAIndicator sma20Tmp = new SMAIndicator(closePriceIndicator2, 20);
		double sma20 = sma20Tmp.getValue(sma20Tmp.getBarSeries().getEndIndex()).doubleValue();
		
		double atr2 = new ATRIndicator(series2, 500).getValue(series2.getEndIndex()).doubleValue();
		
		inRange = true;

		if (queue.size() == 15) {

			double upperBorder = (sma20 + atr2) * 1.5;
			double lowerBorder = (sma20 - atr2) * 1.5;

			for (Double value : queue) {
				if (value > upperBorder || value < lowerBorder) {
					inRange = false;
				}
			}

//			if (inRange) {
//				System.out.println("----");
//				System.out.println("Range detected at " + currentBar2.getEndTime().format(formatter));
//				System.out.println("ATR " + atr2);
//				System.out.println("SMA " + sma20);
//			}
		}

	}
	
	private void enterTrade() {
		if (currentPositionSide == PositionSide.NONE && currentRsi2 > 65 && currentRsi < 10 && !inRange) {

			System.out.println("\n");
			System.out.println("Entering LONG " + currentBar.getBeginTime().format(formatter));
			System.out.println("\n");
			
//			calculateSL(PositionSide.LONG, 2.4);
			prepareOrder(PositionSide.LONG, 2.4, 4.1);

		} else if (currentPositionSide == PositionSide.NONE && currentRsi2 < 35 && currentRsi > 90 && !inRange) {

			System.out.println("\n");
			System.out.println("Entering SHORT " + currentBar.getBeginTime().format(formatter));
			System.out.println("\n");
			
//			calculateSL(PositionSide.SHORT, 2.4);
			prepareOrder(PositionSide.SHORT, 2.4, 4.1);
		}
	}

	private void prepareOrder(PositionSide positionSide, double stopLoss, double takeProfit) {
		currentPositionSide = positionSide;
		calculateSL(positionSide, stopLoss);
		calculateTP(positionSide, takeProfit);		
	}

	private void calculateSL(PositionSide positionSide, double stopLoss) {
		entryPrice = currentBar.getClosePrice().doubleValue();
//		currentPositionSide = positionSide;
		Double result = 0.0;
		if (positionSide == PositionSide.SHORT) {
			result = entryPrice + atr * stopLoss;
		} else if (positionSide == PositionSide.LONG) {
			result = entryPrice - atr * stopLoss;
		}
		currentStopLossPrice = result;
	}

	private void calculateTP(PositionSide positionSide, double takeProfit) {
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
		entryPrice = 0.0;
	}	
}

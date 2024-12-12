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

@Data
public class FuturesRsiTrendStrategy {

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

	private int win;
	private int lose;

	private double lossSum;
	private double gainSum;

	private double entryPrice;
	
	private double currentRsi;
	private double currentRsi2;
	private double currentRsiMean;
	
//	private double lastRsi2;
//	private boolean risingRsi2;
	
//	private double currentAdx;
	
	private boolean rsiOverbought;
	private boolean rsiOversold;
	
//	private double currentAdx2;
//	private double lastAdx2;
//	private boolean fallingAdx2;
	
	private boolean meanPosition;
	private boolean canTrade;
	
	DecimalFormat df = new DecimalFormat("0.0000");
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
	
	public enum RsiArea {
		UPPER,
		MIDDLE,
		LOWER,
		INIT
	}
	
	private RsiArea rsiArea;

	public FuturesRsiTrendStrategy(BarSeries series, BarSeries series2) {
		currentPositionSide = PositionSide.NONE;
		rsiArea = RsiArea.INIT;
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
						double lossL = entryPrice - currentStopLossPrice;
						System.out.println("LOSE LONG           " + currentBar.getEndTime().format(formatter) + " ***** loss " + lossL + "\n");						
						double percentageIncrease = (lossL / entryPrice) * 100;
						double temp = (percentageIncrease / 100) * 30;
						lossSum += temp;
						resetValues();
						lose++;
					}
					
//					else if (meanPosition) {						
//						if (currentRsiMean > 65) {
//							System.out.println("WIN LONG          " + currentBar.getEndTime().format(formatter) + "\n");
//							double gain = currentBar.getClosePrice().doubleValue() - entryPrice;
//							double percentageIncrease = (gain / entryPrice) * 100;
//							double temp = (percentageIncrease / 100) * 30;
//							gainSum += temp;
//							resetValues();
//							win++;
//							meanPosition = false;
//						}
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
						double lossS = currentStopLossPrice - entryPrice;
						System.out.println("LOSE SHORT           " + currentBar.getEndTime().format(formatter) + " ***** loss " + lossS + "\n");
						double percentageIncrease = (lossS / entryPrice) * 100;
						double temp = (percentageIncrease / 100) * 30;
						lossSum += temp;
						resetValues();
						lose++;
					}
					
//					else if (meanPosition) {
//						if (currentRsiMean < 35) {
//							System.out.println("WIN SHORT          " + currentBar.getEndTime().format(formatter) + "\n");
//							double gain = entryPrice - currentBar.getClosePrice().doubleValue();
//							double percentageIncrease = (gain / entryPrice) * 100;
//							double temp = (percentageIncrease / 100) * 30;
//							gainSum += temp;
//							resetValues();
//							win++;
//							meanPosition = false;
//						}
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
		
		RSIIndicator rsiIndicatorMean = new RSIIndicator(closePriceIndicator, 14);
		currentRsiMean = rsiIndicatorMean.getValue(rsiIndicatorMean.getBarSeries().getEndIndex()).doubleValue();
		
		RSIIndicator rsiIndicator = new RSIIndicator(closePriceIndicator, 7);
		currentRsi = rsiIndicator.getValue(rsiIndicator.getBarSeries().getEndIndex()).doubleValue();
		
//		if (currentRsi2 > lastRsi2) {
//			risingRsi2 = true;
//		} else if (currentRsi2 < lastRsi2) {
//			risingRsi2 = false;
//		}
//
//		lastRsi2 = currentRsi2;
		
//		ADXIndicator adxIndicator = new ADXIndicator(series2, 7);
//		currentAdx2 = adxIndicator.getValue(adxIndicator.getBarSeries().getEndIndex()).doubleValue();
		
//		if (currentAdx2 < lastAdx2) {
//			fallingAdx2 = true;
//		} else if (currentAdx2 > lastAdx2) {
//			fallingAdx2 = false;
//		}
//
//		lastAdx2 = currentAdx2;

	}
	
	private void updateValues2() {
		closePriceIndicator2 = new ClosePriceIndicator(series2);

		RSIIndicator rsiIndicator2 = new RSIIndicator(closePriceIndicator2, 7);
		currentRsi2 = rsiIndicator2.getValue(rsiIndicator2.getBarSeries().getEndIndex()).doubleValue();
	}
	
	private void enterTrade() {
		
//		if (rsiArea == RsiArea.INIT) {
//			if (currentRsi > 60) {
//				rsiArea = RsiArea.UPPER;
//			} else if (currentRsi < 60 && currentRsi > 40) {
//				rsiArea = RsiArea.MIDDLE;
//			} else if (currentRsi < 40) {
//				rsiArea = RsiArea.LOWER;
//			}
//		}	
//		
//		if (currentPositionSide == PositionSide.NONE && currentRsi2 > 65 ) {
//
//			if (rsiArea == RsiArea.LOWER && currentRsi > 40) {
//				rsiArea = RsiArea.MIDDLE;
//				System.out.println("\n");
//				System.out.println("Entering LONG " + currentBar.getEndTime().format(formatter));
//				prepareOrder(PositionSide.LONG, 2.4, 4.5);
//			} 
//			else if (rsiArea == RsiArea.MIDDLE && currentRsi > 60) {
//				rsiArea = RsiArea.UPPER;
//				System.out.println("\n");
//				System.out.println("Entering LONG " + currentBar.getEndTime().format(formatter));
//				prepareOrder(PositionSide.LONG, 2.4, 4.1);
//			}
//
//		} else if (currentPositionSide == PositionSide.NONE && currentRsi2 < 35 ) {
//
//			if (rsiArea == RsiArea.UPPER && currentRsi < 60) {
//				rsiArea = RsiArea.MIDDLE;
//				System.out.println("\n");
//				System.out.println("Entering SHORT " + currentBar.getEndTime().format(formatter));
//				prepareOrder(PositionSide.SHORT, 2.4, 4.5);
//			} 
//			else if (rsiArea == RsiArea.MIDDLE && currentRsi < 40) {
//				rsiArea = RsiArea.LOWER;
//				System.out.println("\n");
//				System.out.println("Entering SHORT " + currentBar.getEndTime().format(formatter));
//				prepareOrder(PositionSide.SHORT, 2.4, 4.1);
//			}
//		}

//		if (currentRsi > 60) {
//			rsiArea = RsiArea.UPPER;
//		} else if (currentRsi < 60 && currentRsi > 40) {
//			rsiArea = RsiArea.MIDDLE;
//		} else if (currentRsi < 40) {
//			rsiArea = RsiArea.LOWER;
//		}

		
		if (canTrade && currentRsi < 60 && currentRsi > 40) {
			canTrade = false;
		}

		if (currentPositionSide == PositionSide.NONE && currentRsi2 > 65 && !canTrade && currentRsi > 60) {
			canTrade = true;
			System.out.println("\n");
			System.out.println("Entering LONG " + currentBar.getEndTime().format(formatter));
			prepareOrder(PositionSide.LONG, 2.4, 4.1);

		} else if (currentPositionSide == PositionSide.NONE && currentRsi2 < 35 && !canTrade && currentRsi < 40) {
			canTrade = true;
			System.out.println("\n");
			System.out.println("Entering SHORT " + currentBar.getEndTime().format(formatter));
			prepareOrder(PositionSide.SHORT, 2.4, 4.1);
		}


		
//		if (currentPositionSide == PositionSide.NONE && currentRsi2 < 60 && currentRsi2 > 40) {
//			if (currentRsiMean > 70 && !rsiOverbought) {
//				rsiOverbought = true;
//			} else if (currentRsiMean < 70 && rsiOverbought) {
//				rsiOverbought = false;
//				meanPosition = true;
//				System.out.println("\n");
//				System.out.println("MEAN |||||| Entering SHORT " + currentBar.getEndTime().format(formatter));
//				System.out.println("atr " + df.format(atr));
//				prepareOrder(PositionSide.SHORT, 2.2, 2.8);
//			} else if (currentRsiMean < 30 && !rsiOversold) {
//				rsiOversold = true;
//			} else if (currentRsiMean > 30 && rsiOversold) {
//				rsiOversold = false;
//				meanPosition = true;
//				System.out.println("\n");
//				System.out.println("MEAN |||||| Entering LONG " + currentBar.getEndTime().format(formatter));
//				System.out.println("atr " + df.format(atr));
//				prepareOrder(PositionSide.LONG, 2.2, 2.8);
//			}
//		} else {
//			rsiOverbought = false;
//			rsiOversold = false;
//		}
		
	}

	private void prepareOrder(PositionSide positionSide, double stopLoss, double takeProfit) {
		calculateSL(positionSide, stopLoss);
		calculateTP(positionSide, takeProfit);
		currentPositionSide = positionSide;
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
	
	private double calculatePercentage(double val, double price) {
		return (val / price) * 100;
//		return df.format(temp);
	}
	
	private double calculateValue(double percentage, double price) {
		return (percentage / 100) * price;
	}
}

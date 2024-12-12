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
import org.ta4j.core.indicators.WilliamsRIndicator;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.keltner.KeltnerChannelFacade;

@Data
public class FuturesWilliamsKLStrategy {
	private double currentStopLossPrice;
	private double currentTakeProfitPrice;
	private PositionSide currentPositionSide;
		
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
	
	private double currentAdx;
	private double lastAdx;
	private boolean fallingAdx;
	
	private double currentWilliamsR;
	
	private double currentHigherKeltner;
	private double currentLowerKeltner;
	
	private boolean overbought;
	private boolean oversold;
	
	private boolean waitingShort;
	private boolean waitingLong;
	private double edgePrice;
	
	private int maxCount = 6;
	private int counter;
	
	DecimalFormat df = new DecimalFormat("0.0000");
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

	public FuturesWilliamsKLStrategy(BarSeries series, BarSeries series2) {
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
		compareValues();
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
					
					else if (currentWilliamsR > -20) {
						System.out.println("WIN LONG          " + currentBar.getEndTime().format(formatter) + "\n");
						double gain = currentBar2.getClosePrice().doubleValue() - entryPrice;
						double percentageIncrease = (gain / entryPrice) * 100;
						double temp = (percentageIncrease / 100) * 30;
						gainSum += temp;
						resetValues();
						win++;
					}

//					else if (currentBar.getHighPrice().doubleValue() > currentTakeProfitPrice) {
//						System.out.println("WIN LONG          " + currentBar.getEndTime().format(formatter) + "\n");
//						double gain = currentTakeProfitPrice - entryPrice;
//						double percentageIncrease = (gain / entryPrice) * 100;
//						double temp = (percentageIncrease / 100) * 30;
//						gainSum += temp;
//						resetValues();
//						win++;
//					}

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
					
					else if (currentWilliamsR < -80) {
						System.out.println("WIN SHORT          " + currentBar.getEndTime().format(formatter) + "\n");
						double gain = entryPrice - currentBar2.getClosePrice().doubleValue();
						double percentageIncrease = (gain / entryPrice) * 100;
						double temp = (percentageIncrease / 100) * 30;
						gainSum += temp;
						resetValues();
						win++;
					}

//					else if (currentBar.getLowPrice().doubleValue() < currentTakeProfitPrice) {
//						System.out.println("WIN SHORT          " + currentBar.getEndTime().format(formatter) + "\n");
//						double gain = entryPrice - currentTakeProfitPrice;
//						double percentageIncrease = (gain / entryPrice) * 100;
//						double temp = (percentageIncrease / 100) * 30;
//						gainSum += temp;
//						resetValues();
//						win++;
//					}

					break;
				default:
					break;
			}
		}
	}

	private void updateValues() {
		atr = new ATRIndicator(series, 14).getValue(series.getEndIndex()).doubleValue();
		
		KeltnerChannelFacade kl = new KeltnerChannelFacade(series, 20, 10, 2.5);
		currentHigherKeltner = kl.upper().getValue(kl.upper().getBarSeries().getEndIndex()).doubleValue();
		currentLowerKeltner = kl.lower().getValue(kl.lower().getBarSeries().getEndIndex()).doubleValue();
	}
	
	private void updateValues2() {
		WilliamsRIndicator wr = new WilliamsRIndicator(series2, 14);
		currentWilliamsR = wr.getValue(wr.getBarSeries().getEndIndex()).doubleValue();
		
		ADXIndicator adxIndicator = new ADXIndicator(series2, 14);
		currentAdx = adxIndicator.getValue(adxIndicator.getBarSeries().getEndIndex()).doubleValue();

		if (currentAdx < lastAdx) {
			fallingAdx = true;
		} else if (currentAdx > lastAdx) {
			fallingAdx = false;
		}

		lastAdx = currentAdx;
	}
	
	private void compareValues() {
		
		//Long
		if (!waitingLong && currentBar.getLowPrice().doubleValue() < currentLowerKeltner) {
			edgePrice = currentBar.getLowPrice().doubleValue();
			waitingLong = true;
			return;
		}
		
		else if (waitingLong && currentBar.getLowPrice().doubleValue() < edgePrice) {

			if (currentBar.getLowPrice().doubleValue() < currentLowerKeltner) {
				edgePrice = currentBar.getLowPrice().doubleValue();
				counter = 0;
			} else if (counter > maxCount) {
				waitingLong = false;
				counter = 0;
			} else if (counter <= maxCount) {
				counter++;
			}

			return;
		}
		
//		else if (waitingLong && currentBar.getLowPrice().doubleValue() > edgePrice && currentBar.getLowPrice().doubleValue() < currentLowerKeltner) {
//			counter = 0;
//		}
		
		else if (waitingLong && counter > maxCount) {
			waitingLong = false;
			counter = 0;
			return;
		}

		else if (waitingLong && counter <= maxCount) {
			counter++;
			return;
		}

		// Short
		if (!waitingShort && currentBar.getHighPrice().doubleValue() > currentHigherKeltner) {
			edgePrice = currentBar.getHighPrice().doubleValue();
			waitingShort = true;
		}
		
		else if (waitingShort && currentBar.getHighPrice().doubleValue() > edgePrice) {
			
			if (currentBar.getHighPrice().doubleValue() > currentHigherKeltner) {
				edgePrice = currentBar.getHighPrice().doubleValue();
				counter = 0;
			} else if (counter > maxCount) {
				waitingShort = false;
				counter = 0;
			} else if (counter <= maxCount) {
				counter++;
			}
		}
		
//		else if (waitingShort && currentBar.getHighPrice().doubleValue() < edgePrice && currentBar.getHighPrice().doubleValue() > currentHigherKeltner) {
//			counter = 0;
//		}
		
		else if (waitingShort && counter > maxCount) {
			waitingShort = false;
			counter = 0;
		} 
		
		else if (waitingShort && counter <= maxCount) {
			counter++;
		}

	}
	
	private void enterTrade() {

		if (!oversold && currentWilliamsR < -80) {
			oversold = true;
		}
		else if (oversold && currentWilliamsR > -80) {
			if (currentPositionSide == PositionSide.NONE && waitingLong && fallingAdx) {
				prepareOrder(PositionSide.LONG, 2.0, 4.1);
				System.out.println("\n");
				System.out.println("Entering LONG " + currentBar.getEndTime().format(formatter));
				System.out.println("will " + currentWilliamsR);
				System.out.println("\n");
				
				waitingLong = false;
				counter = 0;
			}
			oversold = false;
		}

		if (!overbought && currentWilliamsR > -20) {
			overbought = true;
		}
		else if (overbought && currentWilliamsR < -20) {
			if (currentPositionSide == PositionSide.NONE && waitingShort && fallingAdx) {
				prepareOrder(PositionSide.SHORT, 2.0, 4.1);
				System.out.println("\n");
				System.out.println("Entering SHORT " + currentBar.getEndTime().format(formatter));
				System.out.println("will " + currentWilliamsR);
				System.out.println("\n");
				
				waitingShort = false;
				counter = 0;
			}
			overbought = false;
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

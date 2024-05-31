package com.tradebot.service.futures;

import com.tradebot.enums.PositionSide;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Queue;
import lombok.Data;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.DoubleEMAIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.StochasticRSIIndicator;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.volume.VWAPIndicator;
import org.ta4j.core.num.DecimalNum;

@Data
public class FuturesStoBacktest {
	
	private double currentStopLossPrice;
	private double currentTakeProfitPrice;
	private PositionSide currentPositionSide;
	private final double stopLoss = 2.5;
	private final double takeProfit = 2.5; // consolidation 2.5 5.0
	private double entryPrice;

	private BarSeries series;
	private Bar currentBar;

	private ClosePriceIndicator closePriceIndicator;
	private boolean firstTime = true;
	private boolean firstTimeVwapQueue = true;
	private double K;
	private double D;
	private double atr;

	private Queue<Double> queue;
	private int maxQueueSize = 23; // horizontal box
	private double verticalPercent = 0.1; // ili 0.11 to je vise, uraditi proveru da li cena nastavlja gore ili dole posle gubitka
	private double upperLimit;
	private double lowerLimit;

	private double upperLimitConsolidation;
	private double lowerLimitConsolidation;

	private double minGap = 0.02;

//	private int ema1 = 10;
//	private int ema2 = 20;
	private int ema3 = 150;
//	private double currentEma1;
//	private double currentEma2;
	private double currentEma3;
	
	private int dema1 = 9;
	private int dema2 = 21;
	private int dema3 = 200;
	private double currentDema1;
	private double currentDema2;
	private double currentDema3;

	private boolean cross;

	private boolean inConsolidation;
//	private int consolidationStartLength = 2;
//	private int consolidationCounter;

	private boolean overbought;
	private boolean oversold;
	
	private int rsiCounter;
	private int rsiMaxWait = 11;
	private boolean rsiWaitingShort;
	private boolean rsiWaitingLong;
	
	private int win;
	private int lose;
	
	private double macdLine;
	private double signalLine;

	private int vwapSeriesCounter;

	private boolean rsiOverbought;
	private boolean rsiOversold;

	private boolean closedBelowEma;
	private boolean closedAboveEma;
	
	private boolean emaRising;
	private boolean emaFalling;

	private boolean vwapRising;
	private boolean vwapFalling;
	
	private double currentVwap;
	private double lastVwap;
	private double lastEma;
	private boolean emaDistant;

	private double lossSum;
	private double gainSum;	

	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

	public FuturesStoBacktest(BarSeries series) {
		queue = new LinkedList<>();
		currentPositionSide = PositionSide.NONE;
		this.series = series;
	}

	public void runner(Bar bar) {
		series.addBar(bar);
		currentBar = series.getLastBar();

		if (currentPositionSide != PositionSide.NONE) {			
			switch (currentPositionSide) {
				case LONG:
					if (currentBar.getLowPrice().doubleValue() < currentStopLossPrice) {						
						System.out.println("LOSE           " + currentBar.getEndTime().format(formatter));
						double lossL = entryPrice - currentStopLossPrice;
						lossSum = lossSum + lossL;
						resetValues();
						lose++;
					} 
//					else if (currentBar.getHighPrice().doubleValue() > currentTakeProfitPrice) {
//						resetValues();
//						win++;
//						System.out.println("WIN C          " + currentBar.getEndTime().format(formatter));
//					}
					break;
				case SHORT:
					if (currentBar.getHighPrice().doubleValue() > currentStopLossPrice) {						
						System.out.println("LOSE           " + currentBar.getEndTime().format(formatter));
						double lossS = currentStopLossPrice - entryPrice;
						lossSum = lossSum + lossS;
						resetValues();
						lose++;
					} 
//					else if (currentBar.getLowPrice().doubleValue() < currentTakeProfitPrice) {
//						resetValues();
//						win++;
//						System.out.println("WIN C          " + currentBar.getEndTime().format(formatter));
//					}
					break;
				default:
					break;
			}
		}

		updateValues();
		compareValues();
	}

	private void updateValues() {
		closePriceIndicator = new ClosePriceIndicator(series);
		
//		if (vwapSeriesCounter == 1440) {
//			vwapSeriesCounter = 1;
//		} else {
//			vwapSeriesCounter++;
//		}
//		
//		VWAPIndicator vwap = new VWAPIndicator(series, vwapSeriesCounter);		
//		currentVwap = vwap.getValue(vwap.getBarSeries().getEndIndex()).doubleValue();
		
//		MACDIndicator macdIndicator = new MACDIndicator(closePriceIndicator);
//		macdLine = macdIndicator.getValue(macdIndicator.getBarSeries().getEndIndex()).doubleValue();
//		signalLine = new EMAIndicator(macdIndicator, 9).getValue(macdIndicator.getBarSeries().getEndIndex()).doubleValue();

//		Indicator sr = new StochasticRSIIndicator(series, 14);
//		SMAIndicator k = new SMAIndicator(sr, 3); // blue
//		SMAIndicator d = new SMAIndicator(k, 3); // yellow		
//
//		K = k.getValue(k.getBarSeries().getEndIndex()).doubleValue();
//		D = d.getValue(k.getBarSeries().getEndIndex()).doubleValue();
		
//		EMAIndicator ema1_tmp = new EMAIndicator(closePriceIndicator, ema1);
//		currentEma1 = ema1_tmp.getValue(ema1_tmp.getBarSeries().getEndIndex()).doubleValue();

//		EMAIndicator ema2_tmp = new EMAIndicator(closePriceIndicator, ema2);
//		currentEma2 = ema2_tmp.getValue(ema2_tmp.getBarSeries().getEndIndex()).doubleValue();

		EMAIndicator ema3_tmp = new EMAIndicator(closePriceIndicator, ema3);
		currentEma3 = ema3_tmp.getValue(ema3_tmp.getBarSeries().getEndIndex()).doubleValue();
		
		DoubleEMAIndicator dema1_tmp = new DoubleEMAIndicator(closePriceIndicator, dema1);
		currentDema1 = dema1_tmp.getValue(dema1_tmp.getBarSeries().getEndIndex()).doubleValue();
		
		DoubleEMAIndicator dema2_tmp = new DoubleEMAIndicator(closePriceIndicator, dema2);
		currentDema2 = dema2_tmp.getValue(dema2_tmp.getBarSeries().getEndIndex()).doubleValue();
//		
//		DoubleEMAIndicator dema3_tmp = new DoubleEMAIndicator(closePriceIndicator, dema3);
//		currentDema3 = dema3_tmp.getValue(dema3_tmp.getBarSeries().getEndIndex()).doubleValue();

		atr = new ATRIndicator(series, 14).getValue(series.getEndIndex()).doubleValue();

//		if (firstTime) {
//			int candleCounter = series.getBarCount() - maxQueueSize;
//
//			for (int i = 0; i < maxQueueSize; i++) {
//				ClosePriceIndicator closePricesSub = new ClosePriceIndicator(series.getSubSeries(0, candleCounter));
//				DoubleEMAIndicator emaTmp = new DoubleEMAIndicator(closePricesSub, dema3);
//				queue.offer(emaTmp.getValue(emaTmp.getBarSeries().getEndIndex()).doubleValue());
//				candleCounter++;
//			}
//			firstTime = false;
//		} else {
//			if (queue.size() == maxQueueSize) {
//				queue.poll();
//			}
//			queue.offer(currentDema3);
//		}
//		
//		double oldest = queue.peek();
//
//		double percentValue = oldest * (verticalPercent / 100);
//		upperLimit = oldest + percentValue;
//		lowerLimit = oldest - percentValue;
		
//		double percentValueCon = oldest * (0.08 / 100);
//		upperLimitConsolidation = oldest + percentValueCon;
//		lowerLimitConsolidation = oldest - percentValueCon;

//		if (!inConsolidation && currentEma3 < upperLimit && currentEma3 > lowerLimit) {
//			inConsolidation = true;
//		} else if (inConsolidation && (currentEma3 > upperLimit || currentEma3 < lowerLimit)) {
//			inConsolidation = false;
//		}
		
//		LinkedList<Double> queueList = new LinkedList<>(queue);
//		double midEma = queueList.get(7);

//		if (currentVwap > lastVwap) {
//			vwapRising = true;
//			vwapFalling = false;
//		} else if (currentVwap < lastVwap) {
//			vwapRising = false;
//			vwapFalling = true;
//		}
//		
//		lastVwap = currentVwap;		
//		
//		double percentValue = currentVwap * (0.3 / 100);
//		upperLimit = currentVwap + percentValue;
//		lowerLimit = currentVwap - percentValue;
		

//		if (currentEma3 > lastEma) {
//			emaRising = true;
//			emaFalling = false;
//		} else if (currentEma3 < lastEma) {
//			emaRising = false;
//			emaFalling = true;
//		}
//		lastEma = currentEma3;

//		lastVwap = currenVwap;
	}

	private void compareValues() {

//		RSIIndicator rsiIndicator = new RSIIndicator(closePriceIndicator, 14);
//		double currentRsi = rsiIndicator.getValue(rsiIndicator.getBarSeries().getEndIndex()).doubleValue();

		ADXIndicator adxIndicator = new ADXIndicator(series, 20);
		double currentAdx = adxIndicator.getValue(adxIndicator.getBarSeries().getEndIndex()).doubleValue();
		
//		double percentVwap = currenVwap * (0.25 / 100);
//		double upLimit = currenVwap + percentVwap;
//		double downLimit = currenVwap - percentVwap;
		
//		if (vwapRising && emaRising && (currentEma3 > upLimit)) {
//			rsiOverbought = false;
//			rsiOversold = false;
//
//			if (currentRsi < 50 && !rsiOversoldTrending) {
//				rsiOversoldTrending = true;
//			} else if (currentRsi > 50 && rsiOversoldTrending) {
//				rsiOversoldTrending = false;
//				if (currentPositionSide == PositionSide.NONE) {
//					System.out.println("Entering Long T " + currentBar.getBeginTime());
//					System.out.println("vwap: " + currenVwap + " " + "ema: " + currentEma3);
//					prepareOrder(PositionSide.LONG);
//				}				
//			}
//		} else if (vwapFalling && emapFalling && (currentEma3 < downLimit)) {
//			rsiOverbought = false;
//			rsiOversold = false;
//			
//			if (currentRsi > 50 && !rsiOverboughtTrending) {
//				rsiOverboughtTrending = true;
//			} else if (currentRsi < 50 && rsiOverboughtTrending) {
//				rsiOverboughtTrending = false;
//				if (currentPositionSide == PositionSide.NONE) {
//					System.out.println("Entering SHORT T " + currentBar.getBeginTime());
//					System.out.println("vwap: " + currenVwap + " " + "ema: " + currentEma3);
//					prepareOrder(PositionSide.SHORT);
//				}				
//			}
//		} else if ((currentEma3 < upLimit) && (currentEma3 > downLimit)) {
//			rsiOversoldTrending = false;
//			rsiOverboughtTrending = false;
//			
//			if (vwapRising && emaRising) {
//				if (currentRsi < 30 && !rsiOversold) {
//					rsiOversold = true;
//				} else if (currentRsi > 30 && rsiOversold) {
//					rsiOversold = false;
//					if (currentPositionSide == PositionSide.NONE) {
//						System.out.println("Entering LONG C " + currentBar.getBeginTime());
//						prepareOrder(PositionSide.LONG);
//					}
//				}
//			} else if (vwapFalling && emapFalling) {
//				if (currentRsi > 70 && !rsiOverbought) {
//					rsiOverbought = true;
//				} else if (currentRsi < 70 && rsiOverbought) {
//					rsiOverbought = false;
//					if (currentPositionSide == PositionSide.NONE) {
//						System.out.println("Entering SHORT C " + currentBar.getBeginTime());
//						prepareOrder(PositionSide.SHORT);
//					}
//				}
//			}			
//		}

//		else {
//			if (isTrendingUp) {
//				if (currentRsi < 40 && !crossed50Down) {
//					crossed50Down = true;
//				} else if (currentRsi > 40 && crossed50Down) {
//					prepareOrder(PositionSide.LONG);
//					crossed50Down = false;
//				}
//			} else {
//				if (currentRsi > 60 && !crossed50Up) {
//					crossed50Up = true;
//				} else if (currentRsi < 60 && crossed50Up) {
//					prepareOrder(PositionSide.SHORT);
//					crossed50Up = false;
//				}
//			}
//		}

			
//		if (cross && K > D) {
//			double increasedD = D + minGap;
//			if (K > increasedD) {
//
//				cross = false;
//
//				if (currentPositionSide == PositionSide.NONE && currentAdx > 20 
//					   && currentBar.getClosePrice().doubleValue() > currentEma3) { // && (K < 0.6 || D < 0.6)
//					System.out.println("-----");
//					System.out.println("Entering LONG " + currentBar.getBeginTime().format(formatter));
//					prepareOrder(PositionSide.LONG);
//				}
//			}
//		} else if (!cross && K < D) {
//			double decreasedD = D - minGap;
//			if (K < decreasedD) {
//
//				cross = true;
//				if (currentPositionSide == PositionSide.NONE && currentAdx > 20
//					   && currentBar.getClosePrice().doubleValue() < currentEma3 ) { // && (K > 0.4 || D > 0.4)
//					System.out.println("-----");
//					System.out.println("Entering Short " + currentBar.getBeginTime().format(formatter));
//					prepareOrder(PositionSide.SHORT);
//				}
//			}
//		}

//		double lastClosePrice = currentBar.getClosePrice().doubleValue();

//		if (cross && macdLine > signalLine) {
//
//			double percentageIncrease = (5.0 / 100) * signalLine;
//			double incrisedSignalLine = signalLine + percentageIncrease;
//
//			if (macdLine > incrisedSignalLine) {
//				cross = false;
//
//				if (!inConsolidation && currentPositionSide == PositionSide.NONE && currentEma3 > upperLimit)  {
//					if (macdLine < 0.1 && signalLine < 0.1) {
//						System.out.println("Entering Long " + currentBar.getBeginTime().format(formatter));
//						prepareOrder(PositionSide.LONG);
//					}
//				} 
//				else {
//					if (currentPositionSide == PositionSide.NONE && macdLine < 0.05 && signalLine < 0.05 && currentEma3 > upperLimit) { // && macdLine < 0 && signalLine < 0
//						System.out.println("Entering Long " + currentBar.getBeginTime());
//						prepareOrder(PositionSide.LONG);
//					}
//				}
//			}
//
//		}
//		
//		else if (!cross && macdLine < signalLine) {
//
//			double percentageIncrease = (5.0 / 100) * signalLine;
//			double incrisedSignalLine = signalLine - percentageIncrease;
//
//			if (macdLine < incrisedSignalLine) {
//				cross = true;
//
//				if (!inConsolidation && currentPositionSide == PositionSide.NONE && currentEma3 < lowerLimit) {
//					if (macdLine > -0.1 && signalLine > -0.1) {
//						System.out.println("Entering Short " + currentBar.getBeginTime().format(formatter));
//						prepareOrder(PositionSide.SHORT);
//					}
//				}
//				else {
//					if (currentPositionSide == PositionSide.NONE && macdLine > -0.05 && signalLine > -0.05 && currentEma3 < lowerLimit) { // && macdLine > 0 && signalLine > 0
//						System.out.println("Entering Short " + currentBar.getBeginTime());
//						prepareOrder(PositionSide.SHORT);
//					}
//				}
//			}
//		}

//		double lastClosePrice = currentBar.getClosePrice().doubleValue();
//		double lastOpenPrice = currentBar.getOpenPrice().doubleValue();
//		double lastLowPrice = currentBar.getLowPrice().doubleValue();
//		double lastHighPrice = currentBar.getHighPrice().doubleValue();
		
//		if (currentEma3 < upperLimit && currentEma3 > lowerLimit) {
//			closedBelowEma = false;
//			closedAboveEma = false;
//		}

//		if (currentEma3 < upperLimit && currentEma3 > lowerLimit) {
//			closedBelowEma = false;
//			closedAboveEma = false;
//		}
//
//		if (currentEma3 > upperLimit) {
//			if (lastLowPrice < currentEma2 && !closedBelowEma) {
//				closedBelowEma = true;
//			} else if (lastClosePrice > currentEma2 && closedBelowEma) {
//				closedBelowEma = false;
//				if (currentPositionSide == PositionSide.NONE) {
//					System.out.println("Entering LONG  " + currentBar.getEndTime().format(formatter));
//					prepareOrder(PositionSide.LONG);
//				}
//			}
//		} else if (currentEma3 < lowerLimit) {
//			if (lastHighPrice > currentEma2 && !closedAboveEma) {
//				closedAboveEma = true;
//			} else if (lastClosePrice < currentEma2 && closedAboveEma) {
//				closedAboveEma = false;
//				if (currentPositionSide == PositionSide.NONE) {
//					System.out.println("Entering SHORT " + currentBar.getEndTime().format(formatter));
//					prepareOrder(PositionSide.SHORT);	
//				}
//			}
//		}

//		if (currentEma2 > currentEma3) {
//			closedAboveEma = false;
//			if (lastLowPrice < currentEma2 && !closedBelowEma) {
//				closedBelowEma = true;
//			} else if (lastClosePrice > currentEma2 && closedBelowEma) {
//				closedBelowEma = false;
//				if (currentPositionSide == PositionSide.NONE) {
//					System.out.println("Entering LONG  " + currentBar.getEndTime().format(formatter));
//					prepareOrder(PositionSide.LONG);
//				}
//			}
//		} else if (currentEma2 < currentEma3) {
//			closedBelowEma = false;
//			if (lastHighPrice > currentEma2 && !closedAboveEma) {
//				closedAboveEma = true;
//			} else if (lastClosePrice < currentEma2 && closedAboveEma) {
//				closedAboveEma = false;
//				if (currentPositionSide == PositionSide.NONE) {
//					System.out.println("Entering SHORT " + currentBar.getEndTime().format(formatter));
//					prepareOrder(PositionSide.SHORT);
//				}
//			}
//		}

//		if (rsiWaitingLong) {
//			if (rsiCounter == rsiMaxWait) {
//				rsiWaitingLong = false;
//				rsiCounter = 0;
//			} else {
//				rsiCounter++;
//			}
//		} else if (rsiWaitingShort) {
//			if (rsiCounter == rsiMaxWait) {
//				rsiWaitingShort = false;
//				rsiCounter = 0;
//			} else {
//				rsiCounter++;
//			}
//		}
//
//		if (currentEma3 < upperLimit && currentEma3 > lowerLimit) {
//			if (!rsiWaitingLong && !rsiWaitingShort) {				
//				if (currentRsi > 70 && !overbought) {
//					overbought = true;
//				} else if (currentRsi < 70 && overbought) {
//					overbought = false;
//					rsiWaitingShort = true;
//				} else if (currentRsi < 30 && !oversold) {
//					oversold = true;
//				} else if (currentRsi > 30 && oversold) {
//					oversold = false;
//					rsiWaitingLong = true;
//				}				
//			}
//		}
//
//		if (cross && currentDema1 > currentDema2) {
//			cross = false;
//
//			if (currentPositionSide == PositionSide.NONE) {
//				if (currentEma3 < upperLimit && currentEma3 > lowerLimit) {
//					if (rsiWaitingLong && rsiCounter < rsiMaxWait) {
//						System.out.println("------------");
//						System.out.println("Entering LONG C  " + currentBar.getEndTime().format(formatter));
//						prepareOrder(PositionSide.LONG_CONSOLIDATION);
//						rsiWaitingLong = false;
//						rsiCounter = 0;
//					}
//				} 
//				else if (currentEma3 > upperLimit) { //&& currentDema1 > increased
//					System.out.println("------------");
//					System.out.println("Entering LONG   " + currentBar.getEndTime().format(formatter));
//					System.out.println("gap " + (currentDema1 - currentDema2));
//					prepareOrder(PositionSide.LONG);
//				}
//			} 
//			else if (currentPositionSide == PositionSide.SHORT) {
//				if (currentBar.getClosePrice().isLessThan(DecimalNum.valueOf(entryPrice))) {
//					resetValues();
//					win++;
//					System.out.println("WIN            " + currentBar.getEndTime().format(formatter));
//				}
//			}
//			
//		} else if (!cross && currentDema1 < currentDema2) {
//			cross = true;
//			
//			if (currentPositionSide == PositionSide.NONE) {
//				if (currentEma3 < upperLimit && currentEma3 > lowerLimit) {
//					if (rsiWaitingShort && rsiCounter < rsiMaxWait) {
//						System.out.println("------------");
//						System.out.println("Entering SHORT C " + currentBar.getEndTime().format(formatter));
//						prepareOrder(PositionSide.SHORT_CONSOLIDATION);
//						rsiWaitingShort = false;
//						rsiCounter = 0;
//					}
//				} 
//				else if (currentEma3 < lowerLimit) {
//					System.out.println("------------");
//					System.out.println("Entering SHORT " + currentBar.getEndTime().format(formatter));
//					System.out.println("gap " + (currentDema2 - currentDema1));
//					prepareOrder(PositionSide.SHORT);
//				}
//			} 
//			else if (currentPositionSide == PositionSide.LONG) {
//					if (currentBar.getClosePrice().isGreaterThan(DecimalNum.valueOf(entryPrice))) {
//						resetValues();
//						win++;
//						System.out.println("WIN            " + currentBar.getEndTime().format(formatter));
//				}
//			}
//
//		}

		// DEMA
		if (cross && currentDema1 > currentDema2) {
			cross = false;
			
			if (currentPositionSide == PositionSide.SHORT) {
				if (currentBar.getClosePrice().isLessThan(DecimalNum.valueOf(entryPrice))) {
					double gainS = entryPrice - currentBar.getClosePrice().doubleValue();
					gainSum = gainSum + gainS;
					resetValues();
					win++;
					System.out.println("WIN            " + currentBar.getEndTime().format(formatter));
					return;
				}
			}

			if (currentPositionSide == PositionSide.NONE && currentBar.getClosePrice().doubleValue() > currentEma3
				   && currentAdx > 30) { //&& currentDema1 > increased
					System.out.println("------------");
					System.out.println("Entering LONG  " + currentBar.getEndTime().format(formatter));
					System.out.println("currentDema1 " + currentDema1);
					System.out.println("currentDema2 " + currentDema2);
					prepareOrder(PositionSide.LONG);
			}

		}
		else if (!cross && currentDema1 < currentDema2) {
			cross = true;
			
			if (currentPositionSide == PositionSide.LONG) {
				if (currentBar.getClosePrice().isGreaterThan(DecimalNum.valueOf(entryPrice))) {
					double gainS2 = currentBar.getClosePrice().doubleValue() - entryPrice;
					gainSum = gainSum + gainS2;
					resetValues();
					win++;
					System.out.println("WIN            " + currentBar.getEndTime().format(formatter));
					return;
				}
			}
			

			if (currentPositionSide == PositionSide.NONE && currentBar.getClosePrice().doubleValue() < currentEma3
				   && currentAdx > 30) { //&& currentDema1 < dencreased
					System.out.println("------------");
					System.out.println("Entering SHORT " + currentBar.getEndTime().format(formatter));
					System.out.println("currentDema1 " + currentDema1);
					System.out.println("currentDema2 " + currentDema2);
					prepareOrder(PositionSide.SHORT);
			}
		}



	}

//	private boolean emasSetForLong() {
//		return currentEma1 > currentEma2
//			  && currentEma2 > currentEma3;
//	}
//
//	private boolean emasSetForShort() {
//		return currentEma1 < currentEma2
//			  && currentEma2 < currentEma3;
//	}
	
	private void prepareOrder(PositionSide positionSide) {
		calculateSL(positionSide);
		//calculateTP(positionSide);
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
	
	private double calcPercent(double newPrice, double oldPrice) {
		return (newPrice - oldPrice) / oldPrice * 100;
	}
}

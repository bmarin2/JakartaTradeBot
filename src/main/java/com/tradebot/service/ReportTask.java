package com.tradebot.service;

import com.tradebot.db.OrderDB;
import com.tradebot.db.TradeBotDB;
import com.tradebot.model.OrderTracker;
import com.tradebot.model.TradeBot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportTask implements Runnable {
    
    private final TelegramBot telegramBot;

    public ReportTask() {
        this.telegramBot = new TelegramBot();
    }

    @Override
    public void run() {
        
        try {
            List<OrderTracker> orders = OrderDB.getOrders24Hours();
            
            StringBuilder sb = new StringBuilder();
            
            Map<Long, List<OrderTracker>> ordersByTradeBot = orders.stream()
                    .collect(Collectors.groupingBy(OrderTracker::getTradebot_id));
            
            for (Map.Entry<Long, List<OrderTracker>> entry : ordersByTradeBot.entrySet()) {

                TradeBot bot = TradeBotDB.getOneTradeBot(entry.getKey());
                sb.append(bot.getSymbol()).append(" ").append(bot.getTaskId()).append("\n");
                Map<Boolean, List<OrderTracker>> ordersBySells = entry.getValue().stream()
                    .collect(Collectors.groupingBy(OrderTracker::getSell));            
                for (Map.Entry<Boolean, List<OrderTracker>> entrySells : ordersBySells.entrySet()) {
                    if(!entrySells.getKey()) {
                        sb.append("Buys ").append(entrySells.getValue().size()).append("\n");
                    } else {
                        BigDecimal profits = BigDecimal.ZERO;
                        sb.append("Sells ").append(entrySells.getValue().size()).append("\n");
                        for (OrderTracker orderTracker : entrySells.getValue()) {
                            profits = profits.add(orderTracker.getProfit());
                        }
                        sb.append("Profits ").append(profits.toString()).append("\n");
                    }
                }
                sb.append("\n");
            }
            
            telegramBot.sendMessage(sb.toString());
        }catch(Exception ex) {
            ex.printStackTrace();
        }
    }    
}

package com.tradebot.backingbean;

import com.tradebot.db.TradeBotDB;
import com.tradebot.model.TradeBot;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import lombok.Data;

@ViewScoped
@Named
@Data
public class HelloBean implements Serializable {

//    @Inject
//    private GreetingService greetingService;

    private String name;
    private String greeting;

//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public String getGreeting() {
//        return greeting;
//    }
    
    public String some() {
	    return "some text";
    }
    
    public void samp() {
	    TradeBot bot = new TradeBot();
	    bot.setSymbol("MATIC");
	    try {
		   TradeBotDB.addDept(bot);
	    } catch (Exception e) {
		    e.printStackTrace();
	    }
	    
	    
    }

//    public void doGreeting() {
//        greeting = String.format(greetingService.getGreetingTemplate(null), name);
//    }
}

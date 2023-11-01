package com.tradebot.model;

import java.io.Serializable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FuturesAccountBalance implements Serializable {
    private String accountAlias;
    private String asset;
    private String balance;
    private String crossWalletBalance;
    private String crossUnPnl;
    private String availableBalance;
    private String maxWithdrawAmount;
    private boolean marginAvailable;
    private long updateTime;
}
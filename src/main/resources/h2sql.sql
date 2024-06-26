TRUNCATE TABLE ORDER_TRACKER RESTART IDENTITY;
TRUNCATE TABLE ERROR_TRACKER RESTART IDENTITY;

ALTER SEQUENCE ORDER_TRACKER RESTART WITH 1;

DELETE FROM ORDER_TRACKER WHERE tradebot_id=x;

DROP TABLE ORDER_TRACKER;

jdbc:h2:~/botdb;AUTO_SERVER=TRUE

--Add column
ALTER TABLE TRADE_BOT ADD COLUMN stopLoss DOUBLE;
ALTER TABLE TRADE_BOT ADD COLUMN stopLossWarning DOUBLE;
ALTER TABLE TRADE_BOT ADD COLUMN enableStopLoss BOOLEAN;
ALTER TABLE TRADE_BOT ADD COLUMN profitBase BOOLEAN;
ALTER TABLE TRADE_BOT ADD COLUMN priceGridLimit DOUBLE;

-- Remove column
ALTER TABLE ORDER_TRACKER DROP COLUMN BUY;
ALTER TABLE ORDER_TRACKER ADD COLUMN stopLossPrice DECIMAL(14,8);
ALTER TABLE ORDER_TRACKER ADD COLUMN stopLossPriceWarning DECIMAL(14,8);

CREATE TABLE TRADE_BOT (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(255),
    createdDate DATE,
    taskId VARCHAR(255),
    quoteOrderQty INT,
    cycleMaxOrders INT,
    orderStep DOUBLE,
    description VARCHAR(255),
    initialDelay INT,
    delay INT,
    timeUnit INT,
    stopLoss DOUBLE,
    demaAlertTaskId VARCHAR(255),
    enableStopLoss BOOLEAN,
    profitBase BOOLEAN,
    priceGridLimit DOUBLE
);

CREATE TABLE FUTURES_BOT (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(255),
    createdDate DATE,
    taskId VARCHAR(255),
    quantity DOUBLE,
    description VARCHAR(255),
    initialDelay INT,
    delay INT,
    timeUnit INT,
    stopLoss DOUBLE,
    takeProfit DOUBLE,
    demaAlertTaskId VARCHAR(255),
    futresDemaStrategy INT,
    chartMode INT,
    intervall VARCHAR(255)
);


CREATE TABLE ORDER_TRACKER (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sell BOOLEAN,
    buyPrice DECIMAL(14,8),
    sellPrice DECIMAL(14,8),
    profit DECIMAL(14,8),
    buyDate TIMESTAMP,
    sellDate TIMESTAMP,
    buyOrderId BIGINT,
    sellOrderId BIGINT,
    tradebot_id BIGINT,
    FOREIGN KEY (tradebot_id) REFERENCES TRADE_BOT(id),
    stopLossPrice DECIMAL(14,8)
);

CREATE TABLE ERROR_TRACKER (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    errorTimestamp TIMESTAMP,
    errorMessage VARCHAR(255),
    acknowledged BOOLEAN,
    tradebot_id BIGINT,
    FOREIGN KEY (tradebot_id) REFERENCES TRADE_BOT(id)
);

CREATE TABLE ALARM (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(255),
    alarmId VARCHAR(255),
    alarmPrice DECIMAL(14,8),
    initialDelay INT,
    delay INT,
    timeUnit INT,
    description VARCHAR(255),
    msgSent BOOLEAN,
    intervall VARCHAR(255),
    firstDema INT,
    secondDema INT,
    thirdDema INT,
    crosss BOOLEAN,
    currentFirstDema DOUBLE,
    currentSecondDema DOUBLE,
    currentThirdDema DOUBLE,
    crosssBig BOOLEAN,
    lastClosingCandle DOUBLE,
    minGap DOUBLE,
    chartMode INT,
    emaCrossStrategy INT,
    alarmType INT,
    enterLong BOOLEAN,
    enterShort BOOLEAN,
    goodForEntry BOOLEAN,
    atr DOUBLE
);

CREATE TABLE MACD_ALARM (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(255),
    alarmId VARCHAR(255),
    initialDelay INT,
    delay INT,
    timeUnit INT,
    description VARCHAR(255),
    intervall VARCHAR(255),
    ema INT,
    currentEMA DOUBLE,
    macdCrosss BOOLEAN,
    goodForEntry BOOLEAN,
    currentMacdLine DOUBLE,
    currentSignalLine DOUBLE,
    lastClosingCandle DOUBLE,
    minGap DOUBLE,
    chartMode INT,
    lastAtr DOUBLE
);

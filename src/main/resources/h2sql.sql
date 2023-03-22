TRUNCATE TABLE TRADE_BOT RESTART IDENTITY;
ALTER SEQUENCE my_table_id_seq RESTART WITH 1;

DROP TABLE ORDER_TRACKER;


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
    timeUnit INT
);


CREATE TABLE ORDER_TRACKER (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    buy BOOLEAN,
    sell BOOLEAN,
    buyPrice DECIMAL(14,8),
    sellPrice DECIMAL(14,8),
    profit DECIMAL(14,8),
    buyDate TIMESTAMP,
    sellDate TIMESTAMP,
    buyOrderId BIGINT,
    sellOrderId BIGINT,
    tradebot_id BIGINT,
    FOREIGN KEY (tradebot_id) REFERENCES TRADE_BOT(id)
);

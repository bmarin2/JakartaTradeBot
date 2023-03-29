package com.tradebot.util;

import java.util.Random;

public class TaskCodeGeneratorService {
    private static final String CHARACTERS = "abcdefghijklmnpqrstuvwxyz0123456789ABCDEFGHIJKLMNPQRSTUVWXYZ";
    private static final int LENGTH = 9;
    
    public static String generateRandomString() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            char randomChar = CHARACTERS.charAt(index);
            sb.append(randomChar);
        }
        return sb.toString();
    }
}

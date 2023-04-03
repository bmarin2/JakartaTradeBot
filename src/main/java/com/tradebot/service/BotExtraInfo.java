package com.tradebot.service;

import com.tradebot.model.BotDTO;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BotExtraInfo {
	private static final Map<String, BotDTO> map = new ConcurrentHashMap<>();

	public static BotDTO getInfo(String taskId) {
		return map.get(taskId);
	}

	public static void putInfo(String taskId, BotDTO botDTO) {
		map.put(taskId, botDTO);
	}
	
	public static boolean containsInfo(String taskId) {
		return map.containsKey(taskId);
	}

	public static Map<String, BotDTO> getMap() {
		return map;
	}	
}

package com.tradebot.model;

import java.io.Serializable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserAccount implements Serializable {
	private long id;
	private String username;
	private String password;
}

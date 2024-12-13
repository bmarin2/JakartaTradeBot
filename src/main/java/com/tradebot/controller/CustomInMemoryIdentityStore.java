package com.tradebot.controller;

import com.tradebot.db.UserAccountDB;
import com.tradebot.model.UserAccount;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;

@ApplicationScoped
public class CustomInMemoryIdentityStore implements IdentityStore {

	@Override
	public CredentialValidationResult validate(Credential credential) {

		UsernamePasswordCredential login = (UsernamePasswordCredential) credential;

		UserAccount userAccount = null;

		try {
			userAccount = UserAccountDB.getOneUserAccount(login.getCaller());
		} catch (Exception ex) {
			ex.printStackTrace();
			return CredentialValidationResult.INVALID_RESULT;		
		}
		
		if (userAccount != null) {
			Argon2 argon2 = Argon2Factory.create();
			if (argon2.verify(userAccount.getPassword(), login.getPasswordAsString().toCharArray())){
				return new CredentialValidationResult(userAccount.getUsername());
			}
		}
		return CredentialValidationResult.INVALID_RESULT;
	}
}

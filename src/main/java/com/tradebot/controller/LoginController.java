/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tradebot.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.annotation.FacesConfig;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.SecurityContext;
import jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import jakarta.security.enterprise.authentication.mechanism.http.CustomFormAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.LoginToContinue;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import lombok.Data;


@CustomFormAuthenticationMechanismDefinition(
	   loginToContinue = @LoginToContinue(
			 loginPage = "/login.xhtml",
			 useForwardToLogin = false
	   )
)
@Named
@Data
@RequestScoped
@FacesConfig
public class LoginController implements Serializable {
	
	private String username;
	private String password;

	@Inject
	private SecurityContext securityContext;

	@Inject
	private ExternalContext externalContext;

	@Inject
	private FacesContext facesContext;
	
	public void authenticate() throws IOException {
		AuthenticationStatus status = securityContext.authenticate(
			   (HttpServletRequest) externalContext.getRequest(),
			   (HttpServletResponse) externalContext.getResponse(),
			   AuthenticationParameters.withParams()
					 .credential(new UsernamePasswordCredential(username, password))
		);
		
		if (status == AuthenticationStatus.SUCCESS) {
			externalContext.redirect(externalContext.getRequestContextPath() + "/index.xhtml");
		}
	}
}

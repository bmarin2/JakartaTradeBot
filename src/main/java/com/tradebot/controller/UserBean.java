package com.tradebot.controller;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;

@Named
@SessionScoped
public class UserBean implements Serializable {

	public String getPrincipalName() {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

		if (request.getUserPrincipal() != null) {
			return request.getUserPrincipal().getName();
		}
		return "Guest";
	}
	
	public void logout() throws IOException {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}

		context.getExternalContext().redirect("login.xhtml");
	}

}

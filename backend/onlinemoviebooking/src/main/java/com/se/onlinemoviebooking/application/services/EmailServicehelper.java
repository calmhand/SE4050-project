package com.se.onlinemoviebooking.application.services;

import com.se.onlinemoviebooking.application.cache.SimpleCache;
import com.se.onlinemoviebooking.application.dto.UserDTO;
import com.se.onlinemoviebooking.application.utilities.ApplicationUtilities;

public class EmailServicehelper {
	
	
	//EMC_userid , RPW_userid
	public static boolean sendRegisterEmailConfirmation(UserDTO user) {
		String sub ="Email Confirmation";
		String body = "Dear [[name]],\\r\\n"+ 
		"Please click the link below to verify your registration:\\r\\n"+ 
				"\"[[URL]]\""+ "Thank you,\\r\\n"+ "b3onlinemoviebooking";
		
		String verifykey = "EMC_"+user.getUserID();
		String verifyURL = "http://localhost:8084/api/onlinemoviebooking/"
				+user.getUserID() + "/emailVerify?code=" +ApplicationUtilities.generateTokenAndUpdateCache(verifykey);
		
		body = body.replace("[[name]]", user.getLastName());
		body = body.replace("[[URL]]",verifyURL);
		
		System.out.println(SimpleCache.getInstance().getCacheMap());
		
		EmailService.getInstance().sendEmail(sub, body, user.getEmail());
		
		return true;
	}
	
	public static boolean sendPasswordResetCode(UserDTO user) {
		String sub ="Email Confirmation";
		
		
		String verifykey = "RPW_"+user.getUserID();
		
		String code = ApplicationUtilities.generateTokenAndUpdateCache(verifykey);
		
		String body = "Dear [[name]],<br>"+ 
		"Please use the below code for reset password<br>"+ 
		code+ "<br>Thank you,<br>"+ "b3onlinemoviebooking";
		
		body = body.replace("[[name]]", user.getLastName());
		
		EmailService.getInstance().sendEmail(sub, body, user.getEmail());
		
		return true;
	}

}
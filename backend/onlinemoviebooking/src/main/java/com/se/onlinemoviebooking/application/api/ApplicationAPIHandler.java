package com.se.onlinemoviebooking.application.api;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.se.onlinemoviebooking.application.cache.SimpleCache;
import com.se.onlinemoviebooking.application.dao.PromotionDAO;
import com.se.onlinemoviebooking.application.dao.SeatBookingDAO;
import com.se.onlinemoviebooking.application.database.service.BookingService;
import com.se.onlinemoviebooking.application.database.service.DefaultPaymentCardService;
import com.se.onlinemoviebooking.application.database.service.DefaultShowTimeService;
import com.se.onlinemoviebooking.application.database.service.MovieService;
import com.se.onlinemoviebooking.application.database.service.PromotionService;
import com.se.onlinemoviebooking.application.database.service.SeatBookingService;
import com.se.onlinemoviebooking.application.database.service.ShowTimeService;
import com.se.onlinemoviebooking.application.database.service.TransactionService;
import com.se.onlinemoviebooking.application.database.service.UserService;
import com.se.onlinemoviebooking.application.dto.BookingDTO;
import com.se.onlinemoviebooking.application.dto.ConfirmBookingDTO;
import com.se.onlinemoviebooking.application.dto.PaymentcardDTO;
import com.se.onlinemoviebooking.application.dto.SeatBookingDTO;
import com.se.onlinemoviebooking.application.dto.ShowTimeDTO;
import com.se.onlinemoviebooking.application.dto.Status;
import com.se.onlinemoviebooking.application.dto.TicketDTO;
import com.se.onlinemoviebooking.application.dto.TransactionDTO;
import com.se.onlinemoviebooking.application.dto.TransactionDetails;
import com.se.onlinemoviebooking.application.dto.TransactionType;
import com.se.onlinemoviebooking.application.dto.UserDTO;
import com.se.onlinemoviebooking.application.dto.ValidateBookingDTO;
import com.se.onlinemoviebooking.application.services.EmailServicehelper;
import com.se.onlinemoviebooking.application.utilities.ApplicationStringConstants;

public class ApplicationAPIHandler {

	/* Registration */

	public static JSONObject registerUser(UserService userService, JSONObject payload, PasswordEncoder encoder) {
		// to-do verify details and save
		UserDTO userDTO = UserDTO.getObject(payload);
		userDTO.setPassword(encoder.encode(userDTO.getPassword()));
		userDTO.setStatus(Status.INACTIVE);
		UserDTO saveduser = userService.saveUser(userDTO);

		EmailServicehelper.sendRegisterEmailConfirmation(saveduser);

		JSONParser parser = new JSONParser();
		JSONObject json;
		try {
			json = (JSONObject) parser.parse(saveduser.toJSONString());
		} catch (ParseException e) {
			json = new JSONObject();
			json.put(ApplicationStringConstants.ERROR, ApplicationStringConstants.SOMETHINGWENTWRONG);
			return failureResponse(json);
		}
		return successResponse(json);
	}

	public static JSONObject getUserProfile(Integer userID, UserService userService) {

		UserDTO user = userService.getUserDTObyId(userID);
		user.setPassword("");
		JSONParser parser = new JSONParser();
		JSONObject json;
		try {
			json = (JSONObject) parser.parse(user.toJSONString());
		} catch (ParseException e) {
			json = new JSONObject();
			json.put(ApplicationStringConstants.ERROR, ApplicationStringConstants.SOMETHINGWENTWRONG);
			return failureResponse(json);
		}
		return successResponse(json);
	}

	public static JSONObject updateUserProfile(Integer userID, UserService userService, UserDTO payload) {

		UserDTO updated = userService.updateUserDTObyId(userID, payload);

		JSONObject json = new JSONObject();
		if (updated.getUserID() != null) {
			JSONParser parser = new JSONParser();
			try {
				json = (JSONObject) parser.parse(updated.toJSONString());
			} catch (ParseException e) {
				json.put(ApplicationStringConstants.ERROR, ApplicationStringConstants.SOMETHINGWENTWRONG);
				return failureResponse(json);
			}
			EmailServicehelper.sendAccountUdatedEmail(updated);
			return successResponse(json);
		}

		return failureResponse(json);
	}

	public static JSONObject updateUserPassword(Integer userID, UserService userService, JSONObject payload) {

		int rec = userService.updateUserPassword(userID, payload);
		if (rec > 0) {
			return successResponse(new JSONObject());
		}

		return failureResponse(new JSONObject());
	}

	public static JSONObject forgotPassword(UserService userService, JSONObject payload) {

		if (payload.get("email") == null || ((String) payload.get("email")).isEmpty()) {
			return failureResponse(new JSONObject());
		}

		UserDTO user = userService.getUserDTObyEmail((String) payload.get("email"));
		if (user == null) {
			return failureResponse(new JSONObject());
		}

		EmailServicehelper.sendPasswordResetCode(user);

		JSONObject resp = new JSONObject();
		resp.put("userID", user.getUserID());
		return successResponse(resp);
	}

	public static JSONObject emailResetPassword(UserService userService, JSONObject payload) {
		int up = userService.resetUserPassword(payload);
		if (up > 0) {
			return successResponse(new JSONObject());
		}
		return failureResponse(new JSONObject());
	}

	public static JSONObject verifyEmail(Integer userID, UserService userService, String code) {
		System.out.println(SimpleCache.getInstance().getCacheMap());
		String key = "EMC_" + userID;
		String val = SimpleCache.getInstance().get(key);
		if (val != null && val.equals(code)) {
			userService.updateUserStatus(userID, Status.ACTIVE);
			return successResponse(new JSONObject());
		}

		JSONObject json = new JSONObject();
		json.put(ApplicationStringConstants.ERROR, ApplicationStringConstants.VERIFICATIONFAILED);
		return failureResponse(json);
	}

	public static JSONObject addUserPayment(Integer userID, DefaultPaymentCardService paymentCardService,
			JSONObject payload) {
		PaymentcardDTO paymentcard = PaymentcardDTO.getObject(payload);

		PaymentcardDTO savedCard = paymentCardService.savePaymentCard(paymentcard);

		JSONParser parser = new JSONParser();
		JSONObject json;
		try {
			json = (JSONObject) parser.parse(savedCard.toJSONString());
		} catch (ParseException e) {
			json = new JSONObject();
			json.put(ApplicationStringConstants.ERROR, ApplicationStringConstants.SOMETHINGWENTWRONG);
			return failureResponse(json);
		}
		return successResponse(json);

	}

	public static List<PaymentcardDTO> getUserPayments(Integer userid, DefaultPaymentCardService paymentCardService) {

		return paymentCardService.getPaymentCards(userid);
	}

	public static JSONObject deleteUserPayment(Integer userid, DefaultPaymentCardService paymentCardService,
			PaymentcardDTO payload) {

		boolean del = paymentCardService.deletePaymentCard(payload.getCardID());
		if (del) {
			return successResponse(new JSONObject());
		}
		return failureResponse(new JSONObject());
	}

	public static JSONObject editUserPayment(Integer userid, DefaultPaymentCardService paymentCardService,
			PaymentcardDTO payload) {
		

		PaymentcardDTO pcd = paymentCardService.updatePaymentCard(payload, payload.getCardID());

		JSONObject json = new JSONObject();

		if (pcd.getCardID() == null) {
			return failureResponse(json);
		}

		JSONParser parser = new JSONParser();
		try {
			json = (JSONObject) parser.parse(pcd.toJSONString());
		} catch (ParseException e) {
			json.put(ApplicationStringConstants.ERROR, ApplicationStringConstants.SOMETHINGWENTWRONG);
			return failureResponse(json);
		}
		return successResponse(json);
	}
	
	
	
	public static JSONObject getHomePageData(MovieService movieService) {
		JSONObject json = movieService.getHomePageMovies();
		return successResponse(json);
	}
	
	public static JSONObject getMatchedMoviesByname(MovieService movieService, String name) {
		JSONObject json = new JSONObject();
		json.put("movies", movieService.getMatchedMovies(name));
		return successResponse(json);
	}
	
	public static JSONObject getMoviesByGenre(MovieService movieService, String genre) {
		JSONObject json = new JSONObject();
		json.put("movies", movieService.getMoviesByGenre(genre));
		return successResponse(json);
	}
	
	public static JSONObject getMatchedMoviesBynameAndGenre(MovieService movieService, String name, String genre) {
		JSONObject json = new JSONObject();
		json.put("movies", movieService.getMatchedMoviesByGenre(name, genre));
		return successResponse(json);
	}
	
	public static JSONObject getShowByID(ShowTimeService showTimeService, Long showid) {
		JSONObject response = showTimeService.getShowTimeById(showid);
		return successResponse(response);
	}
	
	public static JSONObject getShowSeatDetails(SeatBookingService sbService, Long showid) {
		JSONObject json = sbService.getShowSeatDetails(showid);
		if(json ==null) {
			json = new JSONObject();
			json.put(ApplicationStringConstants.ERROR, ApplicationStringConstants.SOMETHINGWENTWRONG);
			return failureResponse(json);
		}
		return successResponse(json);
	}

	public static JSONObject getPromotionByCode(PromotionService pService, String code) {
		PromotionDAO promotion = pService.getPromotionByCode(code.toUpperCase());
		
		JSONParser parser = new JSONParser();
		JSONObject json;
		if(promotion==null) {
			json = new JSONObject();
			json.put(ApplicationStringConstants.ERROR, ApplicationStringConstants.PROMOTIONNOTAVAILABLE);
			return failureResponse(json);
		}
		try {
			json = (JSONObject) parser.parse(promotion.toJSONString());
		} catch (ParseException e) {
			json = new JSONObject();
			json.put(ApplicationStringConstants.ERROR, ApplicationStringConstants.PROMOTIONNOTAVAILABLE);
			return failureResponse(json);
		}
		return successResponse(json);
	}
	
	public static JSONObject validateBooking(ShowTimeService sService,SeatBookingService sb, ValidateBookingDTO payload) {
		JSONObject json = new JSONObject();
		
		ShowTimeDTO show = sService.getShowTimeDTOById(payload.getShowID());
		if(show == null) {
			json.put(ApplicationStringConstants.ERROR, ApplicationStringConstants.SOMETHINGWENTWRONG);
			return failureResponse(json);
		}
		
		SeatBookingDAO sbDetails = sb.getSeatBookingDAODetails(payload.getShowID());
		if(sbDetails!=null) {
			for(String each:sbDetails.getBookedSeats()) {
				if(sbDetails.getBookedSeats().contains(each)) {
					json.put(ApplicationStringConstants.ERROR, ApplicationStringConstants.SEATSBOOKED);
					return failureResponse(json);
				}
			}
		}
		
		
		TicketDTO td = payload.getTickets();
		float total = 0.0f;
		total += td.getChild()*show.getTicketPrices().getChild() 
			   + td.getAdult()*show.getTicketPrices().getAdult()
			   + td.getSenior()*show.getTicketPrices().getSenior();
		
		json.put("show", DefaultShowTimeService.getJsonFromShowTimeDAO(DefaultShowTimeService.populateShowTimeEntity(show)));
		json.put("tickets", td.toJSONString());
		json.put("totalWithoutTax", total);
		json.put("taxPercentage", 5);
		float totalWithTax = total + total*0.05f;
		json.put("total", totalWithTax);
		return successResponse(json);
		
	}
	
	public static JSONObject ConfirmBooking(BookingService bookingService, TransactionService transactionService,
			ShowTimeService sService,SeatBookingService sb,PromotionService promotionService, ConfirmBookingDTO payload) {
		JSONObject json = new JSONObject();
		
		ShowTimeDTO show = sService.getShowTimeDTOById(payload.getShowID());
		if(show == null) {
			json.put(ApplicationStringConstants.ERROR, ApplicationStringConstants.SOMETHINGWENTWRONG);
			return failureResponse(json);
		}
		
		SeatBookingDAO sbDetails = sb.getSeatBookingDAODetails(payload.getShowID());
		if(sbDetails!=null) {
			for(String each:sbDetails.getBookedSeats()) {
				if(sbDetails.getBookedSeats().contains(each)) {
					json.put(ApplicationStringConstants.ERROR, ApplicationStringConstants.SEATSBOOKED);
					return failureResponse(json);
				}
			}
		}
		
		TicketDTO td = payload.getTickets();
		int noTickets = td.getAdult()+td.getChild()+td.getSenior();
		if(noTickets!=payload.getBookedSeats().size()) {
			json.put(ApplicationStringConstants.ERROR, ApplicationStringConstants.SOMETHINGWENTWRONG);
			return failureResponse(json);
		}
		Float discount = 0.0f;
		PromotionDAO promotion = promotionService.getPromotionByCode(payload.getPromocode());
		
		if(promotion!=null) {
			discount = promotion.getDiscount();
		}
		
		float total = 0.0f;
		total += td.getChild()*show.getTicketPrices().getChild() 
			   + td.getAdult()*show.getTicketPrices().getAdult()
			   + td.getSenior()*show.getTicketPrices().getSenior();
		float discountedTotal = total - ((discount/100)*total);
		float totalwithTax = discountedTotal + discountedTotal*0.05f;
		
		json.put("show", DefaultShowTimeService.getJsonFromShowTimeDAO(DefaultShowTimeService.populateShowTimeEntity(show)));
		json.put("tickets", td.toJSONString());
		json.put("totalWithoutTax", total);
		json.put("discountedTotalWithoutTax", discountedTotal);
		json.put("taxPercentage", 5);
		json.put("total", totalwithTax);
		
		
		//transaction
		TransactionDTO tr = new TransactionDTO();
		tr.setTransactionType(TransactionType.CARD);
		
		String cardNum = payload.getPayment().getCardNumber();
		TransactionDetails transactionDetails = TransactionDetails.generateTransaction();
		transactionDetails.setCardNumber("XXXX"+cardNum.substring(cardNum.length()-4));
		tr.setTransactionDetails(transactionDetails);
		
		tr.setTrasactionAmount(totalwithTax);
		tr.setBillingAddress(payload.getPayment().getBillingAddress());
		
		LocalDateTime now = LocalDateTime.now();
		tr.setTransactionTime(now);
		
		TransactionDTO savedTransaction = transactionService.saveTransaction(tr);
		
		//seatbooking
		if(sbDetails==null) {
			
		}else {
			
		}
		
		
		//booking
		BookingDTO booking = new BookingDTO();
		booking.setUserID(payload.getUserID());
		booking.setMovieID(payload.getMovieID());
		booking.setShowID(payload.getShowID());
		booking.setTickets(td);
		booking.setPromoid(promotion!=null?promotion.getPromoID():null);
		booking.setBookedSeats(payload.getBookedSeats());
		booking.setTotal(totalwithTax);
		booking.setTransactionID(savedTransaction.getTransactionID());
		booking.setBookingTime(now);
		
		BookingDTO savedBooking = bookingService.saveBooking(booking);
		
		if(savedBooking!=null) {
			json.put("bookingID", savedBooking.getBookingID());
		}
		//emailsend
		
		return successResponse(json);
	}
	
	public static JSONObject getUserBookings(BookingService bookingService, TransactionService transactionService,
		ShowTimeService sService,SeatBookingService sb,PromotionService promotionService, Long userID) {
		
		JSONArray arr = bookingService.getBookingsOfuser(userID);
		
		JSONObject json = new JSONObject();
		json.put("bookings", arr);
		return successResponse(json);
	}
	
	
	

	public static JSONObject successResponse(JSONObject resp) {
		resp.put(ApplicationStringConstants.PROCESS, ApplicationStringConstants.SUCCESS);
		return resp;
	}

	public static JSONObject failureResponse(JSONObject resp) {
		resp.put(ApplicationStringConstants.PROCESS, ApplicationStringConstants.FAILURE);
		return resp;
	}

}

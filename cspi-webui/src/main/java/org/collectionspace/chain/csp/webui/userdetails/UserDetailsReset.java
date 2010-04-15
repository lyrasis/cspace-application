package org.collectionspace.chain.csp.webui.userdetails;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.collectionspace.chain.csp.config.ConfigException;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.collectionspace.csp.api.ui.UISession;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * receive JSON: { email: "fred@bloggs.com" }
 * test if valid user
 * if valid user: create token and email to user
 * if not send back exception
 * 
 * user gets email: goes to url in email and sets new password
 * we check that the email and token submitted with the password match
 * if so change password
 * if not send back exception
 * @author csm22
 *
 */
public class UserDetailsReset implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(UserDetailsReset.class);
	boolean setnewpassword = false;
	String base = "users";
	private static String tokensalt = "74102328UserDetailsReset";
	public UserDetailsReset(Boolean setnewpassword) { 
		this.setnewpassword = setnewpassword;
	}
	

	
	private Boolean doEmail(String csid, String emailparam) throws UIException {
		String token = createToken(csid);
		
		/* ABSTRACT EMAIL STUFF : WHERE do we get the content of emails from? */
String message = "A password reset has been requested from this email. " +
		"If you wish to reset your password please click on this link " +
		"/cspace-ui/html/?token="+token+"&email="+ emailparam;

	     String SMTP_HOST_NAME = "localhost";
	     String SMTP_PORT = "25";
	     String subject = "CollectionSpace Password reset request";
	     String from = "hendecasyllabic@googlemail.com";
	     String[] recipients = {"csm22@caret.cam.ac.uk"};
       Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
       boolean debug = true;
       
       Properties props = new Properties();
       props.put("mail.smtp.host", SMTP_HOST_NAME);
       props.put("mail.smtp.auth", "false");
       props.put("mail.debug", "true");
       props.put("mail.smtp.port", SMTP_PORT);

       Session session = Session.getDefaultInstance(props);

       session.setDebug(debug);

       Message msg = new MimeMessage(session);
       InternetAddress addressFrom;
		try {
			addressFrom = new InternetAddress(from);
       msg.setFrom(addressFrom);

       InternetAddress[] addressTo = new InternetAddress[recipients.length];
       for (int i = 0; i < recipients.length; i++) {
           addressTo[i] = new InternetAddress(recipients[i]);
       }
       msg.setRecipients(Message.RecipientType.TO, addressTo);

       // Setting the Subject and Content Type
       msg.setSubject(subject);
       msg.setContent(message, "text/plain");
       Transport.send(msg);
		} catch (AddressException e) {
			// TODO Auto-generated catch block
			log.info("AddressException: "+e.getMessage());
			return false;
		} catch (MessagingException e) {
			log.info("MessagingException: "+e.getMessage());
			return false;
		}
		
		return true;
	}
	
	private String createToken(String csid) throws UIException {
		try {
			byte[] buffer = csid.getBytes();
			byte[] result = null;
			StringBuffer buf = null;
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			result = new byte[md5.getDigestLength()];
			md5.reset();
			md5.update(buffer);
			result = md5.digest(tokensalt.getBytes());

			//create hex string from the 16-byte hash 
			buf = new StringBuffer(result.length * 2);
				for (int i = 0; i < result.length; i++) {
					int intVal = result[i] & 0xff;
					if (intVal < 0x10) {
						buf.append("0");
					}
				buf.append(Integer.toHexString(intVal).toUpperCase());
			}
				//log.info(csid);
				//log.info(buf.toString());
			return buf.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new UIException("There were problems with the algorithum");
		}

	}
	
	private Boolean testToken(String csid, String token) throws UIException {
		String match = createToken(csid);
		if(token.equals(match)){

			log.info("token matches");
			return true;
		}
		log.info("token fails"+token+":"+match);
		return false;
	}

	private String sendJSON(Storage storage,String path,JSONObject data) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject fields=data.optJSONObject("fields");
		if(path!=null) {
			// Update
			if(fields!=null)
				storage.updateJSON(base+"/"+path,fields);
		} //else {
			// Create SHOULDN"T EVER HAPPEN
			//if(fields!=null)
			//	path=storage.autocreateJSON(base,fields);
		//}
		return path;
	}
	
	private String getcsID(Storage storage,String emailparam) throws UIException {
		JSONObject restriction=new JSONObject();
		String tester = "";
		try {
			if(emailparam!=null && emailparam!="") {
				//restriction.put("keywords",emailparam);
				//Storage ss=makeServicesStorage(base+"/cspace-services/");
				//String[] paths=ss.getPaths("users/",new JSONObject("{\"email\": emailparam }"));
				String[] paths=storage.getPaths(base,restriction);
				for(int i=0;i<paths.length;i++) {
					if(paths[i].startsWith(base+"/"))
						paths[i]=paths[i].substring((base+"/").length());
					tester += paths[i];
					tester +="\n";
				}
				if (paths.length == 0){
					throw new UIException("Did not return any results");
				}
				else if (paths.length > 1){
				//	throw new UIException("Returned multiple result: "+ tester + ":: "+ paths.length);
				}
				/* one csid returned = valid user */
				return paths[0];
			}
			else{
				throw new UIException("No email specified");
			}
		//} catch (JSONException e) {
		//	throw new UIException("JSONException during search on email address",e);
		} catch (ExistException e) {
			throw new UIException("ExistException during search on email address",e);
		} catch (UnimplementedException e) {
			throw new UIException("UnimplementedException during search on email address",e);
		} catch (UnderlyingStorageException e) {
			throw new UIException("UnderlyingStorageException during search on email address " + emailparam +":"+base,e);
		}
	}
	
	/* find csid for email, create token, email token to the user */
	private void send_reset_email(Storage storage,UIRequest request, Request in) throws UIException {

		//mock login else service layer gets upset
		//GET WORKING ARGH
		request.getSession().setValue(UISession.USERID,"test");
		request.getSession().setValue(UISession.PASSWORD,"test");
		((Request)in).reset();
		
		JSONObject data = null;
		
		JSONObject outputJSON = new JSONObject();
		
		data=request.getJSONBody();
		log.info("JSON OBJECT",data);
		String emailparam = "";
		
		/* get csid of email address - need a better way of searching as this might create false positives
		 * Do we have a more precise way of searching rather than keywords.... */
		try {
			emailparam = data.getString("email");
			String csid = getcsID(storage,emailparam);

			if(!doEmail(csid,emailparam)){
				throw new UIException("Failed to send email");
			}
			outputJSON.put("ok",true);
			outputJSON.put("message","Password reset sent to " + emailparam);
			/* for debug purposes */
			//if(data.getBoolean("debug")){
			//	outputJSON.put("token",createToken(csid));
			//}
		} catch (JSONException e) {
			throw new UIException("JSONException during search on email address",e);
		}		

		request.getSession().setValue(UISession.USERID,"");
		request.getSession().setValue(UISession.PASSWORD,"");
		((Request)in).reset();
		request.sendJSONResponse(outputJSON);
	}
	
	/* check token and if matches csid then reset password 
	 * */
	private void reset_password(Storage storage,UIRequest request, Request in) throws UIException {
		JSONObject outputJSON = new JSONObject();
		String token=request.getRequestArgument("token");
		String password=request.getRequestArgument("password");
		String email=request.getRequestArgument("email");
		
		String csid = getcsID(storage,email);
		if(testToken(csid,token)){
			/* update userdetails */
			String path = csid;
			try {
				/* if I send less data will the service layer leave the missing items alone or null them?*/
				JSONObject changedata = new JSONObject();
				JSONObject updatefields = new JSONObject();
				updatefields.put("email",email);
				updatefields.put("password",password);
				changedata.put("fields",updatefields);
				changedata.put("csid",csid);
				
				path=sendJSON(storage,path,changedata);
				
				outputJSON.put("ok",true);
				outputJSON.put("message","Your Password has been succesfully changed, Please login");
			}	catch (JSONException x) {
				throw new UIException("Failed to parse json: "+x,x);
			} catch (ExistException x) {
				throw new UIException("Existence exception: "+x,x);
			} catch (UnimplementedException x) {
				throw new UIException("Unimplemented exception: "+x,x);
			} catch (UnderlyingStorageException x) {
				throw new UIException("Problem storing: "+x,x);
			} 
			
		}
		else{
			throw new UIException("Token did not match the csid");
		}
		/* should we automagically log them in or let them do that?, 
		 * I think we should let them login, it has the advantage 
		 * that they find out straight away if they can't remember the new password  */
		request.sendJSONResponse(outputJSON);
	}
	

	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		if(setnewpassword){
			reset_password(q.getStorage(),q.getUIRequest(),q);
		}
		else{
			send_reset_email(q.getStorage(),q.getUIRequest(),q);
		}
	}
 

	public void configure() throws ConfigException {}
	public void configure(WebUI ui,Spec spec) {}
}

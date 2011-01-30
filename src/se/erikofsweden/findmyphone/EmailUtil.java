package se.erikofsweden.findmyphone;

import java.util.Date;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class EmailUtil extends Authenticator {
	String defaultHost = "smtp.gmail.com";
	String defaultPort = "465";
	String defaultContent = "text/plain";
	
	String defaultUser = "";
	String defaultPassword = "";
	private String currentUser;
	private String currentPassword;
	
	boolean sendEmail(String from, String[] to, String subject, String body, String user, String password) throws MessagingException {
		Properties props = new Properties(); 
		 
	    props.put("mail.smtp.host", defaultHost);
	    props.put("mail.debug", "true"); 
	    props.put("mail.smtp.auth", "true");
	    
	    props.put("mail.smtp.port", defaultPort); 
	    props.put("mail.smtp.socketFactory.port", defaultPort); 
	    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); 
	    props.put("mail.smtp.socketFactory.fallback", "false");
	    
	    currentUser = (user != null && user.length() > 0) ? user : defaultUser;
	    currentPassword = (password != null && password.length() > 0) ? password : defaultPassword;
	    
	    Session session = Session.getInstance(props, this);
	    MimeMessage msg = new MimeMessage(session);
	    if(from != null && from.length() > 0) {
	    	msg.setFrom(new InternetAddress(from));
	    } else {
	    	msg.setFrom(new InternetAddress(currentUser));
	    }
	    InternetAddress[] addressTo = new InternetAddress[to.length]; 
		for (int i = 0; i < to.length; i++) {
			addressTo[i] = new InternetAddress(to[i]);
		}
	    msg.setRecipients(MimeMessage.RecipientType.TO, addressTo); 
		msg.setSubject(subject);
		msg.setSentDate(new Date());

		// setup message body
		MimeBodyPart messageBodyPart = new MimeBodyPart();
		messageBodyPart.setContent(body, defaultContent);
		MimeMultipart multipart = new MimeMultipart();
		multipart.addBodyPart(messageBodyPart);

		// Put parts in message
		msg.setContent(multipart);

		// send email
		Transport.send(msg);
	 
		return true;
	}

	@Override
	public PasswordAuthentication getPasswordAuthentication() {
		return new PasswordAuthentication(currentUser, currentPassword);
	}
	
	public void setDefaultContent(String defaultContent) {
		this.defaultContent = defaultContent;
	}
}

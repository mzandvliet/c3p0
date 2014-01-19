package c3po;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.structs.TradeIntention;

import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;

/* 
 * TODO:
 * - Better encapsulation of bot ID
 * 		- Does this thing log trades in general, or is it a bot-specific trade logger? There's a difference.
 * 
 * Modified from this: http://stackoverflow.com/questions/46663/how-to-send-an-email-by-java-application-using-gmail-yahoo-hotmail
 */

public class EmailTradeLogger implements ITradeListener {
	//================================================================================
    // Main
    //================================================================================
	
	private static final Logger LOGGER = LoggerFactory.getLogger(EmailTradeLogger.class);
	private static final String HOST = "sub4.homie.mail.dreamhost.com";
	private static final String USER_NAME = "c3po@ramjetanvil.com";  // GMail user name (just the part before "@gmail.com")
    private static final String PASSWORD = "S0youthinkyoucantrade"; // GMail password
    private static final String RECIPIENT = "zandvliet.martijn@gmail.com";
    
    public static void main(String[] args) {
    	String host = HOST;
        String from = USER_NAME;
        String pass = PASSWORD;
        String[] to = { RECIPIENT }; // list of recipient email addresses
        String subject = "Java send mail example";
        String body = "Welcome to JavaMail!";

        sendMail(host, from, pass, to, subject, body);
    }
    
  	//================================================================================
    // Class
    //================================================================================
	
    private int botId;
    private String[] recipients;
    
	public EmailTradeLogger(int botId, String[] recipients) {
		this.botId = botId;
		this.recipients = recipients;
	}
	
	@Override
	public void onTrade(TradeIntention action) {
		String host = HOST;
        String from = USER_NAME;
        String pass = PASSWORD;
        String subject = "Bot " + botId + " - Trade Notification";
        String body = action.toString();

        sendMail(host, from, pass, recipients, subject, body);
	}
	
	private static void sendMail(String host, String from, String pass, String[] to, String subject, String body) {
        Properties props = System.getProperties();
        
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.user", from);
        props.put("mail.smtp.password", pass);
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");

        Session session = Session.getDefaultInstance(props);
        MimeMessage message = new MimeMessage(session);

        try {
            message.setFrom(new InternetAddress(from));
            InternetAddress[] toAddress = new InternetAddress[to.length];

            // To get the array of addresses
            for( int i = 0; i < to.length; i++ ) {
                toAddress[i] = new InternetAddress(to[i]);
            }

            for( int i = 0; i < toAddress.length; i++) {
                message.addRecipient(Message.RecipientType.TO, toAddress[i]);
            }

            message.setSubject(subject);
            message.setText(body);
            Transport transport = session.getTransport("smtp");
            transport.connect(host, from, pass);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
            
            LOGGER.debug("Sent email to " + to.length + " recipients");
        }
        catch (Exception e) {
            LOGGER.error("Could not send mail", e);
        }
    }
}

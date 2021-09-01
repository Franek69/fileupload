package Mailer;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class Mailer{

    public static void send(String from,String password,String to,String sub, String content){
        //Propertie-Objekt wird erstellt, mit den Eigenschaften für den Mailserver (hier gmail)
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");

        // Session-Objekt wird erstellt, Absenderadresse und Passwort werden aus dem Aufruf übernommen
        // geprüft und an das MimeMessage-Objekt übergeben
        Session session = Session.getDefaultInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(from,password);
                    }
                });


        try {
            // erstellt ein MimeMessage-Objekt mit Eigenschaften wie E-Mail-Adresse des Empfängers,
            // E-Mail-Betreff, Reply-To-E-Mail, E-Mail-Body, Anhänge usw.
            MimeMessage message = new MimeMessage(session);

            message.addRecipient(Message.RecipientType.TO,new InternetAddress(to));
            message.setSubject(sub);
            message.setFrom(new InternetAddress("no_reply@example.com", "NoReply-JD"));
            message.setReplyTo(InternetAddress.parse("no_reply@example.com", false));
            message.setContent(content,"text/html" );

            //send message
            Transport.send(message);

            System.out.println("Email wurde erfolgreich versandt");
        } catch (MessagingException | UnsupportedEncodingException e) {throw new RuntimeException(e);}

}
}
class SendMailSSL{
    public static void main(String[] args) {
        //from,password,to,subject,message
        //ändere from, password and to  - (zBsp gmail-account als Ersatz für eigenen Mailserver)
        Mailer.send("absender@domain","XXXXXXXX","mail@domain","ssl fileupload", "html?");

    }
}
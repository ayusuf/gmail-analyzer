package main.java;

import javax.mail.*;
import java.util.Properties;

public class Main {

    public static void main(String[] args) throws MessagingException {
        String username = args[0];
        String password = args[1];

        Store store;
        Message[] messages;

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", "imap.gmail.com");
        props.put("mail.imaps.port", "993");
        props.put("mail.imaps.ssl.enable", "true");

        Session session = Session.getInstance(props);
        store = session.getStore(props.getProperty("mail.store.protocol"));
        store.connect(props.getProperty("mail.imaps.host"), username, password);

        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);

        messages = inbox.getMessages();
        //Collections.reverse(Arrays.asList(messages));
        System.out.println("Total Messages: " + messages.length);


        store.close();
    }



}

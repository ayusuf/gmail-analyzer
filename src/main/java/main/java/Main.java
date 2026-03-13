package main.java;

import com.sun.mail.imap.IMAPFolder;

import javax.mail.FetchProfile;
import javax.mail.Part;
import javax.mail.Store;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.BodyPart;

import java.io.FileWriter;
import java.io.PrintWriter;

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

        // Prefetch envelope (From, Subject, Date) and content type for all messages
        // in a single IMAP FETCH command instead of one round-trip per message.
        FetchProfile profile = new FetchProfile();
        profile.add(FetchProfile.Item.ENVELOPE);
        inbox.fetch(messages, profile);

        System.out.println("Total Messages: " + messages.length);
        System.out.println("Folders");
        listFolders(store);
        printMessages(messages, messages.length, "messages.csv");

        store.close();
    }

    public static void listFolders(Store store) throws MessagingException {
        IMAPFolder[] folders = (IMAPFolder[]) store.getDefaultFolder().list("*");
        for(IMAPFolder folder : folders) {

            int count = folder.getType() == 3 ? folder.getMessageCount() : 0;
            System.out.println(folder.getName() + " = " + count);
        }
    }

    public static boolean hasAttachments(Message message) throws Exception {
        if (message.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) message.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void printMessages(Message[] messages, int n, String filename) {
        PrintWriter writer = null;
        boolean isConsole = filename == null || filename.isEmpty();
        try {
            if (isConsole) {
                writer = new PrintWriter(System.out, true);
            } else {
                writer = new PrintWriter(new FileWriter(filename));
                writer.println("From,Subject,ReceivedDate,HasAttachment");
            }

            for (int i = 0; i < n && i < messages.length; i++) {
                String from = messages[i].getFrom()[0].toString().replace("\"", "\"\"");
                String subject = messages[i].getSubject() != null ? messages[i].getSubject().replace("\"", "\"\"") : "";
                String receivedDate = messages[i].getReceivedDate() != null ? messages[i].getReceivedDate().toString() : "";
                String hasAttachment = String.valueOf(hasAttachments(messages[i]));
                if (isConsole) {
                    writer.printf("%s,%s,%s,%s%n", from, subject, receivedDate,hasAttachment);
                } else {
                    writer.printf("\"%s\",\"%s\",\"%s\",\"%s\"%n", from, subject, receivedDate, hasAttachment);
                }
                if ((i + 1) % 1000 == 0) {
                    System.out.println("Processed " + (i + 1) + " messages...");
                }
            }
            if (!isConsole) {
                System.out.println("Messages exported to " + filename);
            }
        } catch (Exception e) {
            System.err.println("Error writing messages: " + e.getMessage());
        } finally {
            if (writer != null && !isConsole) {
                writer.close();
            }
        }
    }
}

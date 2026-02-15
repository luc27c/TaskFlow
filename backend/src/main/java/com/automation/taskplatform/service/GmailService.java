package com.automation.taskplatform.service;

import com.automation.taskplatform.model.User;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Service
public class GmailService {

    private static final Logger log = LoggerFactory.getLogger(GmailService.class);

    private final GoogleOAuthService googleOAuthService;
    private final AiSummaryService aiSummaryService;

    public GmailService(GoogleOAuthService googleOAuthService, AiSummaryService aiSummaryService) {
        this.googleOAuthService = googleOAuthService;
        this.aiSummaryService = aiSummaryService;
    }

    private Gmail getGmailService(User user) throws IOException {
        String accessToken = googleOAuthService.getAccessToken(user);

        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);

        return new Gmail.Builder(
            new NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
        .setApplicationName("TaskFlow")
        .build();
    }

    public List<EmailSummary> getEmailsSince(User user, LocalDateTime since) throws IOException {
        Gmail gmail = getGmailService(user);
        List<EmailSummary> summaries = new ArrayList<>();

        // Convert LocalDateTime to epoch seconds for Gmail query
        long sinceEpoch = since.atZone(ZoneId.systemDefault()).toEpochSecond();
        String query = "after:" + sinceEpoch;

        ListMessagesResponse response = gmail.users().messages()
            .list("me")
            .setQ(query)
            .setMaxResults(50L)
            .execute();

        if (response.getMessages() == null) {
            return summaries;
        }

        for (Message messageRef : response.getMessages()) {
            Message message = gmail.users().messages()
                .get("me", messageRef.getId())
                .setFormat("metadata")
                .setMetadataHeaders(List.of("From", "Subject", "Date"))
                .execute();

            EmailSummary summary = new EmailSummary();

            for (MessagePartHeader header : message.getPayload().getHeaders()) {
                switch (header.getName()) {
                    case "From" -> summary.setFrom(header.getValue());
                    case "Subject" -> summary.setSubject(header.getValue());
                    case "Date" -> summary.setDate(header.getValue());
                }
            }
            summary.setSnippet(message.getSnippet());

            // Generate AI summary if enabled
            if (aiSummaryService.isEnabled()) {
                String aiSummary = aiSummaryService.summarizeEmail(
                    summary.getFrom(),
                    summary.getSubject(),
                    summary.getSnippet()
                );
                summary.setAiSummary(aiSummary);
            }

            summaries.add(summary);
        }

        return summaries;
    }

    public String generateRecapHtml(List<EmailSummary> emails, LocalDateTime from, LocalDateTime to) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");
        html.append("<h1 style='color: #4F46E5;'>📧 Your Email Recap</h1>");
        html.append("<p style='color: #666;'>From ").append(from.format(formatter))
            .append(" to ").append(to.format(formatter)).append("</p>");
        html.append("<p style='color: #333;'><strong>").append(emails.size())
            .append(" emails</strong> received during this period.</p>");
        html.append("<hr style='border: 1px solid #eee; margin: 20px 0;'>");

        if (emails.isEmpty()) {
            html.append("<p style='color: #666;'>No new emails during this period.</p>");
        } else {
            for (EmailSummary email : emails) {
                html.append("<div style='background: #f9fafb; padding: 15px; border-radius: 8px; margin-bottom: 10px;'>");
                html.append("<p style='margin: 0 0 5px 0;'><strong>From:</strong> ").append(escapeHtml(email.getFrom())).append("</p>");
                html.append("<p style='margin: 0 0 5px 0;'><strong>Subject:</strong> ").append(escapeHtml(email.getSubject())).append("</p>");

                // Show AI summary if available, otherwise show snippet
                if (email.getAiSummary() != null && !email.getAiSummary().isEmpty()) {
                    html.append("<p style='margin: 0; color: #4F46E5; font-size: 14px;'>")
                        .append("💡 ").append(escapeHtml(email.getAiSummary())).append("</p>");
                } else {
                    html.append("<p style='margin: 0; color: #666; font-size: 14px;'>")
                        .append(escapeHtml(email.getSnippet())).append("</p>");
                }
                html.append("</div>");
            }
        }

        html.append("<hr style='border: 1px solid #eee; margin: 20px 0;'>");
        html.append("<p style='color: #999; font-size: 12px;'>Generated by TaskFlow</p>");
        html.append("</body></html>");

        return html.toString();
    }

    public void sendEmail(User user, String to, String subject, String htmlBody) throws IOException, MessagingException {
        log.info("Preparing to send email from {} to {} with subject: {}", user.getEmail(), to, subject);
        Gmail gmail = getGmailService(user);

        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(user.getEmail()));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setContent(htmlBody, "text/html; charset=utf-8");

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        byte[] rawMessageBytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(rawMessageBytes);

        Message message = new Message();
        message.setRaw(encodedEmail);

        Message sentMessage = gmail.users().messages().send("me", message).execute();
        log.info("Email sent successfully! Message ID: {}", sentMessage.getId());
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    // Inner class for email summary
    public static class EmailSummary {
        private String from;
        private String subject;
        private String date;
        private String snippet;
        private String aiSummary;

        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getSnippet() { return snippet; }
        public void setSnippet(String snippet) { this.snippet = snippet; }
        public String getAiSummary() { return aiSummary; }
        public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }
    }
}

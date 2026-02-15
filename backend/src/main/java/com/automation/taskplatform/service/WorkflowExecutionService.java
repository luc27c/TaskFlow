package com.automation.taskplatform.service;

import com.automation.taskplatform.model.User;
import com.automation.taskplatform.model.Workflow;
import com.automation.taskplatform.repository.WorkflowRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class WorkflowExecutionService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionService.class);

    private final WorkflowRepository workflowRepository;
    private final GmailService gmailService;
    private final GoogleOAuthService googleOAuthService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WorkflowExecutionService(
            WorkflowRepository workflowRepository,
            GmailService gmailService,
            GoogleOAuthService googleOAuthService) {
        this.workflowRepository = workflowRepository;
        this.gmailService = gmailService;
        this.googleOAuthService = googleOAuthService;
    }

    // Run every minute to check for scheduled workflows
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void runScheduledWorkflows() {
        List<Workflow> workflows = workflowRepository.findByActiveTrueAndTriggerType("SCHEDULE");
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        log.info("Scheduler check at {} - Found {} scheduled workflows", now, workflows.size());

        for (Workflow workflow : workflows) {
            try {
                boolean shouldRun = shouldRunNow(workflow, now);
                log.debug("Workflow '{}' (cron: {}) - shouldRun: {}",
                         workflow.getName(), workflow.getCronExpression(), shouldRun);

                if (shouldRun) {
                    log.info("Scheduled execution triggered for workflow '{}'", workflow.getName());
                    executeWorkflow(workflow);
                }
            } catch (Exception e) {
                log.error("Failed to execute scheduled workflow {}: {}", workflow.getId(), e.getMessage());
            }
        }
    }

    private boolean shouldRunNow(Workflow workflow, LocalDateTime now) {
        String cron = workflow.getCronExpression();
        if (cron == null || cron.isBlank()) {
            return false;
        }

        try {
            // Convert 5-field cron to 6-field by prepending "0" for seconds
            String normalizedCron = cron;
            if (cron.split(" ").length == 5) {
                normalizedCron = "0 " + cron;
            }

            CronExpression cronExpression = CronExpression.parse(normalizedCron);
            // Check if current minute matches cron schedule
            LocalDateTime lastMinute = now.minusMinutes(1);
            LocalDateTime nextRun = cronExpression.next(lastMinute);

            boolean shouldRun = nextRun != null && nextRun.equals(now);
            log.info("Workflow '{}': cron='{}' normalized='{}' now={} nextRun={} shouldRun={}",
                    workflow.getName(), cron, normalizedCron, now, nextRun, shouldRun);

            return shouldRun;
        } catch (Exception e) {
            log.error("Invalid cron expression '{}' for workflow {}: {}", cron, workflow.getId(), e.getMessage());
            return false;
        }
    }

    @Transactional
    public void executeWorkflow(Workflow workflow) {
        User user = workflow.getUser();
        log.info("Executing workflow '{}' (ID: {}) for user: {}", workflow.getName(), workflow.getId(), user.getEmail());

        if (!googleOAuthService.isConnected(user)) {
            log.error("User {} has not connected Gmail", user.getEmail());
            throw new RuntimeException("User has not connected their Gmail account");
        }

        try {
            switch (workflow.getActionType()) {
                case "EMAIL_RECAP" -> executeEmailRecap(workflow, user);
                case "SEND_EMAIL" -> executeSendEmail(workflow, user);
                default -> throw new RuntimeException("Unknown action type: " + workflow.getActionType());
            }

            // Update last run time
            workflow.setLastRunAt(LocalDateTime.now());
            workflowRepository.save(workflow);
            log.info("Workflow '{}' executed successfully", workflow.getName());

        } catch (Exception e) {
            log.error("Failed to execute workflow '{}': {}", workflow.getName(), e.getMessage(), e);
            throw new RuntimeException("Failed to execute workflow: " + e.getMessage(), e);
        }
    }

    private void executeEmailRecap(Workflow workflow, User user) throws Exception {
        // Parse config to get hoursBack (default: 18)
        int hoursBack = 18;
        try {
            if (workflow.getActionConfig() != null && !workflow.getActionConfig().isBlank()) {
                JsonNode config = objectMapper.readTree(workflow.getActionConfig());
                if (config.has("hoursBack")) {
                    hoursBack = config.get("hoursBack").asInt(18);
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse actionConfig, using default hoursBack: {}", e.getMessage());
        }

        LocalDateTime from = LocalDateTime.now().minusHours(hoursBack);
        LocalDateTime to = LocalDateTime.now();

        log.info("Fetching emails from {} to {} ({} hours back) for user {}", from, to, hoursBack, user.getEmail());

        // Get emails
        List<GmailService.EmailSummary> emails = gmailService.getEmailsSince(user, from);
        log.info("Found {} emails in the specified timeframe", emails.size());

        // Generate recap HTML
        String recapHtml = gmailService.generateRecapHtml(emails, from, to);

        // Send recap email to user
        String subject = "📧 Your Email Recap - " + to.toLocalDate().toString();
        log.info("Sending recap email to: {}", user.getEmail());
        gmailService.sendEmail(user, user.getEmail(), subject, recapHtml);
        log.info("Recap email sent successfully to {}", user.getEmail());
    }

    private void executeSendEmail(Workflow workflow, User user) throws Exception {
        // Parse actionConfig for email details
        String to;
        String subject;
        String body;

        try {
            if (workflow.getActionConfig() == null || workflow.getActionConfig().isBlank()) {
                throw new RuntimeException("Email configuration is missing");
            }
            JsonNode config = objectMapper.readTree(workflow.getActionConfig());
            to = config.has("to") ? config.get("to").asText() : user.getEmail();
            subject = config.has("subject") ? config.get("subject").asText() : "Reminder from TaskFlow";
            body = config.has("body") ? config.get("body").asText() : "";
        } catch (Exception e) {
            throw new RuntimeException("Invalid email configuration: " + e.getMessage());
        }

        // Build HTML email body
        String htmlBody = buildReminderEmailHtml(subject, body);

        log.info("Sending reminder email to: {}", to);
        gmailService.sendEmail(user, to, subject, htmlBody);
        log.info("Reminder email sent successfully to {}", to);
    }

    private String buildReminderEmailHtml(String subject, String body) {
        return """
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background: linear-gradient(135deg, #6366f1 0%%, #8b5cf6 100%%); padding: 20px; border-radius: 10px 10px 0 0;">
                    <h1 style="color: white; margin: 0;">⏰ Reminder</h1>
                </div>
                <div style="background: #f9fafb; padding: 20px; border-radius: 0 0 10px 10px; border: 1px solid #e5e7eb; border-top: none;">
                    <p style="font-size: 16px; color: #374151; white-space: pre-wrap;">%s</p>
                </div>
                <p style="color: #9ca3af; font-size: 12px; margin-top: 20px;">Sent by TaskFlow</p>
            </body>
            </html>
            """.formatted(body.replace("\n", "<br>"));
    }

    // Manual execution endpoint
    @Transactional
    public void runNow(Long workflowId) {
        log.info("Manual run requested for workflow ID: {}", workflowId);
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));
        executeWorkflow(workflow);
    }
}

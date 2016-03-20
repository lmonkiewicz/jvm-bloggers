package pl.tomaszdziurko.jvm_bloggers.mailing;


import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.tomaszdziurko.jvm_bloggers.mailing.domain.MailingAddress;
import pl.tomaszdziurko.jvm_bloggers.mailing.domain.MailingAddressRepository;
import pl.tomaszdziurko.jvm_bloggers.utils.NowProvider;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class BlogSummaryMailSender {

    public static final String MAIL_SUMMARY_TITLE_PREFIX = "[JVM Bloggers] #";
    public static final String MAIL_SUMMARY_TITLE_POSTIFX = ": Nowe wpisy na polskich blogach, ";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BlogSummaryMailGenerator mailGenerator;
    private final MailSender mailSender;
    private final MailingAddressRepository mailingAddressRepository;
    private final IssueNumberRetriever issueNumberRetriever;
    private final MailingSleepIntervalProvider mailingSleepIntervalProvider;
    private final NowProvider nowProvider;

    public void sendSummary(int numberOfDaysBackInThePast) {
        List<MailingAddress> mailingAddresses = mailingAddressRepository.findAll();
        if (mailingAddresses.isEmpty()) {
            log.warn("No e-mails in database to send Blog Summary !!!");
            return;
        }

        long issueNumber = issueNumberRetriever.getNextIssueNumber();
        String mailContent = mailGenerator
            .prepareMailContent(numberOfDaysBackInThePast, issueNumber);
        log.info("Mail content = \n" + mailContent);
        String issueTitle = prepareIssueTitle(issueNumber);
        mailingAddresses.stream().map(MailingAddress::getAddress).forEach(recipient -> {
                mailSender.sendEmail(recipient, issueTitle, mailContent);
                sleepForABit();
            }
        );
    }

    @SneakyThrows
    private void sleepForABit() {
        SleepInterval sleepingInterval = mailingSleepIntervalProvider.getSleepingInterval();
        log.info("Sleeping for {}s", sleepingInterval.asSeconds());
        Thread.sleep(sleepingInterval.asMilliseconds());
    }

    private String prepareIssueTitle(long issueNumber) {
        return MAIL_SUMMARY_TITLE_PREFIX + issueNumber + MAIL_SUMMARY_TITLE_POSTIFX
            + getTodayDateAsString();
    }

    private String getTodayDateAsString() {
        return nowProvider.now().format(FORMATTER);
    }
}

/*
 * Copyright 2023 SNOMED International - All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of SNOMED International
 * The intellectual and technical concepts contained herein are proprietary to
 * SNOMED International and may be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.  Dissemination of this information
 * or reproduction of this material is strictly forbidden.
 */
package org.ihtsdo.refsetservice.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Utility class for interacting with email.
 */
public final class EmailUtility {

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(EmailUtility.class);

    /** The email SMTP user. */
    private static String smtpUser;

    /** The email SMTP password. */
    private static String smtpPassword;

    // /** The email SMTP host. */
    // private static String smtpHost;
    //
    // /** The email SMTP port. */
    // private static String smtpPort;
    //
    // /** The email to send errors to. */
    // private static String errorToEmail;

    /** The email address sent emails are from. */
    private static String emailFrom;

    /** The email enabled. */
    private static String emailEnabled;

    /** The Constant emailValidationRegexPattern. */
    private static final String EMAIL_VALIDATION_REGEX_PATTERN =
        "^(?=.{1,64}@)[\\p{L}0-9_+-]+(\\.[\\p{L}0-9_+-]+)*@[^-][\\p{L}0-9-]+(\\.[\\p{L}0-9-]+)*(\\.[\\p{L}]{2,})$";

    /** Static initialization. */
    static {

        smtpUser = PropertyUtility.getProperty("mail.smtp.user");
        smtpPassword = PropertyUtility.getProperty("mail.smtp.password");
        // smtpHost = PropertyUtility.getProperty("mail.smtp.host");
        // smtpPort = PropertyUtility.getProperty("mail.smtp.port");
        // errorToEmail = PropertyUtility.getProperty("mail.smtp.error.to");
        emailFrom = PropertyUtility.getProperty("mail.smtp.from");
        emailEnabled = PropertyUtility.getProperty("mail.enabled");
    }

    /**
     * Instantiates an empty {@link EmailUtility}.
     */
    private EmailUtility() {

        // n/a
    }

    /**
     * Sends email.
     *
     * @param subject the subject
     * @param from the from
     * @param recipients the recipients
     * @param body the body
     * @throws Exception the exception
     */
    public static void sendEmail(final String subject, final String from, final Set<String> recipients, final String body) throws Exception {

        if (recipients == null || recipients.isEmpty()) {

            final String message = "Email must have recipients";
            LOG.error(message);
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, message);
        }

        if (recipients.stream().anyMatch(r -> !r.matches(EMAIL_VALIDATION_REGEX_PATTERN))) {

            // invalid email address. Return 400
            final List<String> failingEmailAddresses = recipients.stream().filter(r -> r.matches(EMAIL_VALIDATION_REGEX_PATTERN)).collect(Collectors.toList());

            final String message = "Invalid email address requested for recipient(s): " + failingEmailAddresses;
            LOG.error(message);
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, message);
        }

        // avoid sending mail if disabled
        if ("false".equals(emailEnabled)) {

            return;
        }

        final Session session = Session.getInstance(PropertyUtility.getProperties(), new Authenticator() {

            /* see superclass */
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {

                return new PasswordAuthentication(smtpUser, smtpPassword);
            }
        });

        final MimeMessage message = new MimeMessage(session);

        if (body.contains("<html")) {

            message.setContent(body.toString(), "text/html; charset=utf-8");
        } else {

            message.setText(body.toString());
        }

        message.setSubject(subject);
        final String fromAdress = (from != null && !from.isBlank()) ? from : emailFrom;
        message.setFrom(new InternetAddress(fromAdress));

        for (final String recipient : recipients) {

            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
        }

        LOG.info("Sending email: " + message);
        Transport.send(message);
    }

    /**
     * Sends email. Convert recipients with either semi-colon or comma delimiter to List<String>.
     *
     * @param subject the subject
     * @param from the from
     * @param recipients the recipients
     * @param body the body
     * @throws Exception the exception
     */
    public static void sendEmail(final String subject, final String from, final String recipients, final String body) throws Exception {

        if (recipients != null && StringUtils.isNotBlank(recipients)) {

            final Set<String> recipientList = new HashSet<>();

            if (recipients.contains(";")) {
                recipientList.addAll(FieldedStringTokenizer.splitAsSet(recipients, ";"));
            } else if (recipients.contains(",")) {
                recipientList.addAll(FieldedStringTokenizer.splitAsSet(recipients, ","));
            } else {
                recipientList.add(recipients);
            }

            sendEmail(subject, from, recipientList, body);
        } else {
            throw new Exception("Email must have recipients");
        }

    }

    /**
     * SMTPAuthenticator.
     */
    public static class SMTPAuthenticator extends Authenticator {

        /**
         * Returns the password authentication.
         *
         * @return the password authentication
         */
        /* see superclass */
        @Override
        public PasswordAuthentication getPasswordAuthentication() {

            if (smtpPassword == null) {

                return null;
            } else {

                return new PasswordAuthentication(smtpUser, smtpPassword);
            }

        }
    }
}

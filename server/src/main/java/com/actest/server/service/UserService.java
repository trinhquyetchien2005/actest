package com.actest.server.service;

import com.actest.server.repository.UserRepository;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

public class UserService {
    private final UserRepository userRepository;
    private final Map<String, String> otpStorage = new HashMap<>();
    private final Map<String, UserData> tempUserStorage = new HashMap<>();

    public UserService() {
        this.userRepository = new UserRepository();
    }

    public boolean login(String email, String password) {
        return userRepository.validateUser(email, password);
    }

    public boolean initiateRegistration(String email, String password, String name) {
        if (userRepository.userExists(email)) {
            return false;
        }
        String otp = generateOtp();
        System.out.println(">>> GENERATED OTP for " + email + ": " + otp); // Log OTP immediately
        otpStorage.put(email, otp);
        tempUserStorage.put(email, new UserData(email, password, name));
        sendOtpEmail(email, otp);
        return true;
    }

    public boolean verifyOtpAndRegister(String email, String otp) {
        if (otpStorage.containsKey(email) && otpStorage.get(email).equals(otp)) {
            UserData userData = tempUserStorage.get(email);
            if (userData != null) {
                boolean created = userRepository.createUser(userData.email, userData.password, userData.name);
                if (created) {
                    otpStorage.remove(email);
                    tempUserStorage.remove(email);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.userExists(email);
    }

    public boolean initiateEmailChange(String currentEmail, String newEmail) {
        if (userRepository.userExists(newEmail)) {
            return false;
        }
        String otp = generateOtp();
        System.out.println(">>> GENERATED OTP for Email Change (" + currentEmail + " -> " + newEmail + "): " + otp);
        otpStorage.put(newEmail, otp); // Store OTP against NEW email
        sendOtpEmail(newEmail, otp);
        return true;
    }

    public boolean verifyEmailChangeOtp(String currentEmail, String newEmail, String otp) {
        if (otpStorage.containsKey(newEmail) && otpStorage.get(newEmail).equals(otp)) {
            if (userRepository.updateEmail(currentEmail, newEmail)) {
                otpStorage.remove(newEmail);
                return true;
            }
        }
        return false;
    }

    public boolean updateProfile(String email, String name, String password) {
        return userRepository.updateProfile(email, name, password);
    }

    public int getUserIdByEmail(String email) {
        return userRepository.getUserIdByEmail(email);
    }

    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    private void sendOtpEmail(String toEmail, String otp) {
        // TODO: Replace with actual SMTP credentials or use a configuration file
        final String username = "trinhquyetchien2005@gmail.com";
        final String password = "xtufgpibfwgedbyk"; // Spaces removed

        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true"); // TLS

        Session session = Session.getInstance(prop,
                new jakarta.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("trinhquyetchien2005@gmail.com"));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(toEmail));
            message.setSubject("ACTEST Registration OTP");
            message.setText("Your OTP for ACTEST registration is: " + otp);

            Transport.send(message);
            System.out.println("OTP sent to " + toEmail);
        } catch (MessagingException e) {
            e.printStackTrace();
            System.err.println("Failed to send OTP email: " + e.getMessage());
            // Fallback: Log OTP to console for testing
            System.out.println("FALLBACK OTP for " + toEmail + ": " + otp);
        }
    }

    private static class UserData {
        String email;
        String password;
        String name;

        UserData(String email, String password, String name) {
            this.email = email;
            this.password = password;
            this.name = name;
        }
    }
}

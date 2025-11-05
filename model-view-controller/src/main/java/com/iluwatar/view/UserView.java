package com.iluwatar.view;

import com.iluwatar.model.User;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserView {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^(\w+)@(.+)$");

    public String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    public String maskEmail(String email) {
        if (email == null) {
            return "N/A";
        }
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        if (matcher.matches()) {
            String username = matcher.group(1);
            String domain = matcher.group(2);
            if (username.length() <= 3) {
                return username + "***@" + domain;
            } else {
                return username.substring(0, 3) + "***@" + domain;
            }
        }
        return email;
    }

    public String getUserStatus(User user) {
        if (user == null) {
            return "N/A";
        }
        return user.isDeleted() ? "Deleted" : "Active";
    }

    public String displayUser(User user) {
        if (user == null) {
            return "User not found";
        }
        return String.format("User ID: %d%nUsername: %s%nEmail: %s%nCreated At: %s%nLast Login At: %s%nStatus: %s",
                user.getId(),
                user.getUsername(),
                maskEmail(user.getEmail()),
                formatDateTime(user.getCreatedAt()),
                formatDateTime(user.getLastLoginAt()),
                getUserStatus(user)
        );
    }
}
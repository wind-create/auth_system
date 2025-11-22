// src/main/java/com/core/auth/service/MailPort.java
package com.core.auth.service;

public interface MailPort {
  void send(String to, String subject, String body);
}

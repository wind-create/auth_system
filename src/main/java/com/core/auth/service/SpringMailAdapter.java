// src/main/java/com/core/auth/service/SpringMailAdapter.java
package com.core.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpringMailAdapter implements MailPort {
  private final JavaMailSender sender;

  @Value("${mail.enabled:false}") private boolean enabled;

  @Override
  public void send(String to, String subject, String body) {
    if (!enabled) {
      log.info("FAKE-MAIL to={} subj={} \n{}", to, subject, body);
      return;
    }
    SimpleMailMessage msg = new SimpleMailMessage();
    msg.setTo(to);
    msg.setSubject(subject);
    msg.setText(body);
    sender.send(msg);
  }
}

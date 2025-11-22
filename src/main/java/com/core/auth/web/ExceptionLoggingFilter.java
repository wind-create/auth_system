package com.core.auth.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class ExceptionLoggingFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    try {
      chain.doFilter(request, response);
    } catch (Throwable ex) {
      // Selalu log stacktrace + request info
      log.error("UNCAUGHT {} {} -> {}", request.getMethod(), request.getRequestURI(), ex.toString(), ex);
      // lempar lagi supaya handler/security tetap kerja (jadi 500/403 sesuai)
      if (ex instanceof ServletException se) throw se;
      if (ex instanceof IOException ioe) throw ioe;
      throw new ServletException(ex);
    }
  }
}

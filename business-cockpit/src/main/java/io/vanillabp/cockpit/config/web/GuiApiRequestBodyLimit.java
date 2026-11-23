package io.vanillabp.cockpit.config.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.unit.DataSize;
import org.springframework.web.filter.OncePerRequestFilter;

import io.vanillabp.cockpit.exceptions.BcContentTooLargeException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The largest request body the GUI API reads, set by
 * {@value #MAX_REQUEST_BODY_SIZE_PROPERTY} with a default of 1 MB. A larger body is answered with
 * {@code 413 Content Too Large}.
 * <p>
 * Neither Tomcat nor Spring limits the body of a JSON request by itself. The limit is a servlet
 * filter for {@code /gui/api/*} and not a setting of the server, because the BPMS API takes the
 * reports of the workflow modules, and their business data can be larger. The browser sends
 * small bodies only: a list request, a note, a setting.
 * <p>
 * A body which announces its length is refused before it is read. A body without a length, sent
 * in chunks, is counted while the controller reads it, and the request ends when the count goes
 * over the limit.
 */
@Configuration
public class GuiApiRequestBodyLimit {

  /**
   * The key which sets the limit, like {@code 1MB} or {@code 512KB}.
   */
  public static final String MAX_REQUEST_BODY_SIZE_PROPERTY = "business-cockpit.gui-api.max-request-body-size";

  /**
   * The limit when the key is not set.
   */
  public static final DataSize DEFAULT_MAX_REQUEST_BODY_SIZE = DataSize.ofMegabytes(1);

  /**
   * Creates the configuration. Spring creates it because of {@code @Configuration}.
   */
  public GuiApiRequestBodyLimit() {
  }

  /**
   * Registers the filter for the paths of the GUI API.
   *
   * @param environment Holds the configured limit
   * @return The registration
   */
  @Bean
  public FilterRegistrationBean<LimitFilter> guiApiRequestBodyLimitFilter(
      final Environment environment) {

    final var limit = Binder
        .get(environment)
        .bind(MAX_REQUEST_BODY_SIZE_PROPERTY, DataSize.class)
        .orElse(DEFAULT_MAX_REQUEST_BODY_SIZE);
    final var registration = new FilterRegistrationBean<>(new LimitFilter(limit.toBytes()));
    registration.addUrlPatterns(GuiApiContentSecurityPolicy.GUI_API_PATHS);
    return registration;

  }

  /**
   * Refuses or counts the body of a request to the GUI API.
   */
  public static class LimitFilter extends OncePerRequestFilter {

    private final long maxBytes;

    /**
     * Creates the filter.
     *
     * @param maxBytes The largest body, in bytes
     */
    public LimitFilter(
        final long maxBytes) {

      this.maxBytes = maxBytes;

    }

    @Override
    protected void doFilterInternal(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final FilterChain filterChain) throws ServletException, IOException {

      if (request.getContentLengthLong() > maxBytes) {
        response.setStatus(HttpStatus.CONTENT_TOO_LARGE.value());
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(tooLarge());
        return;
      }
      filterChain.doFilter(new CountingRequest(request), response);

    }

    private String tooLarge() {

      return "The request body is larger than %d bytes, the most the GUI API reads.".formatted(maxBytes);

    }

    /**
     * Counts the bytes of a body which did not announce its length.
     */
    private class CountingRequest extends HttpServletRequestWrapper {

      private ServletInputStream counted;

      CountingRequest(
          final HttpServletRequest request) {

        super(request);

      }

      @Override
      public ServletInputStream getInputStream() throws IOException {

        if (counted == null) {
          counted = new CountingInputStream(super.getInputStream());
        }
        return counted;

      }

    }

    private class CountingInputStream extends ServletInputStream {

      private final ServletInputStream body;

      private long bytesRead;

      CountingInputStream(
          final ServletInputStream body) {

        this.body = body;

      }

      @Override
      public int read() throws IOException {

        final var read = body.read();
        if (read >= 0) {
          count(1);
        }
        return read;

      }

      @Override
      public int read(
          final byte[] buffer,
          final int offset,
          final int length) throws IOException {

        final var read = body.read(buffer, offset, length);
        if (read > 0) {
          count(read);
        }
        return read;

      }

      private void count(
          final int read) {

        bytesRead += read;
        if (bytesRead > maxBytes) {
          throw new BcContentTooLargeException(tooLarge());
        }

      }

      @Override
      public boolean isFinished() {
        return body.isFinished();
      }

      @Override
      public boolean isReady() {
        return body.isReady();
      }

      @Override
      public void setReadListener(
          final ReadListener readListener) {
        body.setReadListener(readListener);
      }

    }

  }

}

package io.vanillabp.cockpit.commons.rest.adapter.oauth;

import java.io.IOException;
import java.util.Map;

import org.apache.oltu.oauth2.client.HttpClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthClientResponse;
import org.apache.oltu.oauth2.client.response.OAuthClientResponseFactory;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Sends the token requests of the OAuth client with OkHttp, so they use the same proxy, TLS and
 * logging settings as the REST client itself.
 */
public class OAuthOkHttpClient extends OkHttpClient implements HttpClient {

  private OkHttpClient client;

  /**
   * Creates a client with a plain OkHttp client, without proxy, TLS or logging settings.
   */
  public OAuthOkHttpClient() {

    this.client = new OkHttpClient();

  }

  /**
   * Creates a client which sends the requests with the given OkHttp client.
   *
   * @param client The OkHttp client, already configured
   */
  public OAuthOkHttpClient(
      final OkHttpClient client) {

    this.client = client;

  }

  @Override
  public <T extends OAuthClientResponse> T execute(
      final OAuthClientRequest request,
      final Map<String, String> headers,
      final String requestMethod,
      final Class<T> responseClass) throws OAuthSystemException, OAuthProblemException {

    var mediaType = MediaType.parse("application/json");
    var requestBuilder = new Request.Builder().url(request.getLocationUri());
    if (headers != null) {
      for (var entry : headers.entrySet()) {
        if (entry.getKey().equalsIgnoreCase("Content-Type")) {
          mediaType = MediaType.parse(entry.getValue());
        } else {
          requestBuilder.addHeader(entry.getKey(), entry.getValue());
        }
      }
    }

    var body = request.getBody() != null ? RequestBody.create(request.getBody(), mediaType) : null;
    requestBuilder.method(requestMethod, body);

    try {
      var response = client.newCall(requestBuilder.build()).execute();
      final var responseBody = response.body();
      return OAuthClientResponseFactory
          .createCustomResponse(responseBody != null ? responseBody.string() : null,
              responseBody != null && responseBody.contentType() != null
                  ? responseBody.contentType().toString()
                  : null,
              response.code(),
              // 1.0.2 of the client asks for the response headers as well, and okhttp
              // hands them over in exactly the shape the factory wants
              response.headers().toMultimap(),
              responseClass);
    } catch (IOException e) {
      throw new OAuthSystemException(e);
    }

  }

  @Override
  public void shutdown() {
    /* do nothing */
  }

}

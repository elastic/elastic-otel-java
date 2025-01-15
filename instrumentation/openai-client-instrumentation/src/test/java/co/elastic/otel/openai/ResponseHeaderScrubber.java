package co.elastic.otel.openai;

import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

public class ResponseHeaderScrubber implements ResponseTransformerV2 {
    @Override
    public String getName() {
        return "scrub-response-header";
    }

    @Override
    public Response transform(Response response, ServeEvent serveEvent) {
        HttpHeaders scrubbed = HttpHeaders.noHeaders();
        for (HttpHeader header : response.getHeaders().all()) {
            switch (header.key()) {
                case "Set-Cookie":
                    scrubbed = scrubbed.plus(HttpHeader.httpHeader("Set-Cookie", "test_set_cookie"));
                    break;
                case "openai-organization":
                    scrubbed = scrubbed.plus(HttpHeader.httpHeader("openai-organization", "test_openai_org_id"));
                    break;
                default:
                    scrubbed = scrubbed.plus(header);
                    break;
            }
        }
        return Response.Builder.like(response)
                .but().headers(scrubbed)
                .build();
    }
}

package com.extremecloudiq.inlets.tests.startup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;

/**
 * @author dprasad
 */
@Slf4j
@Configuration
public class NOSSOverInletsTest extends AbstractTest {
    @Value("${inlets.tests.noss.enable:true}")
    private boolean enabled;

    @Value("${inlets.tests.noss.url:https://inlets-server:8080/rest/openapi/v0/configuration}")
    private String baseUrl;

    @Value("${inlets.tests.noss.pool.size:25}")
    private int poolSize;

    @Value("${inlets.tests.noss.max.devices:50000}")
    private int maxDevices;

    @Value("${inlets.tests.noss.wait.time:180000}")
    private long waitTime;

    public void execute(String serialNumber) {
        long st = 0L;
        String resp;
        try {
            WebClient client = WebClient.builder().
                baseUrl(baseUrl).
                defaultHeaders(httpHeaders -> httpHeaders.addAll(createHeaders(serialNumber))).
                build();

            st = System.currentTimeMillis();
            resp = client.get()
                .exchange()
//                    .doOnSuccess(httpResponse -> System.out.println(httpResponse.statusCode()))
//                    .flatMap(clientResponse -> clientResponse.bodyToMono(String.class))

//                        .retrieve()
//                        .onStatus(HttpStatus::is4xxClientError, response1 -> response1.bodyToMono(String.class).map(Exception::new))
//                        .onStatus(HttpStatus::is5xxServerError, response1 -> response1.bodyToMono(String.class).map(Exception::new))
//                        .onStatus(HttpStatus::isError, response1 -> response1.bodyToMono(String.class).map(Exception::new))
//                        .bodyToMono(String.class)

                .block()
                .statusCode() + "";


        } catch (Exception e) {
            resp = e.getMessage();
        }

        logResponse(log, resp, st, serialNumber);

    }

    private HttpHeaders createHeaders(String sn) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.HOST, "openapi." + sn);
        headers.add(HttpHeaders.AUTHORIZATION, "Basic YWxleDptZXJI");
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.add("x-api-user", "xiq-config");

        return headers;
    }

    @PostConstruct
    public void init() {
        setValuesAndStartTests(log, enabled, poolSize, maxDevices, waitTime);

    }
}
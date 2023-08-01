package com.extremecloudiq.inlets.tests.startup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.xml.ws.http.HTTPException;
import java.time.Duration;

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

    @Override
    public String getName() {
        return "COI";
    }

    public void execute(String serialNumber) {
        long st = 0L;
        String resp;
        try {
            WebClient client = WebClient.builder().
                baseUrl(baseUrl).
                defaultHeaders(httpHeaders -> httpHeaders.addAll(createHeaders(serialNumber))).
                build();

            st = System.currentTimeMillis();
            client.get()
                .retrieve()
                .onStatus(HttpStatus::isError, response1 -> Mono.error(new HTTPException(response1.statusCode().value())))

                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(20))
                .block();
            resp = "200";

        } catch (HTTPException e) {
            resp = e.getStatusCode()+"";
        }catch (Exception e) {
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
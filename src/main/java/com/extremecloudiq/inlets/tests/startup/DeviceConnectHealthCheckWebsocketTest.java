package com.extremecloudiq.inlets.tests.startup;

import com.extremecloudiq.inlets.tests.vo.DeviceConnectBaseRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
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
public class DeviceConnectHealthCheckWebsocketTest extends AbstractTest {

    @Value("${inlets.tests.hc.ws.enable:true}")
    private boolean enabled;

    @Value("${inlets.tests.hc.ws.url:http://device-connector:9921/device-connect/rest/v1/health-check/}")
    private String healthCheckBaseUrl;

    @Value("${inlets.tests.hc.ws.pool.size:25}")
    private int poolSize;

    @Value("${inlets.tests.hc.ws.max.devices:50000}")
    private int maxDevices;

    @Value("${inlets.tests.hc.ws.wait.time:180000}")
    private long waitTime;

    @Value("${inlets.tests.hc.ws.host-header:device-connector}")
    private String hostHeader;

    RestTemplate restTemplate = new RestTemplate();

    public void execute(String serialNumber) {
        DeviceConnectBaseRequest request = new DeviceConnectBaseRequest();
        request.setDeviceFamily("VSP_SERIES");
        request.setDeviceModel("VOSS5520");
        request.setInletsVersion("0.1.0.0");
        request.setPlatform("VOSS5520");

        long st = 0L;
        String resp;
        try {
            st = System.currentTimeMillis();

            WebClient client = WebClient.builder().
                baseUrl(healthCheckBaseUrl + serialNumber).
                defaultHeaders(httpHeaders -> httpHeaders.addAll(createHeaders(serialNumber))).
                build();

            st = System.currentTimeMillis();
            client.post()
                .body(BodyInserters.fromObject(request))
                .retrieve()
                .onStatus(HttpStatus::isError, response1 -> Mono.error(new HTTPException(response1.statusCode().value())))

                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(20))
                .block();
            resp = "200";

        } catch (Exception e) {
            resp = e.getMessage();
        }

        logResponse(log, resp, st, serialNumber);

    }

    private HttpHeaders createHeaders(String sn) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.HOST, hostHeader);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        return headers;
    }

    @PostConstruct
    public void init() {
        setValuesAndStartTests(log, enabled, poolSize, maxDevices, waitTime);
    }
}
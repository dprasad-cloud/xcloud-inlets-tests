package com.extremecloudiq.inlets.tests.startup;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author dprasad
 */
@Slf4j
@Configuration
public class NOSSVersionOverInletsTest {
    @Value("${inlets.tests.noss.version.enable:true}")
    private boolean enabled;

    @Value("${inlets.tests.noss.version.url:https://inlets-server:8080/rest/openapi/v0/configuration}")
    private String baseUrl;

    @Value("${inlets.tests.serial.prefix:IQEMU}")
    private String serialPrefix;

    @Value("${inlets.tests.noss.version.pool.size:25}")
    private int poolSize;

    @Value("${inlets.tests.noss.version.max.devices:50000}")
    private int maxDevices;

    @Value("${inlets.tests.noss.version.wait.time:180000}")
    private long waitTime;

    class Runner extends Thread {
        int start = 0;
        int poolSize = 0;

        public Runner(int start, int max) {
            this.start = start;
            this.poolSize = max;
        }

        public void run() {

            int startIndex = (maxDevices / poolSize) * start;
            int lastIndex = (startIndex + maxDevices / poolSize);
            for (int i = startIndex; i < lastIndex; i++) {


                String serialNumber = String.format(serialPrefix , i);
//                String serialNumber = "2021Q-30238";
                System.out.println("serial:"+serialNumber);
                long st = 0L;
                String resp = null;

                try {
                    long st1 = System.currentTimeMillis();
                    WebClient client = WebClient.builder().
                        baseUrl(baseUrl).
                        defaultHeaders(httpHeaders -> httpHeaders.addAll(createHeaders(serialNumber))).
                        build();
                    long et1 = System.currentTimeMillis();
                    System.out.println("Time taken to build:" + (et1 - st1));

                    st = System.currentTimeMillis();
                    client.
                        get().
                        retrieve()

                        .onStatus(HttpStatus::is4xxClientError, response1 -> response1.bodyToMono(String.class).map(Exception::new))
                        .onStatus(HttpStatus::is5xxServerError, response1 -> response1.bodyToMono(String.class).map(Exception::new))
                        .onStatus(HttpStatus::isError, response1 -> response1.bodyToMono(String.class).map(Exception::new))

                        .bodyToMono(String.class)
                        .block();

                } catch (Exception e) {
                    e.printStackTrace();
                    resp = e.getMessage();
                }

                resp = resp!=null?resp.replaceAll(" ", ""):resp;
                long et = System.currentTimeMillis();
                log.debug("resp: " + resp);
                log.info(serialNumber +"="+ resp + " | " + (et - st));

            }
        }

        private HttpHeaders createHeaders(String sn) {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.HOST, "openapi." + sn);
            headers.add(HttpHeaders.AUTHORIZATION, "Basic YWxleDptZXJI");
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            headers.add("x-api-user", "xiq-config");

            return headers;
        }
    }

    @PostConstruct
    public void init() {

        if (!enabled) {
            log.info("noss.version tests not enabled.");
            return;
        }
        new Thread() {

            @SneakyThrows
            @Override
            public void run() {
                while (true) {
                    long t1 = System.currentTimeMillis();
                    ExecutorService service = Executors.newFixedThreadPool(poolSize);
                    for (int j = 0; j < poolSize; j = j + 1) {
                        service.submit(new Runner(j, poolSize));
                    }

                    System.out.println("Tasks submitted: time taken = " + (System.currentTimeMillis() - t1));
                    service.shutdown();
                    service.awaitTermination(waitTime, TimeUnit.MILLISECONDS);
                    System.out.println("completed current iteration, time taken = " + (System.currentTimeMillis() - t1));
                }

            }
        }.start();

    }
}
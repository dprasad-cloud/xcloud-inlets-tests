package com.extremecloudiq.inlets.tests.startup;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SocketFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author dprasad
 */
@Slf4j
@Configuration
public class SshOverInletsTest {
    @Value("${inlets.tests.serial.prefix:IQEMU}")
    private String serialPrefix;

    @Value("${inlets.tests.serial.realdevices:2021Q-30238}")
    private String realDevices;

    @Value("${inlets.tests.ssh.enable:true}")
    private boolean enabled;

    @Value("${inlets.tests.ssh.url:tcp://inlets-server:8090}")
    private String baseUrl;

    @Value("${inlets.tests.ssh.pool.size:25}")
    private int poolSize;

    @Value("${inlets.tests.ssh.max.devices:50000}")
    private int maxDevices;

    @Value("${inlets.tests.ssh.wait.time:180000}")
    private long waitTime;

    @Value("${inlets.tests.ssh.username:guest}")
    private String username;

    @Value("${inlets.tests.ssh.password:guest}")
    private String password;

    @Value("${inlets.tests.ssh.commands:cat /etc/hosts; echo \"--Executing ls--\"; ls}")
    private String commands;

    @Value("${inlets.tests.sleep.time:1000}")
    private long sleepTime;

    class Runner extends Thread {
        int start;
        int poolSize;
        Socket staticSocket;
        URI uri;

        public Runner(int start, int max) {
            this.start = start;
            this.poolSize = max;
        }

        public void run() {

            int startIndex = (maxDevices / poolSize) * start;
            int lastIndex = (startIndex + maxDevices / poolSize);

            //Handle realDevices
            //Only execute in the first thread
            if(start == 0){
                List<String> realDevicesList = Arrays.stream(realDevices.split(",")).
                    filter(s -> s.length() >0).
                    collect(Collectors.toList());
                for(String serialNumber: realDevicesList)
                    execute(serialNumber);

            }

            for (int i = startIndex; i < lastIndex; i++) {

                String serialNumber = String.format(serialPrefix , i);
                execute(serialNumber);

            }
        }

        private void execute(String serialNumber){
            OutputStream outputStream;
            long st = 0L;
            String resp = null;
            try {

                uri = new URI(baseUrl);
                staticSocket = new Socket(uri.getHost(), uri.getPort());
                outputStream = staticSocket.getOutputStream();

                st = System.currentTimeMillis();
                final PrintWriter writer = new PrintWriter(outputStream, true);
                writer.println("GET / HTTP/1.1");
                writer.println("Host: ssh." + serialNumber);
                writer.println("Content-Type: application/json");
                writer.println("Connection: Upgrade");
                writer.println("Upgrade: tcp");

                writer.println();
                writer.flush();

                listFolderStructureShell();
                resp = "OK";

            } catch (Exception e) {
                e.printStackTrace();
                resp = e.getMessage();
            }

            resp = resp != null ? resp.replaceAll(" ", "") : null;
            long et = System.currentTimeMillis();
            log.debug("resp: " + resp);
            log.info(serialNumber +"="+ resp + " | " + (et - st));

            try{
                staticSocket.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        public void listFolderStructureShell() throws Exception {

            Session session;
            ChannelShell channel;

            session = new JSch().getSession(username, uri.getHost(), uri.getPort());
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");

            SocketFactory sfactory = new MySocketFactory();
            session.setSocketFactory(sfactory);
            session.connect();

            channel = (ChannelShell) session.openChannel("shell");
            channel.setInputStream(null);
            channel.setOutputStream(null);

            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream(pos, 1024);

            channel.setInputStream(pis);
            ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
            channel.setOutputStream(responseStream);

            channel.connect(3000);
            while (channel.isConnected() && !channel.isClosed()) {

                String[] commands = SshOverInletsTest.this.commands.split(";");
                for(String commandStr: commands){
                    System.out.println("Command="+commandStr);
                    pos.write((commandStr + " \n").getBytes(StandardCharsets.UTF_8));
                    pos.flush();

                    Thread.sleep(2000);
                    System.out.println(responseStream);
                }

                Thread.sleep(2000);

                pos.write("exit \n".getBytes(StandardCharsets.UTF_8));
                pos.flush();
                break;
            }

            try{
                channel.disconnect();
                session.disconnect();
            }catch(Exception ignored){}
        }

        class MySocketFactory implements SocketFactory {

            @Override
            public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
                return staticSocket;
            }

            @Override
            public InputStream getInputStream(Socket socket) throws IOException {
                return socket.getInputStream();
            }

            @Override
            public OutputStream getOutputStream(Socket socket) throws IOException {
                return socket.getOutputStream();
            }

        }
    }

    @PostConstruct
    public void init() {

        if (!enabled) {
            log.info("SSH tests not enabled.");
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

                    Thread.sleep(sleepTime);
                }

            }
        }.start();

    }
}
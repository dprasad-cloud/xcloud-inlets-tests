package com.extremecloudiq.inlets.tests.startup;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SocketFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/**
 * @author dprasad
 */
@Slf4j
@Configuration
public class SshOverInletsTest extends AbstractTest {

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

    Socket staticSocket;
    URI uri;

    @Override
    public String getName() {
        return "SSH";
    }

    public void execute(String serialNumber) {
        OutputStream outputStream;
        long st = 0L;
        String resp;
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

        logResponse(log, resp, st, serialNumber);

        try {
            staticSocket.close();
        } catch (Exception e) {
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
            for (String commandStr : commands) {
                System.out.println("Command=" + commandStr);
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

        try {
            channel.disconnect();
            session.disconnect();
        } catch (Exception ignored) {
        }
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

    @PostConstruct
    public void init() {
        setValuesAndStartTests(log, enabled, poolSize, maxDevices, waitTime);

    }
}
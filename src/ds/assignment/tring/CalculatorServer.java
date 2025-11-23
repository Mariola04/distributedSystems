package ds.assignment.tring;

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Calculator server using raw sockets 
 */
public class CalculatorServer {
    
    private final int port;
    private ServerSocket serverSocket;
    
    public CalculatorServer(int port) {
        this.port = port;
    }
    
    public void start() throws Exception {
        serverSocket = new ServerSocket(port);
        System.out.printf("Calculator server running on port %d\n", port);
        
        while (true) {
            Socket clientSocket = serverSocket.accept();
            String clientAddress = clientSocket.getInetAddress().getHostAddress();
            System.out.printf("New connection from %s\n", clientAddress);
            new Thread(new RequestHandler(clientAddress, clientSocket)).start();
        }
    }
    
    public void stop() throws Exception {
        if (serverSocket != null) serverSocket.close();
    }
    
    private class RequestHandler implements Runnable {
        private final String clientAddress;
        private final Socket clientSocket;
        
        public RequestHandler(String clientAddress, Socket clientSocket) {
            this.clientAddress = clientAddress;
            this.clientSocket = clientSocket;
        }
        
        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                
                String command;
                while ((command = in.readLine()) != null) {
                    System.out.printf("Request from %s: %s\n", clientAddress, command);
                    
                    // Parse command: "add:5.2:3.1"
                    Scanner sc = new Scanner(command).useDelimiter(":");
                    String op = sc.next();
                    double x = Double.parseDouble(sc.next());
                    double y = Double.parseDouble(sc.next());
                    
                    // Calculate result (professor's exact logic)
                    double result = 0.0;
                    switch (op) {
                        case "add": result = x + y; break;
                        case "sub": result = x - y; break;
                        case "mul": result = x * y; break;
                        case "div": result = x / y; break;
                    }
                    
                    out.println(String.valueOf(result));
                }
            } catch (Exception e) {
                System.err.println("Error handling client: " + e.getMessage());
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9000;
        new CalculatorServer(port).start();
    }
}
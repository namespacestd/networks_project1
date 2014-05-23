import java.net.*;
import java.io.*;

public class Client {
    public static void main(String[] args) {
        //Ensures that the command line arguments given to the Client class are correct
        if(args.length != 2) {
            System.out.println("Usage: java Client <ip_address> <port>");
            return;
        }
        String targetIpAddress = args[0];
        Integer targetPort = null;

        try {
            targetPort = Integer.parseInt(args[1]);
        }
        catch(Exception ex) {
            System.out.println("Error: Invalid port number.");
            return;
        }

        try {
            //Opens up a connection to the target ip address and port taken in as command line arguments. 
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            Socket sock = new Socket(targetIpAddress, targetPort);
            PrintWriter out = new PrintWriter(sock.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            
            String input = null;

            /*Loops until the socket connection is closed. Prints to the console the messages recieved from the 
            Server before blocking to wait for user input. Any messages received from ther Server during this time
            is not printed to the console because console is blocked waiting for user input. Any messages from the
            Server aren't printed until some form of user input is entered. (e.g. a newline) */
            while((input = in.readLine()) != null) {
                System.out.print(input.replace("%n", "\n"));
                
                if(in != null && !in.ready()) {
                    String output = userInput.readLine();
                    out.println(output);
                    out.flush();
                }
            }
            sock.close();
        }
        catch(Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }
}
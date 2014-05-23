import java.net.*;
import java.io.*;
import java.util.Hashtable;
import java.util.ArrayList;

public class Server extends Thread {
    //This class is used to keep track of blocked user/ip pairs, and when they got blocked.
    protected class Pair {
        protected Long time;
        protected String user;

        protected Pair(Long time, String user) {
            this.time = time;
            this.user = user;
        } 
    }
    //Holds the username/password combinations found in user_pass.txt
    private static Hashtable<String,String> validUserPass;
    //Holds the currently blocked user/ip pairs and when they got blocked.
    private static Hashtable<InetAddress, Pair> blockedUsers = new Hashtable<InetAddress, Pair>();
    //Holds the usernames of people currently logged into the chat server
    private static ArrayList<String> currentConnected = new ArrayList<String>();
    //Holds the last time that people logged on
    private static Hashtable<String, Long> lastLoggedOn = new Hashtable<String, Long>();
    //Holds any messages that need to be sent to users the next time they log on
    private static Hashtable<String, String> offlineMessages = new Hashtable<String, String>();
    //Holds the user blocked users
    private static Hashtable<String, ArrayList<String>> userBlockedUsers = new Hashtable<String, ArrayList<String>>();
    //Holds all the currently running threads of the Server object
    private static ArrayList<Server> runningThreads = new ArrayList<Server>();
    //The server socket
    private static ServerSocket socket = null;

    private static Hashtable <String, ArrayList<String>> friends = new Hashtable<String, ArrayList<String>>();
    private static Hashtable <String, ArrayList<String>> pendingFriendRequests = new Hashtable<String, ArrayList<String>>();

    //Measured in seconds
    private static final int BLOCK_TIME = 60;

    //Measured in minutes
    private static final int LAST_HOUR = 60;

    //Measured in minutes
    private static final int TIME_OUT = 60;

    //The specific socket associated with this instance of Server and the client that it's connected to
    private Socket sock = null;
    //The user that this instance of the Server is associated with
    private String currentUser = null;
    //The output stream to the client for a specific instance of Server
    private PrintWriter out = null;

    private static int PORT = 4119;

    public Server() {

    }

    public String getUser() {
        return currentUser;
    }
    public PrintWriter getOutput() {
        return out;
    }

    /* Creates a socket listening on port
    */
    public void run() {
        try {
            System.out.println("Starting program");
            Server.runningThreads.add(this);
            if(Server.socket == null)
                Server.socket = new ServerSocket(Server.PORT);    
            System.out.println("Socket created. Waiting for request...");
            sock = Server.socket.accept();
            InetAddress currentClient = sock.getInetAddress();
            Server threadedServer = new Server();
            threadedServer.start();

            out = new PrintWriter(sock.getOutputStream());

            System.out.println("Request recieved from: " + sock.getInetAddress());

            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

            String input, output, currentPassword = null, password;
            int numTries = 0;
            boolean usernameRecieved = false, correctPassword = false;
            out.println("Username: ");
            out.flush();
 
            //Username/Password Verification
            while((input = in.readLine()) != null) {
                //Socket timeout
                sock.setSoTimeout(Server.TIME_OUT * 1000 * 60);

                if(!usernameRecieved) {
                    currentUser = validUserPass.get(input);

                    if(currentUser == null) {
                        out.println("Username not found in our database. Try another. %n");
                        out.println("Username: ");
                        out.flush();
                    }
                    else {
                        //Check if username/ip address on blocked list. If on blocked list, tell the client and close the connection.
                        if(Server.blockedUsers.get(currentClient) != null && Server.blockedUsers.get(currentClient).user.equals(input)) {
                            System.out.println("Blocked user attempt");
                            Long currentTime = System.currentTimeMillis()/1000;
                            Long blockTime = Server.blockedUsers.get(currentClient).time;

                            Long elapsedTime = currentTime - blockTime;
                            if(elapsedTime >= Server.BLOCK_TIME)
                                Server.blockedUsers.remove(currentClient);
                            else {
                                out.print("You are blocked from the system for too many incorrect password attempts. Please try again in ");
                                out.println((Server.BLOCK_TIME - elapsedTime) + " seconds.%nServer disconnecting.%n");
                                out.flush();
                                sock.close();
                                return;
                            }                
                        }
                        // Only allow one instance of a user to be logged into the system at once. (e.g. A user cannot log on multiple terminals)
                        if(Server.currentConnected.contains(input)) {
                            out.println("User '" + input + "' is already logged into the system. Users may only log in on a single console.%nServer disconnecting.%n");
                            out.flush();
                            break;
                        }
                        else {
                            usernameRecieved = true;
                            currentUser = input;
                            currentPassword = validUserPass.get(currentUser);
                            out.println("Password: ");
                            out.flush();
                        }
                    }
                }
                else if(!correctPassword) {
                    // Same as above to handle the case where if the user got to the password screen on both terminals.
                    if(Server.currentConnected.contains(currentUser)) {
                        out.println("User '" + currentUser + "' is already logged into the system. Users may only log in on a single console.%nServer disconnecting.%n");
                        out.flush();
                        break;
                    }

                    if(input.equals(currentPassword)) {
                        correctPassword = true;
                        Server.currentConnected.add(currentUser);
                        Server.lastLoggedOn.remove(currentUser);
                        out.print("Welcome to simple chat server!%n\n");
                        
                        String offlineMessage = Server.offlineMessages.get(currentUser);
                        if(offlineMessage != null) {
                            out.println(offlineMessage);
                            Server.offlineMessages.remove(currentUser);
                        }
                        out.println("%n");

                        if(Server.pendingFriendRequests.get(currentUser) != null) {
                            for(String user : Server.pendingFriendRequests.get(currentUser)) 
                                out.println("You have a friend request pending from '" + user + "'.%n");
                            out.println("%n");                
                        }
                        out.println("Command: ");
                        out.flush();
                    }
                    //Block the user after too many (3+) incorrect password attempts.
                    else {
                        out.print("Invalid Password. Try again.%n\n");
                        numTries++;
                        if(numTries >= 3) {
                            out.println("Too many incorrect password attempts. You will be blocked from the system.%nServer disconnecting.%n");
                            Server.blockedUsers.put(currentClient, new Pair(System.currentTimeMillis()/1000, currentUser));
                            out.flush();
                            break;
                        }
                        else {
                            out.println("Password: ");
                            out.flush();
                        }
                    }
                }
                else {
                    if(input.equalsIgnoreCase("")) {
                        out.println("");
                        out.flush();
                    }
                    //Tells the current user all the valid command
                    else if(input.equalsIgnoreCase("commands")) {
                        out.println("whoelse - prints all other users who are currently logged in%n");
                        out.println("wholasthr - prints all the users who have been on in the last hour%n");
                        out.println("broadcast <message> - sends a public message to everyone that is currently logged on%n");
                        out.println("message <user> <message> - sends a private message to a target user, if ablen%");
                        out.println("block <user> - blocks a user from being able to private message you%n");
                        out.println("unblock <user> - unblocks a user from being unable to private message you%n");
                        out.println("friend <user> - sends a friend request (user must accept) to target user. You are notified whenever someone on your friend's list logs on%n");
                        out.println("reject <user> - rejects a currently pending friend request%n");
                        out.println("pending - lists all the friend requests waiting for your response%n");
                        out.println("friendlist - lists all your friends and their current status%n%n");
                        out.flush();
                    }
                    //Lists the status of all the users on one's friendlist
                    else if(input.equalsIgnoreCase("friendlist")) {
                        ArrayList<String> friends = Server.friends.get(currentUser);

                        if(friends != null) {
                            out.println("Friend List: %n");
                            for(String friend : friends) {
                                out.println(friend + " (");
                                if(Server.currentConnected.contains(friend))
                                    out.println("online)%n");
                                else
                                    out.println("offline)%n");
                            }
                            out.println("%n");
                            out.flush();
                        }
                        else {
                            out.println("You do not have any friends. %n%n");
                            out.flush();
                        }
                    }
                    //Lists all the current pending friend requests waiting for the current user's response
                    else if(input.equalsIgnoreCase("pending")) {
                        ArrayList<String> pending = Server.pendingFriendRequests.get(currentUser);

                        if(pending != null) {
                            if(pending.size() == 0) 
                                out.println("You have no pending friend request.%n%n");
                            else {                          
                                out.println("Currently Pending Friend Requests: ");
                                for(String user : Server.pendingFriendRequests.get(currentUser)) {
                                    out.println(user + " ");
                                }
                                out.println("%n");
                            }
                            out.flush();
                        }
                        else {
                            out.println("You do not have any pending friend requests.%n%n");
                            out.flush();
                        }
                    }
                    //Logs the current user out and closes the socket.
                    else if(input.equalsIgnoreCase("logout")) {
                        out.println("Logging out...%nServer disconnecting.%n");
                        out.close();
                        in.close();
                        break;
                    }
                    //Send to the client connected the other users that are currently logged in
                    else if(input.equalsIgnoreCase("whoelse")) {
                        for(String user : currentConnected)
                            if(!user.equals(currentUser))
                                out.println(user + "%n");
                        out.println("%n%n");
                        out.flush();
                    }
                    //Send to the client connected the people who have been online in the last hour
                    else if(input.equalsIgnoreCase("wholasthr")) {
                        Long currentTime = System.currentTimeMillis()/1000;
                        ArrayList<String> users = new ArrayList<String>();

                        for(String user : Server.currentConnected)
                            users.add(user);

                        for(String key : Server.lastLoggedOn.keySet()) {
                            Long time = lastLoggedOn.get(key);
                            if((currentTime-time) <= (60 * Server.LAST_HOUR))
                                users.add(key);
                        }

                        for(String user : users) {
                            if(!user.equals(currentUser))
                                out.println(user + "%n");
                        }
                        out.println("%n");
                        out.flush();
                    }
                    else {
                        //Ensures that the broadcast, message, block, and unblock commands are called with the correct number of variables.
                        String[] splitInput = input.split(" ");
                        if(splitInput.length==1) {
                            if(splitInput[0].trim().equals("broadcast")) {
                                out.println("Please enter a message to broadcast.%n");
                                out.println("Usage: broadcast <message>%n%n");
                                out.flush();
                            }
                            else if(splitInput[0].trim().equals("message")) {
                                out.println("Please enter a target user and message.%n");
                                out.println("Usage: message <user> <message>%n%n");
                                out.flush();
                            }
                            else if(splitInput[0].trim().equals("block")) {
                                out.println("Please enter a target user to block.%n");
                                out.println("Usage: block <user>%n%n");
                                out.flush();
                            }
                            else if(splitInput[0].trim().equals("unblock")) {
                                out.println("Please enter a target user to unblock.%n");
                                out.println("Usage: unblock <user>%n%n");
                                out.flush();
                            }
                            else if(splitInput[0].trim().equals("friend")) {
                                out.println("Please enter a target user to friend.%n");
                                out.println("Usage: friend <user>%n%n");
                                out.flush();
                            }
                            else {
                                out.println("Unknown Command.%n%n");
                                out.flush();
                            }
                        }
                        else {
                            //Broadcasts to all the currently connected clients some message (including the client who did the broadcast)
                            if(splitInput[0].equals("broadcast")) {
                                String finalMessage = "";
                                for(int i=1; i<splitInput.length; i++)
                                    finalMessage += splitInput[i] + " ";
                            
                                for(Server s : Server.runningThreads) {
                                    if(s.getUser() != null) {
                                        PrintWriter targetOutput = s.getOutput();
                                        targetOutput.println(currentUser+": "+finalMessage + "%n%n");
                                        targetOutput.flush();
                                    }
                                }
                                out.flush();
                            }
                            //Handles the friend command
                            else if(splitInput[0].equals("friend")) {
                                if(splitInput.length !=2) {
                                    out.println("Please enter a target user to friend.%n");
                                    out.println("Usage: friend <user>%n%n");
                                    out.flush();
                                }
                                else {
                                    String targetUser = splitInput[1];

                                    if(targetUser.equals(currentUser)) {
                                        out.println("Error! You cannot friend yourself!%n%n");
                                        out.flush();
                                    }
                                    else {
                                        if(Server.validUserPass.get(targetUser) != null) {
                                            ArrayList<String> currentFriends = Server.friends.get(currentUser); 
                                            if(currentFriends != null && currentFriends.contains(targetUser)) {
                                                out.println("You are already friends with '" + targetUser + "'!%n");
                                                out.flush();
                                            }
                                            else {
                                                ArrayList<String> friendRequestAccept = Server.pendingFriendRequests.get(currentUser);
                                                if(friendRequestAccept != null && friendRequestAccept.contains(targetUser)) {
                                                    if(Server.friends.get(currentUser) == null) {
                                                        ArrayList<String> addFriend = new ArrayList<String>();
                                                        addFriend.add(targetUser);
                                                        Server.friends.put(currentUser, addFriend);
                                                    }
                                                    else
                                                        Server.friends.get(currentUser).add(targetUser);
                                                    
                                                    if(Server.friends.get(targetUser) == null) {
                                                        ArrayList<String> addFriend = new ArrayList<String>();
                                                        addFriend.add(currentUser);
                                                        Server.friends.put(targetUser, addFriend);
                                                    }
                                                    else
                                                        Server.friends.get(targetUser).add(currentUser);
                                                    
                                                    if(Server.pendingFriendRequests.get(currentUser) == null) 
                                                        Server.pendingFriendRequests.put(currentUser, new ArrayList<String>());
                                                    else 
                                                        Server.pendingFriendRequests.get(currentUser).remove(targetUser);
                                                    
                                                    if(Server.pendingFriendRequests.get(targetUser) == null)
                                                        Server.pendingFriendRequests.put(targetUser, new ArrayList<String>());
                                                    else
                                                        Server.pendingFriendRequests.get(targetUser).remove(currentUser);
                                                    
                                                    out.println("You accepted the friend request from '" + targetUser + "'!%n%n");
                                                    out.flush();

                                                    for(Server s : Server.runningThreads) {
                                                        if(s.getUser() != null && s.getUser().equals(targetUser)) {
                                                            PrintWriter p = s.getOutput();
                                                            p.println("'" + currentUser + "' just accepted your friend request.%n%n");
                                                            p.flush();
                                                            break;
                                                        }
                                                    }
                                                }
                                                else {
                                                    if(Server.pendingFriendRequests.get(targetUser) == null) 
                                                        Server.pendingFriendRequests.put(targetUser, new ArrayList<String>());
                                                    Server.pendingFriendRequests.get(targetUser).add(currentUser);
                                                    out.println("You have sent '" + targetUser + "' a friend request.%n%n");
                                                    out.flush();
                                                
                                                    for(Server s : Server.runningThreads) {
                                                        if(s.getUser() != null && s.getUser().equals(targetUser)) {
                                                            PrintWriter p = s.getOutput();
                                                            p.println("You just recieved a friend request from '" + currentUser + "'.%n%n");
                                                            p.flush();
                                                            break;        
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        else {
                                            out.println("Target user '" + targetUser + "' does not exist in our system. Please check the username.%n%n");
                                            out.flush();
                                        }
                                    }
                                }
                                
                            }
                            //Allows a user to block private messages from another user
                            else if(splitInput[0].equals("block")) {
                                if(splitInput.length !=2) {
                                    out.println("Please enter a target single user to block.%n");
                                    out.println("Usage: block <user>%n%n");
                                    out.flush();
                                }
                                else {
                                    String targetUser = splitInput[1];

                                    if(targetUser.equals(currentUser)) {
                                        out.println("Error! You cannot block yourself!%n%n");
                                        out.flush();
                                    }
                                    else {
                                        if(Server.validUserPass.get(targetUser) != null) {
                                            ArrayList<String> blockedUsers;
                                            if(Server.userBlockedUsers.get(currentUser) == null)
                                                blockedUsers = new ArrayList<String>();
                                            else
                                                blockedUsers = Server.userBlockedUsers.get(currentUser);

                                            blockedUsers.add(targetUser);
                                            Server.userBlockedUsers.put(currentUser, blockedUsers);
                                            out.println("You have successfully blocked '" + targetUser + "' from sending you messages.%n%n");
                                            out.flush();
                                        }
                                        else {
                                            out.println("Target user '" + targetUser + "' does not exist in our system. Please check the username.%n%n");
                                            out.flush();
                                        }
                                    }    
                                }
                            }
                            //Allows a user to unblock private messages from another user
                            else if(splitInput[0].equals("unblock")) {
                                if(splitInput.length !=2) {
                                    out.println("Please enter a target single user to unblock.%n");
                                    out.println("Usage: unblock <user>%n%n");
                                    out.flush();
                                }
                                else {
                                    String targetUser = splitInput[1];
                                    if(Server.validUserPass.get(targetUser) != null) {
                                        ArrayList<String> blockedUsers = Server.userBlockedUsers.get(currentUser);
                                        if(blockedUsers != null && blockedUsers.contains(targetUser)) {
                                            blockedUsers.remove(targetUser);
                                            Server.userBlockedUsers.put(currentUser, blockedUsers);
                                            out.println("You have successfully unblocked '" + targetUser + "'.%n%n");
                                            out.flush();
                                        }
                                        else {
                                            out.println("Error: Target user was not on your blocked list.%n%n");
                                            out.flush();
                                        }
                                    }
                                    else {
                                        out.println("Target user '" + targetUser + "' does not exist in our system. Please check the username.%n%n");
                                        out.flush();
                                    }
                                }
                            }
                            //Allows a user to send a private message another user
                            else if(splitInput[0].equals("message")) {
                                if(splitInput.length <=2) {
                                    out.println("Please enter a target user and message.%n");
                                    out.println("Usage: message <user> <message>%n%n");
                                    out.flush();
                                }
                                else {
                                    String targetUser = splitInput[1];
                                    if(targetUser.equals(currentUser)) {
                                        out.println("Error: You cannot send messages to yourself.%n%n");
                                        out.flush();
                                    }
                                    else {
                                        boolean messageSent = false;
                                        if(Server.validUserPass.get(targetUser) != null) {
                                            ArrayList<String> blocked = Server.userBlockedUsers.get(targetUser);
                                            if(blocked != null && blocked.contains(currentUser)) {
                                                out.println("You cannot send any message to " + targetUser + ". You have been blocked by the user.%n%n");
                                                out.flush();
                                            }
                                            else {
                                                String finalMessage = "";
                                                
                                                for(int i=2; i<splitInput.length; i++)
                                                    finalMessage += splitInput[i] + " ";
                                                
                                                finalMessage = currentUser + ": " + finalMessage + "%n";

                                                for(Server s : Server.runningThreads) {
                                                    if(s.getUser() != null && s.getUser().equals(targetUser)) {
                                                        PrintWriter targetOutput = s.getOutput();
                                                        out.println("Message Sent.%n%n");
                                                        out.flush();                                                
                                                        targetOutput.println(finalMessage);
                                                        targetOutput.flush();
                                                        messageSent = true;
                                                        break;
                                                    }
                                                }
                                                if(!messageSent) {
                                                    if(Server.offlineMessages.get(targetUser) == null)
                                                        Server.offlineMessages.put(targetUser, finalMessage);
                                                    else {
                                                        String currentMessage = Server.offlineMessages.get(targetUser);
                                                        Server.offlineMessages.put(targetUser, currentMessage + finalMessage);
                                                    }
                                                    out.println("User is currently not online. They will recieve the message the next time they log on.%n%n");
                                                    out.flush();
                                                }
                                            }
                                        }
                                        else {
                                            out.println("Target user '" + targetUser + "' does not exist in our system. Please check the username.%n%n");
                                            out.flush();
                                        }
                                    }
                                    
                                    
                                }
                            }
                            else {
                                out.println("Unknown Command.%n%n");
                                out.flush(); 
                            }
                        }
                    }
                    out.println("Command: ");
                    out.flush();
                }

            }
            Server.currentConnected.remove(currentUser);
            Server.lastLoggedOn.put(currentUser, System.currentTimeMillis()/1000);
            sock.close();
            System.out.println("Socket Closed");
        }
        //Logs people off the client after the amount of inactivity set above.
        catch(SocketTimeoutException timeout) {
            out.println("You were logged out for inactivity. Users are logged out of the system after '" + Server.TIME_OUT + "' minutes of inactivity.%nServer disconnected.%n");
            out.flush();
            System.out.println("Client '" + currentUser + "' disconnected for inactivity.");
            Server.currentConnected.remove(currentUser);
            Server.lastLoggedOn.put(currentUser, System.currentTimeMillis()/1000);

            try {
                sock.close();
            }
            catch(IOException ex) {
                System.out.println("Unable to close socket.");
            }
        }
        catch(Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        }
        Server.runningThreads.remove(this);
    }

    //Initializes the valid username/password HashTable defined above with the pairs specified in user_pass.txt
    public static void main(String[] args) {
        if(args.length != 1) {
            System.out.println("Usage: java Server <port>");
            return;
        }
        Integer targetPort = null;

        try {
            targetPort = Integer.parseInt(args[0]);
            Server.PORT = targetPort;
        }
        catch(Exception ex) {
            System.out.println("Error: Invalid port number.");
            return;
        }

        try {
            BufferedReader file = new BufferedReader(new FileReader("user_pass.txt"));
            validUserPass = new Hashtable<String,String>();
            String line;
            
            while((line = file.readLine()) != null) {
                String[] userpass = line.split(" ");

                if(userpass.length != 2) {
                    System.out.println("Invalid user/pass format found in file 'user_pass.txt'");
                    return;
                }
                validUserPass.put(userpass[0], userpass[1]);
            }
        }
        catch(Exception ex) {
            System.out.println("Problem with opening file 'user_pass.txt' or file not found.");
            return;
        }

        
        try {
            InetAddress inetAddr = InetAddress.getLocalHost();
            System.out.println("Server started at " + inetAddr.getHostAddress() + " on port " + targetPort + ".");
        }
        catch(Exception ex) {
            System.out.println("Can't retrieve ip address of computer.");
            return;
        }

        Server s = new Server();
        s.run();
    }
}
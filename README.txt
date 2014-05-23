Name: Alexander Dong
Uni: aqd2000
Computer Networks Programming Assignment #1
Due: March 4

a. 
    An implementation of a server-client terminal chat interface written in Java.
    The Server class, when run, opens up a socket on port 4119 on the local computer and waits for someone to connect. When someone connects, they are asked to identify themselves before allowing them to access the actual chat part of the program. Once they are in the 'chat part of the program', the Client can run any of the commands specified in the assignment plus the additional ones mentioned in (e).

    The Client class requires two command line arguments to run: a target ip address and port. When run, it tries to connect to a Server program running at that ip address and port. Once connected, it relays to the user any output coming from the server before blocking and waiting for user input. Because of the difficulty of implementing a synchronous terminal chat client, the Client program currently blocks after recieving a message from the Server. (It was said on Piazza that it wasn't necessary to have non-blocking user input) That is, in order to actually see messages from other users, it maybe be necessary at times for the user to press the "Enter" key in order to unblock the terminal temporarily. The Client is essentially an infinite loop that prints to the client the server output and afterwards blocks to wait for user input. The loop does not end until the Socket between it and the Server is closed. 

b. 
    java version "1.7.0_51"
    OpenJDK Runtime Environment (IcedTea 2.4.4) (7u51-2.4.4-0ubuntu0.13.10.1)
    OpenJDK 64-Bit Server VM (build 24.45-b08, mixed mode)
    Sublime Text 2

c.
    make 

    To Run Server:
        java Server <port>

    To Run Client:
        java Client <ip_address> <port>

d. 
    Currently Logged On: facebook, Google
    On in the past hour: windows

    java Client 127.0.1.1 4119
    Username: facebook
    Password: wastingtime
    Welcome to simple chat server!
    
    Command: whoelse
    Google

    Command: wholasthr
    Google
    windows

    Commmand: broadcast test
    facebook: test 

    Command: message facebook rawr
    Error: You cannot send messages to yourself.

    Command: message Google asdf

    Command: message Google pls
    You cannot send any message to Google. You have been blocked by the user.

    message Google o hai there
    Message Sent.




    java Client 127.0.1.1 4119
    Username: Google
    Password: hasglasses
    Welcome to simple chat server!
    facebook: test 
    facebook: asdf 

    Command: block Facebook
    Command: unblock facebook
    You have successfully unblocked 'facebook'.

    facebook: o hai there 

e.
    Extra Commands:

    commands - lists all the accepted commands of the chat server
        commands
        whoelse - prints all other users who are currently logged in
        wholasthr - prints all the users who have been on in the last hour
        broadcast <message> - sends a public message to everyone that is currently logged on
        message <user> <message> - sends a private message to a target user, if ablen%block <user> - blocks a user from being able to private message you
        unblock <user> - unblocks a user from being unable to private message you
        friend <user> - sends a friend request (user must accept) to target user. You are notified whenever someone on your friend's list logs on
        pending - lists all the friend requests waiting for your response
        friendlist - lists all your friends and their current status

    friend <user> - sends a friend request (user must accept) to target user. You are notified whenever someone on your friend's list logs on
    pending - lists all the friend requests waiting for your response
    friendlist - lists all your friends and their current status
        "Google" Client:
            Command: friend facebook
            You have sent 'facebook' a friend request.


            'facebook' just accepted your friend request.

            Command: friendlist
            Friend List: 
            facebook (online)
            windows (offline)

        "Facebook" Client:
            Username: facebook
            Password: wastingtime
            Welcome to simple chat server!
            You have a friend request pending from 'Google'.

            Command: pending
            Currently Pending Friend Requests: Google

            Command: friend Google
            You accepted the friend request from 'Google'!
    
    
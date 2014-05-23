
all: run

run: Server.java Client.java
	javac Server.java Client.java

clean:
	rm *.class
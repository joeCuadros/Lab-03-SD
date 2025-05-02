import java.net.*;
import java.io.*;
import java.util.*;
//The Client that can be run as a console
public class Client {
    // notification
    private String notif = " *** ";
    // for I/O
    private ObjectInputStream sInput; // to read from the socket
    private ObjectOutputStream sOutput; // to write on the socket
    private Socket socket; // socket object
    private String server, username; // server and username
    private int port; //port
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    Client(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;
    }
    public boolean start() {
        try {
            socket = new Socket(server, port);
        }catch(Exception ec) {
            display("Error al conectarse al servidor:" + ec);
            return false;
        }
        String msg = "Conexion aceptada " + socket.getInetAddress() + ":" + socket.getPort();
        display(msg);
        try {
            sInput = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        }catch (IOException eIO) {
            display("Excepcion al crear nuevos flujos de entrada/salida: " + eIO);
            return false;
        }
        new ListenFromServer().start();
        try{
            sOutput.writeObject(username);
        }catch (IOException eIO) {
            display("Excepcion al iniciar sesion : " + eIO);
            disconnect();
            return false;
        }
        return true;
    }
    private void display(String msg) {
        System.out.println(msg);
    }
    void sendMessage(ChatMessage msg) {
        try {
            sOutput.writeObject(msg);
        }
        catch(IOException e) {
            display("Excepcion al escribir en el servidor: " + e);
        }
    }
    private void disconnect() {
        try {
            if(sInput != null) sInput.close();
        } catch(Exception e) {}
        try {
            if(sOutput != null) sOutput.close();
        } catch(Exception e) {}
        
        try{
            if(socket != null) socket.close();
        } catch(Exception e) {}
    }

    public static void main(String[] args) {
        int portNumber = 1500;
        String serverAddress = "localhost";
        String userName = "Anonymous";
        Scanner scan = new Scanner(System.in);
        
        switch(args.length) {
            case 3:
                // caso > java Client username portNumber [serverAddr]
                serverAddress = args[2];
            case 2:
                // caso > java Client username [portNumber] serverAddr
                try {
                    portNumber = Integer.parseInt(args[1]);
                }  catch(Exception e) {
                    System.out.println("Numero de puerto invalido.");
                    System.out.println("Use: > java Client [username] [portNumber] [serverAddress]");
                    scan.close();
                    return;
                }
            case 1:
                userName = args[0];
                break;
            case 0:
                System.out.println("Enter the username: ");
                userName = scan.nextLine();
                break;
            default:
                System.out.println("Use: > java Client [username] [portNumber] [serverAddress]");
        }
        Client client = new Client(serverAddress, portNumber, userName);
        if(!client.start()){
            scan.close();
            return;
        }
        System.out.println("\n¡Hola! Bienvenido al chat.");
        System.out.println("Instrucciones:");
        System.out.println("1. Escribe un mensaje y presiona Enter para enviarlo a todos los usuarios conectados.");
        System.out.println("2. Escribe '@usuario<espacio>tu mensaje' (sin comillas) para enviar un mensaje privado.");
        System.out.println("3. Escribe 'WHOISIN' (sin comillas) para ver la lista de usuarios conectados.");
        System.out.println("4. Escribe 'LOGOUT' (sin comillas) para cerrar sesión y salir del chat.");
        while(true) {
            System.out.print("> ");
            // read message from user
            String msg = scan.nextLine();
            // logout if message is LOGOUT
            if(msg.equalsIgnoreCase("LOGOUT")) {
                client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
                break;
            }
            // message to check who are present in chatroom
            else if(msg.equalsIgnoreCase("WHOISIN")) {
                client.sendMessage(new ChatMessage(ChatMessage.WHOISIN, ""));
            }
            // regular text message
            else {
                client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, msg));
            }
        }
        scan.close();
        client.disconnect();   
    }
    class ListenFromServer extends Thread {
        public void run() {
        while(true) {
            try {
                // read the message form the input datastream
                String msg = (String) sInput.readObject();
                // print the message
                System.out.println(msg);
                System.out.print("> ");
            } catch(IOException e) {
                display(notif + "El servidor ha cerrado la conexión: " + e + notif);
                break;
            } catch(ClassNotFoundException e2) {}
        }
        }
    }
}

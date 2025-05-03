import java.net.*;
import java.io.*;
import java.util.*;
//Ejecucion de cliente
public class Client {
    private String notif = " *** "; //prefijo y sufijo
    private ObjectInputStream sInput; // lectura en el socket
    private ObjectOutputStream sOutput; // escritura en el socket
    private Socket socket; // objeto socket
    private String server, username; // server y username
    private int port; //puerto
    //METODOS NO USADOS
    /* 
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }*/
    // constructor
    Client(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;
    }
    private void display(String msg) {
        System.out.println(msg);
    }
    public boolean start() {
        try {
            socket = new Socket(server, port); //crear 
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
        new ListenFromServer().start(); //crea hilo
        try{
            sOutput.writeObject(username);
        }catch (IOException eIO) {
            display("Excepcion al iniciar sesion : " + eIO);
            disconnect();
            return false;
        }
        return true;
    }
    
    void sendMessage(ChatMessage msg) {
        try {
            sOutput.writeObject(msg);
        }
        catch(IOException e) {
            display("Excepcion al escribir en el servidor: " + e);
        }
    }
    //liberar recursos
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
        // predeterminado
        int portNumber = 1500;
        String serverAddress = "localhost";
        String userName = "Anonymous";
        Scanner scan = new Scanner(System.in);
        // configuracion
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
                System.out.print("Ingrese nombre de usuario: ");
                String user = scan.nextLine();
                if (!user.trim().equals("")) userName = user;
                break;
            default:
                System.out.println("Use: > java Client [username] [portNumber] [serverAddress]");
                scan.close();
                return;
        }
        Client client = new Client(serverAddress, portNumber, userName); //crear cliente
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
            String msg = scan.nextLine(); //escribir mensaje de usuario
            // LOGOUT
            if(msg.equalsIgnoreCase("LOGOUT")) {
                client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
                break;
            }
            // Listar usuarios
            else if(msg.equalsIgnoreCase("WHOISIN")) {
                client.sendMessage(new ChatMessage(ChatMessage.WHOISIN, ""));
            }
            // Mensaje normal
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
                String msg = (String) sInput.readObject();
                System.out.println("\r"+msg); // escribir sin >
                System.out.print("> ");
            } catch(IOException e) {
                display(notif + "El servidor ha cerrado la conexion: " + e + notif);
                break;
            } catch(ClassNotFoundException e2) {}
        }
        }
    }
}

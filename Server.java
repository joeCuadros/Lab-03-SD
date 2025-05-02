import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server {
    private static int uniqueId = 0; //unico Id
    private ArrayList<ClientThread> al; //lista de clientes
    private SimpleDateFormat sdf; //mostrar hora
    private int port; //numero de puerto
    private boolean keepGoing; //estado del servidor
    private String notif = " *** "; //notificacion

    public Server(int port) {
        this.port = port;
        sdf = new SimpleDateFormat("HH:mm:ss"); //tiempo hh:mm:ss
        al = new ArrayList<ClientThread>();
    }
    public void start() {
        keepGoing = true;
        try{
            ServerSocket serverSocket = new ServerSocket(port);
            while(keepGoing){
                display("Server esperando a Clientes en el puerto " + port + ".");
                Socket socket = serverSocket.accept();
                if(!keepGoing) break;
                ClientThread t = new ClientThread(socket);
                al.add(t);
                t.start();
            }
            try {
                serverSocket.close();
                for(int i = 0; i < al.size(); ++i) {
                    ClientThread tc = al.get(i);
                    try {
                        // close all data streams and socket
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    }  catch(IOException ioE) {
                    }
                }
            } catch(Exception e) {
                display("Excepcion cerrando el servidor y los clientes: " + e);
            }
        }catch (IOException e) {
            String msg = sdf.format(new Date()) + " Excepcion en el ServerSocket: " + e + "\n";
            display(msg);
        }
    }

    // to stop the server
    protected void stop() {
        keepGoing = false;
        try {
            new Socket("localhost", port);
        }
            catch(Exception e) {
        }
    }

    // Display an event to the console
    private void display(String msg) {
        String time = sdf.format(new Date()) + " " + msg;
        System.out.println(time);
    }

    private synchronized boolean broadcast(String message) {
        String time = sdf.format(new Date());
        String[] w = message.split(" ",3);
        boolean isPrivate = false;
        if(w[1].charAt(0)=='@') isPrivate=true;
        if(isPrivate==true){
            String tocheck=w[1].substring(1, w[1].length());
            message=w[0]+w[2];
            String messageLf = time + " " + message;
            boolean found=false;
            for(int y=al.size(); --y>=0;){
                ClientThread ct1=al.get(y);
                String check=ct1.getUsername();
                if(check.equals(tocheck)){
                    if(!ct1.writeMsg(messageLf)) {
                        al.remove(y);
                        display("Client desconectado " + ct1.username + " eliminado de la lista.");
                    }
                    found=true;
                    break;
                }
            }
            if(found!=true){
                return false;
            }
        } else {
            String messageLf = time + " " + message ;
            System.out.print(messageLf+ "\n");
            for(int i = al.size(); --i >= 0;) {
                ClientThread ct = al.get(i);
                // try to write to the Client if it fails remove it from the list
                if(!ct.writeMsg(messageLf)) {
                    al.remove(i);
                    display("Client desconectado " + ct.username + " eliminado de la lista.");
                }
            }
        }
        return true;
    }

    synchronized void remove(int id) {
        String disconnectedClient = "";
        // scan the array list until we found the Id
        for(int i = 0; i < al.size(); ++i) {
            ClientThread ct = al.get(i);
            // if found remove it
            if(ct.id == id) {
                disconnectedClient = ct.getUsername();
                al.remove(i);
                break;
            }
        }
        broadcast(notif + disconnectedClient + " ha abandonado la sala de chat." + notif);
    }

    public static void main(String[] args) {
        // start server on port 1500 unless a PortNumber is specified
        int portNumber = 1500;
        switch(args.length) {
        case 1:
            try {
                portNumber = Integer.parseInt(args[0]);
            } catch(Exception e) {
                System.out.println("Numero de puerto invalido.");
                System.out.println("Use: > java Server [portNumber]");
                return;
            }
        case 0:
            break;
        default:
            System.out.println("Use: > java Server [portNumber]");
            return;
        }
        // create a server object and start it
        Server server = new Server(portNumber);
        server.start();
    }

    class ClientThread extends Thread {
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        int id;
        String username;
        ChatMessage cm;
        String date;
        ClientThread(Socket socket) {
            id = ++uniqueId;
            this.socket = socket;
            System.out.println("Thread intentando crear Object Input/Output Streams");
            try{
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                // read the username
                username = (String) sInput.readObject();
                broadcast(notif + username + " se ha unido a la sala de chat." + notif);
            } catch (IOException e) {
                display("Excepcion al crear nuevo Input/output Streams: " + e);
                return;
            }catch (ClassNotFoundException e) {}
            date = new Date().toString() + "\n";
        }
        public String getUsername() {
            return username;
        }
        public void setUsername(String username) {
            this.username = username;
        }
        public void run() {
            boolean keepGoing = true;
            while(keepGoing) {
                try {
                    cm = (ChatMessage) sInput.readObject();
                } catch (IOException e) {
                    display(username + " Excepción de lectura Streams: " + e);
                    break;
                } catch (ClassNotFoundException e2) {
                    break;
                }
                String message = cm.getMessage();
                switch(cm.getType()) {
                    case ChatMessage.MESSAGE:
                        boolean confirmation = broadcast(username + ": " + message);
                        if(confirmation==false){
                            String msg = notif + "Lo siento. Ese usuario no existe." + notif;
                            writeMsg(msg);
                        }
                        break;
                    case ChatMessage.LOGOUT:
                        display(username + " usuario se desconecto con un mensaje de LOGOUT.");
                        keepGoing = false;
                        break;
                    case ChatMessage.WHOISIN:
                        writeMsg("Lista de los usuarios conectados en " + sdf.format(new Date())); 
                        for(int i = 0; i < al.size(); ++i) {
                            ClientThread ct = al.get(i);
                            writeMsg((i+1) + ") " + ct.username + " desde " + ct.date);
                        }
                        break;
                }
            }
            remove(id);
            close();
        }

        // close everything
        private void close() {
            try {
                if(sOutput != null) sOutput.close();
            }  catch (Exception e) {}
            try {
                if(sInput != null) sInput.close();
            } catch(Exception e) {};
            try {
                if(socket != null) socket.close();
            } catch (Exception e) {}
        }
        private boolean writeMsg(String msg) {
            if(!socket.isConnected()) {
                close();
                return false;
            }
            try {
                sOutput.writeObject(msg);
            } catch(IOException e) {
                display(notif + "Error al enviar mensaje a " + username + notif);
                display(e.toString());
            }
            return true;
        }
    }
}

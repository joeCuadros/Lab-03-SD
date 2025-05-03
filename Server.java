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
        this.port = port; //puerto
        sdf = new SimpleDateFormat("HH:mm:ss"); //tiempo hh:mm:ss
        al = new ArrayList<ClientThread>();
    }
    public void start() {
        keepGoing = true;
        try{
            ServerSocket serverSocket = new ServerSocket(port);
            while(keepGoing){
                display("Server esperando a Clientes en el puerto " + port + ".");
                Socket socket = serverSocket.accept(); //esperar hasta que conecte
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
    // NO SE UTILIZA ESTE METODO
    /*protected void stop() {
        keepGoing = false;
        try {
            new Socket("localhost", port);
        }
            catch(Exception e) {
        }
    }*/

    // Display an event to the console
    private void display(String msg) {
        String time = sdf.format(new Date()) + " " + msg;
        System.out.println(time);
    }

    private synchronized boolean broadcast(String message, String username) {
        String time = sdf.format(new Date());
        String[] word = message.split(" ",2);
        boolean isPrivate = false;
        //mensaje usuarios
        if(word[0].charAt(0)=='@') isPrivate=true; //@user es privado
        if(isPrivate==true && !username.equals("")){
            String tocheck=word[0].substring(1, word[0].length()); //extraer usuario
            if (word.length == 1) return false; //si no hay mensaje
            String messageLf = time + " (" + username+") [privado] "+word[1]; //mensaje
            boolean found=false;
            if (tocheck.equals("")) return false; //si el usuario es ''
            // buscando cliente con ese nombre
            for(int y=al.size(); --y>=0;){
                ClientThread ct1=al.get(y);
                String check=ct1.getUsername();
                if(check.equals(tocheck)){
                    if(!ct1.writeMsg(messageLf)) { //intenta mandar mensaje
                        al.remove(y);
                        display("Client desconectado " + ct1.username + " eliminado de la lista.");
                    }
                    found=true;
                    break;
                }
            }
            return found;
        } else {
            String messageLf;
            // notificacion 
            if(username.equals("")){
                messageLf = time + " " + notif + message + notif;
            }else{
                messageLf = time + " (" + username+") "+message;
            }
            System.out.println(messageLf);
            for(int i = al.size(); --i >= 0;) {
                ClientThread ct = al.get(i);
                if (ct.getUsername().equals(username)) continue;
                if(!ct.writeMsg(messageLf)) { //inteneta mandar mensaje
                    al.remove(i);
                    display("Client desconectado " + ct.username + " eliminado de la lista.");
                }
            }
        }
        return true;
    }

    synchronized void remove(int id) {
        String disconnectedClient = "";
        for(int i = 0; i < al.size(); ++i) {
            ClientThread ct = al.get(i);
            // cliente encontrado
            if(ct.id == id) {
                disconnectedClient = ct.getUsername();
                al.remove(i);
                break;
            }
        }
        broadcast(disconnectedClient + " ha abandonado la sala de chat.", "");
    }

    public static void main(String[] args) {
        // crear servidor
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
        Server server = new Server(portNumber); //nuevo server
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
            id = ++uniqueId; //id unico
            this.socket = socket;
            System.out.println("Thread intentando crear Object Input/Output Streams");
            try{
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                username = (String) sInput.readObject();
                broadcast(username + " se ha unido a la sala de chat.","");
            } catch (IOException e) {
                display("Excepcion al crear nuevo Input/output Streams: " + e);
                return;
            }catch (ClassNotFoundException e) {}
            date = new Date().toString();
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
                    cm = (ChatMessage) sInput.readObject(); //recepcionar mensaje
                } catch (IOException e) {
                    display(username + " Excepcion de lectura Streams: " + e);
                    break;
                } catch (ClassNotFoundException e2) {
                    break;
                }
                String message = cm.getMessage();
                switch(cm.getType()) {
                    case ChatMessage.MESSAGE:
                        boolean confirmation = broadcast(message,username);
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

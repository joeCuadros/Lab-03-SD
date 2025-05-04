import socket, threading, pickle, sys, msvcrt  
from datetime import datetime
from ChatMessage import ChatMessage

def obtenerFecha():
    return datetime.now().strftime("%H:%M:%S")
class ClientThread(threading.Thread):
    def __init__(self, client_socket, address, server):
        threading.Thread.__init__(self)
        self.client_socket = client_socket
        self.address = address
        self.server = server
        self.username = None
        self.running = True
        self.date = str(datetime.now().strftime("%a %b %d %H:%M:%S"))

    def run(self):
        try:
            self.username = self.receive_message().message
            self.server.broadcast(f" *** {obtenerFecha()} {self.username} se ha unido al chat . *** ", self)
            self.server.add_client(self)
            while self.running:
                msg_obj = self.receive_message()
                if msg_obj.type == ChatMessage.MESSAGE:
                    if msg_obj.message.startswith("@"):
                        try:
                            to_user, message = msg_obj.message[1:].split(" ", 1)
                            self.server.private_message(f"({self.username})[Privado]: {message}", to_user, self)
                        except ValueError:
                            self.send_message("Formato incorrecto para mensaje privado. Usa: @usuario mensaje")
                    else:
                        self.server.broadcast(f"({self.username}): {msg_obj.message}", self)
                elif msg_obj.type == ChatMessage.WHOISIN:
                    user_list = self.server.get_user_list()
                    enumerated_list = "\n".join(f"{i+1}. {username}" for i, username in enumerate(user_list))
                    self.send_message("Usuarios conectados:\n" + enumerated_list)

                elif msg_obj.type == ChatMessage.LOGOUT:
                    self.running = False
        except:
            pass
        finally:
            self.server.remove_client(self)
            self.client_socket.close()
            self.server.broadcast(f" *** {obtenerFecha()} {self.username} ha salido del chat. *** ", self)

    def receive_message(self):
        data = self.client_socket.recv(4096)
        return pickle.loads(data)

    def send_message(self, msg):
        try:
            self.client_socket.send(pickle.dumps(ChatMessage(ChatMessage.MESSAGE, msg)))
        except:
            pass


class Server:
    def __init__(self, port):
        self.port = port
        self.clients = []
        self.lock = threading.Lock()

    def start(self):
        self.keep_going = True
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.bind(('', self.port))
        self.server_socket.listen()
        print(f"{datetime.now().strftime('%H:%M:%S')} Servidor escuchando en el puerto {self.port}")
        threading.Thread(target=self._stop_on_enter).start()

        while self.keep_going:
            try:
                client_socket, addr = self.server_socket.accept()
                ClientThread(client_socket, addr, self).start()
            except:
                continue

        self.server_socket.close()

    def _stop_on_enter(self):
        print("Presiona la tecla 'q' para cerrar el servidor...")
        while True:
            if msvcrt.kbhit() and msvcrt.getch() == b'q':
                self.broadcast(" *** Cerrando el servidor... *** ")
                self.keep_going = False
                self.server_socket.close()
                break

    def broadcast(self, message, sender=None):
        with self.lock:
            print(message)
            for client in self.clients:
                if client != sender:
                    client.send_message(message)

    def private_message(self, message, to_user, sender):
        with self.lock:
            for client in self.clients:
                if client.username == to_user:
                    client.send_message(message)
                    return
        sender.send_message(f"Usuario '{to_user}' no encontrado.")

    def add_client(self, client):
        with self.lock:
            self.clients.append(client)

    def remove_client(self, client):
        with self.lock:
            if client in self.clients:
                self.clients.remove(client)

    def get_user_list(self):
        with self.lock:
            return [f"{c.username} {c.date}" for c in self.clients]

if __name__ == "__main__":
    port = 1500
    if len(sys.argv) == 2:
        port = int(sys.argv[1])
    Server(port).start()


import socket, threading, pickle, sys
from ChatMessage import ChatMessage

class Client:
    def __init__(self, username, port, host):
        self.username = username
        self.port = port
        self.host = host
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.running = True

    def start(self):
        try:
            self.sock.connect((self.host, self.port))
        except Exception as e:
            print(f"Error de conexión: {e}")
            return

        # Enviar nombre
        self.send(ChatMessage(ChatMessage.MESSAGE, self.username))

        print("\n¡Hola! Bienvenido al chat.")
        print("Instrucciones:")
        print("1. Escribe un mensaje y presiona Enter para enviarlo a todos los usuarios conectados.")
        print("2. Escribe '@usuario mensaje' para enviar un mensaje privado.")
        print("3. Escribe 'WHOISIN' para ver la lista de usuarios conectados.")
        print("4. Escribe 'LOGOUT' para cerrar sesión y salir del chat.")

        threading.Thread(target=self.listen).start()

        while self.running:
            try:
                msg = input("\r> ")
                if msg.upper() == "WHOISIN":
                    self.send(ChatMessage(ChatMessage.WHOISIN, ""))
                elif msg.upper() == "LOGOUT":
                    self.send(ChatMessage(ChatMessage.LOGOUT, ""))
                    self.running = False
                else:
                    self.send(ChatMessage(ChatMessage.MESSAGE, msg))
            except KeyboardInterrupt:
                self.send(ChatMessage(ChatMessage.LOGOUT, ""))
                self.running = False

        self.sock.close()

    def listen(self):
        while self.running:
            try:
                data = self.sock.recv(4096)
                if not data:
                    break
                msg_obj = pickle.loads(data)
                print("\r"+msg_obj.message+"\n> ",end="")
            except:
                break

    def send(self, msg_obj):
        try:
            self.sock.send(pickle.dumps(msg_obj))
        except Exception as e:
            print(f"Error enviando mensaje: {e}")
            self.running = False

if __name__ == "__main__":
    port_number = 1500
    server_address = "localhost"
    username = "Anonymous"

    # Configuración de argumentos
    import sys
    if len(sys.argv) > 4: sys.exit(1)

    if len(sys.argv) == 4: 
        server_address = sys.argv[3]

    if len(sys.argv) >= 3:
        try:
            port_number = int(sys.argv[2])
        except ValueError:
            print("Numero de puerto invalido.")
            print("Uso: python Client.py [username] [portNumber] [serverAddress]")
            sys.exit(1)

    if len(sys.argv) >= 2:
        username = sys.argv[1]

    if len(sys.argv) == 1:
        user = input("Ingrese nombre de usuario: ")
        if user.strip() != "": username = user
        
    Client(username, port_number, server_address).start()

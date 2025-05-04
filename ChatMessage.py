class ChatMessage:
    WHOISIN = 0
    MESSAGE = 1
    LOGOUT = 2
    STOP = 3

    def __init__(self, message_type, message=""):
        self.type = message_type
        self.message = message

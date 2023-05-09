# import websocket

# def on_message(wsapp, message):
#     print(message)

# wsapp = websocket.WebSocketApp("ws://127.0.0.1:8080/chat",
#         on_message=on_message)

# wsapp.run_forever()


import websocket
import _thread
import time
import rel
import sys
import select


if __name__ == "__main__":

    ws = websocket.WebSocket()
    ws.connect("ws://127.0.0.1:8080/chat")

    done = False

    text_to_send = None

    while not done:
        inputs = [ sys.stdin, ws.sock ]
        outputs = [ws.sock ]

        readable, writable, exceptional = select.select(inputs, outputs, outputs)

        if ws.sock in exceptional:
            print("Error on socket, exiting")
            done = True

        if sys.stdin in readable:
            text_to_send = sys.stdin.readline().strip()
        if ws.sock in readable:
            received_text = ws.recv()
            print(received_text)

        if text_to_send and ws.sock in writable:
            ws.send(text_to_send)
            text_to_send = None

    ws.close()



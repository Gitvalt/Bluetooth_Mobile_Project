import time
from bluetooth import *

while True:
    
    server_sock = BluetoothSocket(RFCOMM)
    server_sock.bind(("", PORT_ANY))
    server_sock.listen(1)

    port = server_sock.getsockname()[1]

    uuid = "94f39d29-7d6d-437d-973b-fba39e49d4ee"

    advertise_service(server_sock, "BluetoothDemoServer", service_id = uuid, service_classes = [ uuid, SERIAL_PORT_CLASS ], profiles = [ SERIAL_PORT_PROFILE ])


    print("Waiting for connection...")

    client_sock, client_info = server_sock.accept()
    print("New connection: ", client_info)
    client_sock.send("Connection successful!")
    #file = open("test.jpg", "wb+")
    try:
        while True:
            data = client_sock.recv(1024)
            data_str = data.decode("utf-8")
            if len(data) == 0: break
            print("Got message: {}".format(data_str))
            #file.write(data)
            client_sock.send("Message was recieved OK! time: {} \n Got message: \n {} \n Message backwards: \n {}".format(time.time(), data_str,data_str[::-1]))
    except IOError:
        pass

    print("disconnected")
    #file.close()
    client_sock.close()
    server_sock.close()

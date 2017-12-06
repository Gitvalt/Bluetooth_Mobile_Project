# coding=utf-8
import time
import io
from PIL import Image
from bluetooth import *


while True:

    server_sock = BluetoothSocket(RFCOMM)
    server_sock.bind(("", PORT_ANY))
    server_sock.listen(1)

    port = server_sock.getsockname()[1]

    uuid = "94f39d29-7d6d-437d-973b-fba39e49d4ee"

    advertise_service(server_sock, "BluetoothDemoServer", service_id=uuid, service_classes=[
                      uuid, SERIAL_PORT_CLASS], profiles=[SERIAL_PORT_PROFILE])

    print("Waiting for connection...")

    client_sock, client_info = server_sock.accept()
    print("New connection: ", client_info)
    client_sock.send("Connection successful!")
    
    try:
        collection = ""
        command = "default"
        msgLenght = 0 
        imgType = None
        while True:
            #Default input mode (expect string message (mode, length, type))
            if command == "default":
                try:
                    #try to parse input as utf-8 message.
                    print "default"
                    data = client_sock.recv(2000)
                    data_str = data.decode("utf-8")
                    
                    #print received input
                    print data

                    # parse input command 
                    # Usually prepares frame for reading picture bytearray:
                    # (Picture, how many bytes to be expected, pictures extension(png, jpeg...))
                    msg = data_str.split(",")
                    command = msg[0]
                    msgLenght = msg[1]
                    imgType = msg[2]

                    if command == "Picture":
                            client_sock.send("Ready for picture")

                except Exception:
                    pass

            #if we are expecting a picture
            elif command == "Picture":
                #print downloading milestones
                print "picture", msgLenght, imgType
                size = len(collection)
                print size

                #if current bytesize is same size as the expected picture bytearray size
                if size == int(msgLenght):
                    print "data received!"
                    client_sock.send("Picture,OK,{}".format(time.time()))       
                    item = Image.open(io.BytesIO(collection))
                    item.save("./media/imported.png")
                    command = "default"
                    print "finished"

                #if too much data has been received
                elif size > int(msgLenght):
                    print "too much data! Mobile app should not send extra data until data transfer has been confirmed by server"
                    client_sock.send("{},{},{}".format("ERROR","Too much data",time.time()))       
                    
                #not same size --> downloading is still ongoing
                else:
                    data = client_sock.recv(50000)
                    collection += (data)
                    print "Downloading...({} out of {})".format(len(collection), msgLenght)

            elif command == "Status":
                client_sock.send("{},{},{}".format("Status","All OK",time.time()))
                   
                
    except IOError as err:
        print "Error has happened! {}".format(err)


    print("disconnected")
    # file.close()
    client_sock.close()
    server_sock.close()

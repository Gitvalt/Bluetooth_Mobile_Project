# coding=utf-8
import time
import io
from PIL import Image
from bluetooth import *
from tqdm import tqdm
import base64


def serverstart(self):
    while True:
        try:

            server_sock = BluetoothSocket(RFCOMM)
            server_sock.bind(("", PORT_ANY))
            server_sock.listen(1)

            port = server_sock.getsockname()[1]

            uuid = "94f39d29-7d6d-437d-973b-fba39e49d4ee"

            advertise_service(server_sock, "BluetoothDemoServer", service_id=uuid, service_classes=[
                            uuid, SERIAL_PORT_CLASS], profiles=[SERIAL_PORT_PROFILE])

            print("Waiting for connection...")

            client_sock, client_info = server_sock.accept()
            print(("New connection: ", client_info))
            client_sock.send("Connection successful!,200")
            start_time = time.time()
            collection = b""
        
            
            command = "default"
            msgLenght = 0 
            imgType = None

            #Default input mode (expect string message (mode, length, type))
            if command == "default":
                try:
                    #try to parse input as utf-8 message.
                    print("default!")
                    data = client_sock.recv(2000)
                    data_str = data.decode("utf-8")
                    
                    #print received input
                    print("Received input: '{}'".format(data_str))

                    if data_str is not '':

                        # parse input command 
                        # Usually prepares frame for reading picture bytearray:
                        # (Picture, how many bytes to be expected, pictures extension(png, jpeg...))
                        msg = data_str.split(",")
                        command = msg[0]
                        msgLenght = msg[1]
                        imgType = msg[2]

                        if command == "Picture":
                                client_sock.send("200")

                except Exception as e:
                    print(e.message)

            #if we are expecting a picture
            if command == "Picture":
                size = len(collection)
                collection = b""
                while size < int(msgLenght):
                    #print downloading milestones
                    print("picture", msgLenght, imgType)
                    size = len(collection)
                    print(size)

                    #if current bytesize is same size as the expected picture bytearray size
                    if size == int(msgLenght):
                        print("data received!")
                        try:
                            with open("media/{}.png".format(time.time()), "wb+") as image_f:
                                image_f.write(base64.b64decode(collection))
                            command = "default"
                            client_sock.send("Picture,OK,{}".format(time.time())) 
                            print("finished")
                            self.get_images(True)
                        except Exception as e:
                            print(e.message)

                    #if too much data has been received
                    elif size > int(msgLenght):
                        print("too much data! Mobile app should not send extra data until data transfer has been confirmed by server")
                        client_sock.send("{},{},{}".format("ERROR","Too much data",time.time()))       
                        
                    #not same size --> downloading is still ongoing
                    else:
                        data = client_sock.recv(50000)
                        collection += (data)
                        print("Downloading...({} out of {})".format(len(collection), msgLenght))

            elif command == "Status":
                client_sock.send("{},{},{}".format("Status","200",time.time()))

            elif command == "GetImage":
                print("sending ")
                with open(self.images[self.selected_img], "rb") as image_f:
                    imgb64 = base64.b64encode(image_f.read())
                #print("{}".format(imgb64))
                debugfile = open("Output.txt", "w+")
                debugfile.write("{}".format(imgb64))
                debugfile.close()
                client_sock.send("{}".format(len(imgb64)))
                print(len(imgb64))
                handshake = client_sock.recv(512)
                client_sock.send("{}".format(imgb64)[2:-1])
                print("done")
                command = "default"
                                    
            elif command == "Shutdown":
                client_sock.send("Shutdown")
                print("Disconnecting")
                break
                
                    
        except IOError as err:
            pass

        try:
            print("disconnected")
            # file.close()
            client_sock.close()
            server_sock.close()
            stop_advertising(server_sock)

        except Exception as e:
            print(e.message)

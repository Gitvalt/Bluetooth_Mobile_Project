from PIL import Image, ImageTk
import Tkinter as tk
import os as OS
from threading import Thread

print "Server Starting!"

class BluetoothServer(Thread):
	def __init__(self):
		Thread.__init__(self)

	def run(self):
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
			#file = open("test.jpg", "wb+")
			try:
				while True:
					data = client_sock.recv(1024)
					if len(data) == 0:
							break
					image = Image.open(io.BytesIO(bytes))
					image.save("./")
					print("Got message: image")
					client_sock.send("Message was recieved OK! time: {} \n Got message: \n {} \n Message backwards: \n {}".format(time.time(), "Image", "asjkdlÃ¶jsdl"))
			except IOError:
				pass

			print("disconnected")
			# file.close()
			client_sock.close()
			server_sock.close()

class Application(tk.Frame):              
	def __init__(self, master=None):
		tk.Frame.__init__(self, master)   
		self.grid(row=2, column=2)    	
		self.pictures = self.getPictures()
		self.createWidgets()
		
		self.serverThread = BluetoothServer()
		serverThread.start()

		# 4000 ms = 4s
		self.UPDATE_RATE = (4000)
		self.updater()

	def getPictures(self):
			
			print "Get pictures from media!"
			
			content = OS.listdir("./media")

			i = 0
			pictures = []
			
			for i in range(len(content)):
				picture_count = len(pictures)
				item = content[i]
				name, ext =  OS.path.splitext(item)
				print name + ext
				
				if ext == ".jpg":
					pictures.append(name + ext)
				elif ext == ".png":
					pictures.append(name + ext)

			return pictures

	def createWidgets(self):

		pictures = self.pictures

		img = None

		if len(pictures) > 0:
			path = pictures[0]
			photo = Image.open("./media/" + path)
			photo = photo.resize((400, 400), Image.BOX)
			img = ImageTk.PhotoImage(photo)
		
		label = tk.Label(image = img, relief="raised", borderwidth=10)
		label.image = img
		
		self.Picture = label
		self.Picture.grid(row=0, column=0)

		self.quitButton = tk.Button(self, text='Quit', command=self.quit, width=30, height=2, relief="raised")
		self.quitButton.grid(row=1, column=0)

	# Updater is run every $self.UPDATE_RATE time has passed 
	def updater(self):

		if self.serverThread.isAlive():

			#actions to be done when app is updated:
			tmp = self.getPictures()

			if tmp != self.pictures:
				print "dataset changed"
				self.pictures = tmp

			#set app to update after $self.UPDATE_RATE has passed
			self.after(self.UPDATE_RATE, self.updater)



		
	


app = Application()                       
app.master.title('Picture Frame Application')    
app.mainloop()
from PIL import Image, ImageTk
import Tkinter as tk
import os as OS

print "Server Starting!"



class Application(tk.Frame):              
	def __init__(self, master=None):
		tk.Frame.__init__(self, master)   
		self.grid(row=2, column=2)    
		
		self.pictures = self.getPictures()

		self.createWidgets()

		self.UPDATE_RATE = (400 * 1000)
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
		
		#actions to be done when app is updated:

		#set app to update after $self.UPDATE_RATE has passed
		self.after(self.UPDATE_RATE, self.updater)
	


app = Application()                       
app.master.title('Picture Frame Application')    
app.mainloop()
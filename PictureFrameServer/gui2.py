import tkinter as tk
import tkinter.scrolledtext as tkst
import threading
import queue
import glob
import sys
from PIL import Image, ImageTk
import serverpy3
import time
import os



class Application(tk.Frame):
    
    def __init__(self, master=None):
        self.serverthread = threading.Thread(target=self.serverstart)
        self.serverthread.start()
        self.width = 1280
        self.height = 720
        self.sidebar = 100
        self.selected_img = 0
        super().__init__(master)
        master.minsize(width=self.width, height=self.height)
        master.maxsize(width=self.width, height=self.height)
        self.images = []
        self.grid_columnconfigure(1, minsize=self.width-self.sidebar)
        self.grid_columnconfigure(2, minsize=self.sidebar)
        self.grid()
        self.get_images()
        self.create_widgets()

    def serverstart(self):
        while True:
            try:
                serverpy3.serverstart(self)
            except:
                print("error",sys.exc_info())
                time.sleep(1)

    def create_widgets(self):
        #self.textbox = tkst.ScrolledText(self, height=20, width=180)
        #self.textbox.pack(padx=10, pady=10,side="top")
        

        self.nextbtn = tk.Button(self, command=lambda: self.change_img(1))
        self.nextbtn["text"] = "Next img >>"

        self.nextbtn.grid(row=0, column=2)

        self.prevbtn = tk.Button(self, command=lambda: self.change_img(-1))
        self.prevbtn["text"] = "<< Prev img"
        self.prevbtn.grid(row=1, column=2)

        self.rotatebtn = tk.Button(self, command=self.rotate)
        self.rotatebtn["text"] = "Rotate >"
        self.rotatebtn.grid(row=2, column=2)

        self.img_text = tk.Label(self, text="{} / {}".format(self.selected_img+1, len(self.images)))
        self.img_text.grid(row=3, column=2)

        self.imagelabel = tk.Label(self)
        self.imagelabel.grid(row=0, column=1, rowspan=20)
        self.change_img(self.selected_img)


    def get_images(self, change=False):
        newimages = glob.glob("media/*.jpg") + glob.glob("media/*.png")
        self.images = glob.glob("media/*.jpg") + glob.glob("media/*.png")
        self.images.sort(key=os.path.getmtime, reverse=True)
        if change:
            self.selected_img = 0
            self.change_img(0)

        print(self.images)

    def rotate(self):
        tmp = Image.open(self.images[self.selected_img])
        tmp = tmp.rotate(-90, expand=True)
        tmp.save(self.images[self.selected_img])
        self.change_img(0)

    def change_img(self, value):
        print(self.selected_img + value)
        self.selected_img += value
        if self.selected_img > len(self.images)-1:
            self.selected_img = 0
        elif self.selected_img < 0:
            self.selected_img = len(self.images)-1
        self.img = Image.open(self.images[self.selected_img])
        w,h = self.img.size
        ratio = min([(self.width-self.sidebar)/w, self.height/h])
        print(w,h,ratio) 

        #self.tkimage = ImageTk.PhotoImage( self.img.resize((int(w*ratio), int(h*ratio)), Image.LANCZOS))
        self.tkimage = ImageTk.PhotoImage(self.img.resize((int(w*ratio), int(h*ratio))))
        self.imagelabel["image"] = self.tkimage
        self.img_text["text"] = "{} / {}".format(self.selected_img+1, len(self.images))


root = tk.Tk()
app = Application(master=root)
app.mainloop()
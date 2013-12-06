using System;
using Microsoft.SPOT;
using Microsoft.SPOT.Hardware;
using Microsoft.SPOT.Presentation;
using Microsoft.SPOT.Presentation.Media;
using System.Threading;
using System.IO.Ports;
using System.Text;

namespace WhatsAroundYou
{
    public class Program
    {
        static int zoom = 10; //has to be equal to android apps default zoom value
        static int zoom_min = 0;
        static int zoom_max = 18;
        static bool action = false; //action/ mid button

        static Font font = null;
        static Bitmap display;
        static Bitmap copy;
        static SerialPort serial = null;

        static Menu menu;

        public static void Main()
        {
            //default font
            font = Resources.GetFont(Resources.FontResources.NinaB);

            //init text
            display = new Bitmap(Bitmap.MaxWidth, Bitmap.MaxHeight);
            display.Clear();
            display.DrawText("waiting for", font, Color.White, 25, 50);
            display.DrawText("connection", font, Color.White, 25, 70);
            display.Flush();

            //add bluetooth receiver and open port
            serial = new SerialPort("COM1");
            serial.DataReceived += serialDataReceived;
            serial.Open();

            //add top button handler
            InterruptPort topBtn = new InterruptPort(HardwareProvider.HwProvider.GetButtonPins(Button.VK_UP), false, Port.ResistorMode.PullDown, Port.InterruptMode.InterruptEdgeHigh);
            topBtn.OnInterrupt += btnPressed;

            //add middle button handler
            InterruptPort midBtn = new InterruptPort(HardwareProvider.HwProvider.GetButtonPins(Button.VK_SELECT), false, Port.ResistorMode.PullDown, Port.InterruptMode.InterruptEdgeHigh);
            midBtn.OnInterrupt += btnPressed;

            //add bottom button handler
            InterruptPort botBtn = new InterruptPort(HardwareProvider.HwProvider.GetButtonPins(Button.VK_DOWN), false, Port.ResistorMode.PullDown, Port.InterruptMode.InterruptEdgeHigh);
            botBtn.OnInterrupt += btnPressed;

            Thread.Sleep(Timeout.Infinite);
        }

        //bluetooth receiver
        static void serialDataReceived(object sender, SerialDataReceivedEventArgs e)
        {
            try
            {
                display.Dispose(); //remove old bitmap
                byte[] receivedData = new byte[serial.BytesToRead]; //buffer for the data
                serial.Read(receivedData, 0, serial.BytesToRead); //get byte data

                if (action == false) //show image, if additional action is inactive
                    display = new Bitmap(receivedData, Bitmap.BitmapImageType.Jpeg); //generate bitmap; compression is jpeg, becaue it's the only compression also supported by android
                else
                {
                    //byte array to char array
                    System.Text.UTF8Encoding encoding = new System.Text.UTF8Encoding();
                    char[] characters = encoding.GetChars(receivedData);
                    

                    if (menu != null && menu.selected) //show details of selected menu item
                    {
                        display = new Bitmap(Bitmap.MaxWidth, Bitmap.MaxHeight); //create a new, empty bitmap

                        String line = "";
                        for (int i = 0, lineCounter = 0, lines = 1; i < characters.Length && lines * font.Height + 4 < Bitmap.MaxHeight - font.Height - 4; i++) //cancel if all characters were drawn or the max height was reached
                        {
                            char c = characters[i];
                            line += c.ToString(); //count the lines
                            lineCounter += font.CharWidth(c); //calculate needed width for each character
                            if (lineCounter > Bitmap.MaxWidth || i == characters.Length-1)
                            {
                                display.DrawText(line, font, Color.White, 4, 4 + font.Height * lines); //draw line
                                lineCounter = 0;
                                lines++;
                            }
                        }
                    }
                    else //init menu
                    {
                        String list = new String(characters);

                        copy = new Bitmap(display.GetBitmap(), Bitmap.BitmapImageType.Jpeg); //create a copy of the map image
                        display = new Bitmap(Bitmap.MaxWidth, Bitmap.MaxHeight); //create a new, empty bitmap to reduce memory usage

                        //create and draw menu
                        menu = new Menu(list, display, font);
                        menu.draw();
                    }
                }

                display.Flush();
            }
            catch {} //discard invalid bitmaps
        }

        //button handler
        static void btnPressed(uint pin, uint value, DateTime time)
        {
            switch (pin)
            {
                case 2: //top button; zoom in
                    if (action == false)
                    {
                        if (zoom < zoom_max)
                        {
                            zoom++;
                            sendData();
                        }
                    }
                    else
                        menu.down();
                    break;
                case 3: //middle button; start/ stop action
                    if (action == true)
                    {
                        if (menu == null || menu.current == 0) //back item is selected
                        {
                            display = copy; //show previous map image
                            action = false;
                        }
                        else
                        {
                            if (menu.selected) //redraw menu
                            {
                                menu.draw();
                                menu.selected = false;
                            }
                            else
                                menu.selected = true;
                        }
                    }
                    else
                        action = true;
                    sendData();
                    break;
                case 4: //bottom button; zoom out
                    if (action == false)
                    {
                        if (value == 1 && zoom > zoom_min)
                        {
                            zoom--;
                            sendData();
                        }
                    }
                    else
                        menu.up();
                    break;
            }
        }

        //send data to bluetooth device
        static void sendData()
        {
            String datastring = zoom + ",";
            if(action && menu != null)
                datastring += "1,"+(menu.current-1);
            else
                datastring += "0,-1";
            byte[] data = Encoding.UTF8.GetBytes(datastring + ",");

            Debug.Print(datastring);

            serial.Write(data,0,data.Length);
        }
    }
}

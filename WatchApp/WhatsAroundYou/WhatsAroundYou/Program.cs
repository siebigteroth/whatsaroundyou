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
        static bool tracking = false;

        static Font font = null;
        static Bitmap display;
        static SerialPort serial = null;

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
                byte[] receivedImageData = new byte[serial.BytesToRead]; //buffer for the data
                serial.Read(receivedImageData, 0, serial.BytesToRead); //get byte data
                display = new Bitmap(receivedImageData, Bitmap.BitmapImageType.Jpeg); //generate bitmap; compression is jpeg, becaue it's the only compression also supported by android
                display.Flush();
            }
            catch {} //discard invalid bitmaps
        }

        //button handler
        static void btnPressed(uint pin, uint value, DateTime time)
        {
            switch (pin)
            {
                case 2: //top button
                    if (value == 1 && zoom < zoom_max)
                    {
                        zoom++;
                        sendData();
                    }
                    else if(value == 0 && zoom > zoom_min)
                    {
                        zoom--;
                        sendData();
                    }
                    break;
                case 3: //middle button
                    if (tracking == true)
                        tracking = false;
                    else
                        tracking = true;
                    sendData();
                    break;
                case 4: //bottom button
                    if (value == 1 && zoom > zoom_min)
                    {
                        zoom--;
                        sendData();
                    }
                    else if (value == 0 && zoom < zoom_max)
                    {
                        zoom++;
                        sendData();
                    }
                    break;
            }
        }

        //send data to bluetooth device
        static void sendData()
        {
            String datastring = zoom + ",";
            if(tracking)
                datastring += "1";
            else
                datastring += "0";
            byte[] data = Encoding.UTF8.GetBytes(datastring + ",");
            serial.Write(data,0,data.Length);
        }
    }
}

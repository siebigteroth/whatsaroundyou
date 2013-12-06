using System;
using Microsoft.SPOT;
using Microsoft.SPOT.Presentation.Media;

namespace WhatsAroundYou
{
    class Menu
    {
        private String[] items;
        public int current;
        private Bitmap bitmap;
        private Font font;
        public bool selected;

        public Menu(String list, Bitmap bitmap, Font font)
        {
            String[] listItems = list.Split(";".ToCharArray());
            this.items = new String[listItems.Length + 1];
            this.items[0] = "back";
            for (int i = 0; i < listItems.Length; i++)
                this.items[i + 1] = listItems[i];
            this.current = 0;
            this.bitmap = bitmap;
            this.font = font;
            this.selected = false;
        }

        public void up()
        {
            if(current<items.Length-1)
                current++;
            draw();
        }

        public void down()
        {
            if (current>0)
                current--;
            draw();
        }

        public void draw()
        {
            bitmap = new Bitmap(Bitmap.MaxWidth, Bitmap.MaxHeight);
            bitmap.Clear();
            bitmap.DrawLine(Color.White, Bitmap.MaxHeight, 0, 0, Bitmap.MaxWidth, Bitmap.MaxHeight); //set background-color to white

            //calculate entries for each page and the visible lines
            int entriesPage = (Bitmap.MaxHeight-8) / font.Height;
            int factor = (int)System.Math.Floor(current / entriesPage);
            int start = factor * entriesPage;
            int end = factor * entriesPage * 2;
            if (end > items.Length)
                end = items.Length;
            else if (end <= 0)
                end = entriesPage;

            //draw visible lines
            for (int i = 0, n=start; n < end; i++, n++)
            {
                if (font.Height * (i + 1) < Bitmap.MaxHeight)
                {
                    Color color;
                    if (n == current) //define highlighted/ current active line
                    {
                        int height = 4 + font.Height * (i + 1) - (int)(System.Math.Round(font.Height * 0.5));
                        bitmap.DrawLine(Color.Black, font.Height - 8, 0, height, Bitmap.MaxWidth, height);
                        color = Color.White;
                    }
                    else //define other visible lines
                        color = Color.Black;
                    bitmap.DrawText(items[n], font, color, 4, 4 + font.Height * i); //draw line
                }
            }

            bitmap.Flush();
        }
    }
}

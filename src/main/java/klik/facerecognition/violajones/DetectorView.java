package klik.facerecognition.violajones;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;


public class DetectorView extends JFrame{

  public static void main(String[] args) throws IOException {
    new DetectorView(new File(args[0]),args[1]).setVisible(true);
  }

	public DetectorView(File img, String XMLFile) throws FileNotFoundException, IOException{
		Image image=null;
		try {
			image = ImageIO.read(img);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Dessin d = new Dessin(image);
		Viola_Jones_detector detector=new Viola_Jones_detector(XMLFile);
		List<Rectangle> res=detector.getFaces(img.getAbsolutePath(), 1, 1.25f, 0.1f,1,true);

		System.out.println(res.size()+" faces found!");

    for (Rectangle rect : res ){
		  System.out.println("----");
		  System.out.println("width " + rect.getWidth());
		  System.out.println("height " + rect.getHeight());
		  System.out.println("x " + rect.getX());
		  System.out.println("y " + rect.getY());
    }

    d.setRects(res);
		setContentPane(d);
		this.setExtendedState(Frame.MAXIMIZED_BOTH);
		this.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent evt) {
            // Exit the application
            System.exit(0);
        }
    });
	}
}

class Dessin extends JPanel
{
	protected Image img;
	int img_width,img_height;
	List<Rectangle> res;
	public Dessin(Image img) {
		super();
		this.img=img;
		img_width=img.getWidth(null);
		img_height=img.getHeight(null);
		res=null;
	}
	
	public void paint(Graphics g)
	{
		Graphics2D g1 = (Graphics2D) g;
		g1.setColor(Color.red);
		g1.setStroke(new BasicStroke(2f));
		if(img==null)
			return;
		Dimension dim=getSize();
		//System.out.println("véridique");
		g1.clearRect(0,0, dim.width, dim.height);
		double scale_x=dim.width*1.f/img_width;
		double scale_y=dim.height*1.f/img_height;
		double scale=Math.min(scale_x, scale_y);
		int x_img=(dim.width-(int)(img_width*scale))/2;
		int y_img=(dim.height-(int)(img_height*scale))/2;
		g1.drawImage(img,x_img,y_img,(int)(img_width*scale),(int)(img_height*scale), null);
		if(res==null) return;
		
		for(Rectangle rect : res)
		{
		int w=(int) (rect.width*scale);
		int h=(int) (rect.height*scale);
		int x=(int) (rect.x*scale)+x_img;
		int y=(int) (rect.y*scale)+y_img;
		g1.drawRect(x,y,w,h);
		}
		
	}

	public void setRects(List<Rectangle> list) {
		this.res=list;
		repaint();
	}
	
}


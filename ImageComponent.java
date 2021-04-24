import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

public class ImageComponent extends JComponent {
  private BufferedImage img;

  public void setImg(BufferedImage ig){
    this.img = ig;
  }

  public void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g;
    g2.drawImage(img,0,0,this);
  }
}

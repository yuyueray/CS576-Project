import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class VideoPlayer extends JFrame implements ActionListener, Runnable {

  private static final int WIDTH = 512;
  private static final int HEIGHT = 512;
  private static final int FPS = 30;

  private JButton playButton, stopButton, pauseButton;
  private JPanel panel, videoPanel, buttonPanel;
  private ImageComponent iComp;
  private byte[] bytes;
  private BufferedImage img;
  private volatile boolean flag = true;
  private RandomAccessFile raf;
  private PlaySoundClip pSound;
  private ArrayList<RandomAccessFile> files;
  private int status, curFrame = 0;

  public void run(){
    play();
  }

  public VideoPlayer(ArrayList<RandomAccessFile> files, PlaySoundClip playSoundClip) {
    this.pSound = playSoundClip;
    this.files = files;
    img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    long len = WIDTH * HEIGHT * 3;
    bytes = new byte[(int) len];
    System.out.println(this.pSound);

    panel = new JPanel();
    videoPanel = new JPanel();
    buttonPanel = new JPanel();
    panel.setBorder(new EmptyBorder(5, 5, 5, 5));
    panel.setLayout(new BorderLayout(0, 0));

    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.setVisible(true);
    this.setContentPane(panel);
    this.setSize(600, 400);

    panel.add(videoPanel, BorderLayout.CENTER);
    videoPanel.setLayout(new BorderLayout(0, 0));

    playButton = new JButton("Play");
    stopButton = new JButton("Stop");
    pauseButton = new JButton("Pause");

    buttonPanel.add(playButton);
    buttonPanel.add(pauseButton);
    buttonPanel.add(stopButton);

    playButton.addActionListener(this);
    stopButton.addActionListener(this);
    pauseButton.addActionListener(this);
    panel.add(buttonPanel, BorderLayout.SOUTH);
  }

  public void play(){
    iComp = new ImageComponent();

    double soundSample = pSound.getSampleRate()/ FPS;

    int beginFrame = 0;
    int length = files.size();

    while(beginFrame<Math.round(pSound.getPosition()/soundSample)) {
      readImageRGB(WIDTH, HEIGHT, beginFrame % 2, img);
      iComp.setImg(img);
      videoPanel.add(iComp);
      videoPanel.repaint();
      videoPanel.setVisible(true);
      beginFrame++;
    }

    curFrame = beginFrame;
    //Video ahead of audio, wait for audio to catch up
    while(beginFrame > Math.round(pSound.getPosition()/soundSample));

    for (; curFrame < 30000; curFrame++) {
      while(!flag);

      // Video ahead of audio, wait for audio to catch up
      while(curFrame>Math.round(pSound.getPosition()/soundSample));

      // Audio ahead of video, roll video forward to catch up
      while(curFrame<Math.round(pSound.getPosition()/soundSample)) {
        readImageRGB(WIDTH, HEIGHT, curFrame % 2, img);
        iComp.setImg(img);
        videoPanel.add(iComp);
        videoPanel.repaint();
        videoPanel.setVisible(true);
        curFrame++;
      }

      long t1 = System.currentTimeMillis();
      readImageRGB(WIDTH, HEIGHT, curFrame % 2, img);
      iComp.setImg(img);
      videoPanel.add(iComp);
      videoPanel.repaint();
      videoPanel.setVisible(true);
      while(System.currentTimeMillis() - t1 < 33.3333);

    }

  }

  private void readImageRGB(int width, int height, int index, BufferedImage img)
  {
    try
    {
      int frameLength = width*height*3;
      raf = files.get(index);
      raf.seek(0);

      long len = frameLength;
      byte[] bytes = new byte[(int) len];

      raf.read(bytes);

      int ind = 0;
      for(int y = 0; y < height; y++)
      {
        for(int x = 0; x < width; x++)
        {
          byte r = bytes[ind];
          byte g = bytes[ind+height*width];
          byte b = bytes[ind+height*width*2];

          int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
          img.setRGB(x,y,pix);
          ind++;
        }
      }
    }
    catch (FileNotFoundException e)
    {
      e.printStackTrace();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    try {
      if (e.getSource() == pauseButton) {
        flag = false;
        pSound.pause();
        status = 1;

      } else if (e.getSource() == playButton) {
        flag = true;
        if(status == 1) {
          pSound.resumeAudio();
        }else if(status == 2){
          pSound.restart();
        }
        status = 0;
      } else {
        curFrame = 0;
        pSound.pause();
        status = 2;
      }
    }catch(Exception e1){
      e1.printStackTrace();
    }
  }

}
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class VideoPlayer extends JFrame implements ActionListener, Runnable {

  private static final int WIDTH = 320;
  private static final int HEIGHT = 180;
  private static final int FPS = 30;

  private JButton playButton, stopButton, pauseButton;
  private JPanel panel, videoPanel, buttonPanel;
  private ImageComponent iComp = new ImageComponent();
  private BufferedImage img;
  private volatile boolean flag = true;
  private RandomAccessFile raf;
  private PlaySoundClip pSound;
  private ArrayList<RandomAccessFile> files;
  private ArrayList<Integer> indices;
  private int status, curFrame, curIdx = 0;

  public void run(){
    play();
  }

  public VideoPlayer(ArrayList<RandomAccessFile> files, ArrayList<Integer> indices, PlaySoundClip playSoundClip) {
    this.pSound = playSoundClip;
    this.files = files;
    this.indices = indices;
    img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    long len = WIDTH * HEIGHT * 3;

    panel = new JPanel();
    videoPanel = new JPanel();
    buttonPanel = new JPanel();
    panel.setBorder(new EmptyBorder(5, 5, 5, 5)); // top, left, bot, right
    panel.setLayout(new BorderLayout(0, 0));

    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.setVisible(true);
    this.setContentPane(panel);
    this.setSize(350,300);

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
    System.out.println("Entered play()");
    double sampleRate = pSound.getSampleRate();
    double numSamplesPerFrame = sampleRate/ FPS;

    int curIdx = 0;
    int length = files.size();
    System.out.println("file size is "+length);
    // // audio precedes video
    // while(beginFrame<Math.round(pSound.getPosition()/numSamplesPerFrame)) {
    //   readImageRGB(WIDTH, HEIGHT, beginFrame, img);
    //   iComp.setImg(img);
    //   videoPanel.add(iComp);
    //   videoPanel.repaint();
    //   videoPanel.setVisible(true);
    //   beginFrame++;
    // }

    // //jumping to a new shot
    // while(beginFrame > Math.round(pSound.getPosition()/numSamplesPerFrame));
    
    curFrame = indices.get(curIdx);
    playVideo:
    for (; curIdx < length; curIdx++) {
      System.out.println("curFrame is "+curFrame);
      while(!flag);

      // audio precedes video
      while(curFrame<Math.round(pSound.getPosition()/numSamplesPerFrame)) {
        System.out.println("sound precedes: "+curIdx);
        readImageRGB(WIDTH, HEIGHT, curIdx, img);
        iComp.setImg(img);
        videoPanel.add(iComp);
        videoPanel.repaint();
        videoPanel.setVisible(true);
        if (++curIdx == length) break playVideo;
        curFrame = indices.get(curIdx);
      }

      //jumping to a new shot
      try {
        if (curFrame>Math.round(pSound.getPosition()/numSamplesPerFrame) + 10) {
          pSound.jump((long)Math.floor(curFrame / FPS * 1000000));
        }
      }
      catch (Exception e) {
        return;
      }

      // video precedes audio
      while(curFrame>Math.round(pSound.getPosition()/numSamplesPerFrame)) {};


      long t1 = System.currentTimeMillis();
      readImageRGB(WIDTH, HEIGHT, curIdx, img);
      iComp.setImg(img);
      videoPanel.add(iComp);
      videoPanel.repaint();
      videoPanel.setVisible(true);
      while(System.currentTimeMillis() - t1 < 33.3333);
    }
    
    curIdx = 0;
    curFrame = indices.get(curIdx);
    pSound.pause();
    status = 2;
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
          status = 0;
          pSound.resumeAudio();
        } else if(status == 2){
          status = 0;
          pSound.restart();
          play();
        }
        
      } else if (e.getSource() == stopButton) {
        flag = false;
        pSound.pause();
        status = 2;
      }
    } catch(Exception e1){
      e1.printStackTrace();
    }
  }

}

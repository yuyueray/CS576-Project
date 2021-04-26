import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;

public class Player{

  public static void main(String[] args){
    try {
      String video = args[0], audio = args[1];
      ArrayList<Integer> index = new ArrayList<Integer>();

      for(int i=0; i<16120; i++){
        index.add(i);
      }

      File folder = new File(video);
      int j = 0;
      File fa[] = folder.listFiles();
      Arrays.sort(fa, (f1, f2)-> {
        int n1 = Integer.parseInt(f1.getName().replaceAll("[^0-9]", ""));
        int n2 = Integer.parseInt(f2.getName().replaceAll("[^0-9]", ""));
        return n1 - n2;
      });
      ArrayList<RandomAccessFile> files = new ArrayList<>();
      for(int i=0; i<fa.length; i++){
        File f = fa[i];
        if(i == index.get(j)){
          ++j;
          RandomAccessFile rf = new RandomAccessFile(f, "r");
          files.add(rf);
        }
      }

      try {
        PlaySoundClip playSoundClip = new PlaySoundClip(audio);
        VideoPlayer player = new VideoPlayer(video, playSoundClip);

        Thread t1 = new Thread(playSoundClip);
        Thread t2 = new Thread(player);
        t1.start();
        t2.start();
      }
      catch (Exception e){
        e.printStackTrace();
      }
    }catch (FileNotFoundException e){
      e.printStackTrace();
    }

  }

}
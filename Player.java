import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class Player{

  public static void main(String[] args){
    //Add the Video player to this frame and pack
    try {
      String video1 = args[0], video2 = args[1], audio = args[2];
//
//      File folder = new File("");
//      for (final File fileEntry : folder.listFiles()) {
//        if (fileEntry.isDirectory()) {
//          listFilesForFolder(fileEntry);
//        } else {
//          System.out.println(fileEntry.getName());
//        }
//      }

      File file1 = new File(video1);
      File file2 = new File(video2);
      ArrayList<RandomAccessFile> files = new ArrayList<>();
      RandomAccessFile rf1 = new RandomAccessFile(file1, "r");
      RandomAccessFile rf2 = new RandomAccessFile(file2, "r");
      files.add(rf1);
      files.add(rf2);

      FileInputStream inputStream = new FileInputStream(audio);
      try {
        PlaySoundClip playSoundClip = new PlaySoundClip(audio);
        VideoPlayer player = new VideoPlayer(files, playSoundClip);

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
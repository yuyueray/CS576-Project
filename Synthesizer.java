import java.util.*;
import java.lang.*;
import java.io.*;



/** A synthesizer that takes an array of audio scores and an array of video scores to generate a final array of frame numbers
  * usage: java Synthesizer scores/test_video_audio_score.txt scores/test_video_video_score.txt
           java Synthesizer scores/test_video_3_audio_score.txt scores/test_video_3_video_score.txt
           java Synthesizer scores/soccer_audio_score.txt scores/soccer_video_score.txt
  */
public class Synthesizer{
    private static final double FPS = 30.0;
    private static final int maxShotLength = 4;
    private static final int maxVideoLength = 90; // compress videos to 90 seconds

    public static int[] getAudioScore(ArrayList<Integer> audioScores, int startFrameNo, int length) {
        int numFrames = 0, audioScore = 0;
        int startSec = (int)Math.ceil(((double)startFrameNo) / FPS);
        int endSec = (int)Math.floor((double)(startFrameNo+length-1) / FPS);
        
        if (endSec < startSec) {
            int[] res = {startFrameNo + length, 0};
            return res;
        }

        if (endSec - startSec < maxShotLength) {
            double sumScore = 0;
            for (int i = startSec; i < endSec; i++) {
                sumScore += audioScores.get(i); 
            }
            numFrames = length;
            audioScore = (int)(sumScore/(double)(endSec-startSec+1) * 10.0);
        } else {
            double sumScore = 0;
            double maxSumScore = 0;
            for (int i=startSec; i<=endSec-maxShotLength+1; i++) {
                sumScore = 0;
                for (int j=i; j<i+maxShotLength; j++) {
                    sumScore += audioScores.get(j);
                }
                maxSumScore = Math.max(maxSumScore, sumScore);
            }
            numFrames = maxShotLength * (int)FPS;
            audioScore = (int)(maxSumScore / (double)maxShotLength * 10.0);
        }
        int[] res = {startFrameNo + numFrames, (int)audioScore}; // endFrameNo, audioScore
        return res;
    }


    public static void main(String[] args){
        String audioScoreFile = args[0];
        String videoScoreFile = args[1];
        
        BufferedReader reader;

        // read audio scores into array
        ArrayList<Integer> audioScores = new ArrayList<Integer> ();
		try {
			reader = new BufferedReader(new FileReader(audioScoreFile));
			String line = reader.readLine();
			while (line != null) {
				audioScores.add((int)Double.parseDouble(line));
				// read next line
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

        // read video scores into array
        ArrayList<int[]> videoScores = new ArrayList<int[]>();
        try {
			reader = new BufferedReader(new FileReader(videoScoreFile));
			String line = reader.readLine();
			while (line != null) {
                String[] values = line.split(" ");
                int[] valueArray = new int[5];
                valueArray[0] = Integer.parseInt(values[0]); // start frame#
                valueArray[1] = Integer.parseInt(values[1]); // difference
                valueArray[2] = Integer.parseInt(values[2]); // movement
                valueArray[3] = Integer.parseInt(values[3]); // colorfulness
                valueArray[4] = Integer.parseInt(values[4]); // length
                videoScores.add(valueArray);
				// read next line
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        // shotScores: start frame#, end frame#, score
        ArrayList<int[]> shotScores = new ArrayList<int[]>();
        for (int i=0; i<videoScores.size(); i++) {
            // directly calculate scores for short shots
            int startFrameNo = videoScores.get(i)[0];
            int difference = videoScores.get(i)[1];
            int movement = videoScores.get(i)[2];
            int colorfulness = videoScores.get(i)[3];
            int length = videoScores.get(i)[4];
            int[] res = getAudioScore(audioScores, startFrameNo, length);
            int endFrameNo = res[0];
            int audioScore = res[1];

            int[] shotScore = new int[3];

            shotScore[0] = startFrameNo;
            shotScore[1] = endFrameNo;
            shotScore[2] = (int)(movement * 2.0) + (int)(colorfulness * 20) + (int)(audioScore * 50);
            // System.out.println("audioScore is "+audioScore);
            shotScores.add(shotScore);
        }


        // compute recommendation result
        ArrayList<int[]> res = new ArrayList<int[]>();
        shotScores.sort(Comparator.comparing(a -> -a[2]));
        int maxFrameLength = maxVideoLength * (int)FPS;
        int currFrameLength = 0;
        for (int i=0; i<shotScores.size(); i++) {
            if ((currFrameLength += (shotScores.get(i)[1] - shotScores.get(i)[0])) > maxFrameLength) {
                currFrameLength -= (shotScores.get(i)[1] - shotScores.get(i)[0]);
                break;
            }
            int[] frame = new int[2];
            frame[0] = shotScores.get(i)[0];
            frame[1] = shotScores.get(i)[1];
            res.add(frame);
        }
        res.sort(Comparator.comparing(a -> a[0]));

        // write result to file
        String[] videoScoreFileNames = videoScoreFile.split("/")[1].split("_");
        String synsResultFileName = "";
        for (int i=0; i<videoScoreFileNames.length-2; i++) {
            synsResultFileName += videoScoreFileNames[i] + "_";
        }
        String fileName = "results/"+synsResultFileName+"res.txt";
		try {
			BufferedWriter outFile = new BufferedWriter(new FileWriter(fileName));
            for (int i=0; i<res.size(); i++) {
                outFile.write(res.get(i)[0]+" "+res.get(i)[1]);
                outFile.newLine();
            }
            outFile.close();
        }
		catch (IOException e) {}
        // System.out.println(audioScores + " " + audioScores.size());
        // System.out.println(videoScores + " " + videoScores.size());

        // System.out.println(shotScores + " " + shotScores.size());

        // for (int i=0; i<shotScores.size(); i++) {
        //     System.out.println(shotScores.get(i)[0]+" "+shotScores.get(i)[1]+" "+shotScores.get(i)[2]);
        // }
        
        // System.out.println("----------------------------------------------------------------");
        // 
        System.out.println("***Selected Frames***");
        for (int i=0; i<res.size(); i++) {
            System.out.println(res.get(i)[0]+" "+res.get(i)[1]);
        }
        System.out.println("A total of " + currFrameLength + " frames are selected");
        System.out.println("*********************");
        // output: a list of (start frame#, end frame#)
    }

}
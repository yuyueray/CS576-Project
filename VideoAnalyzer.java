import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.lang.*;
import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime; 

public class VideoAnalyzer {

	JFrame frame;
	JLabel lbIm1;
	BufferedImage imgOne;
	int width = 320;
	int height = 180;
	int macroBlockSize = 10;
	int searchSize = 10;
	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void readImageRGB(int width, int height, String imgPath, BufferedImage img)
	{
		try
		{
			int frameLength = width*height*3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);

			int ind = 0;
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind+height*width];
					byte b = bytes[ind+height*width*2];

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
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

	public void showIms(String[] args){

		// Read a parameter from command line
		String param1 = args[1];
		System.out.println("The second parameter was: " + param1);

		// Read in the specified image
		imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, args[0], imgOne);

		// Use label to display the image
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		lbIm1 = new JLabel(new ImageIcon(imgOne));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);

		frame.pack();
		frame.setVisible(true);
	}

	public double[] getDiffAndMotionOfCurrBlock(int idxRow, int idxCol, byte[] currImgBytes, byte[] otherImgBytes) {
		// double minDiff = Double.MAX_VALUE;
		double currDiff = 0;
		// double motion = 0;
		double[] res = {Double.MAX_VALUE, 0}; // {min diff. motion}
		// determine the indices of the upper left corner of the current macroblock
		int iCurrStart = idxRow * macroBlockSize;
		int jCurrStart = idxCol * macroBlockSize;
		//System.out.println("!!!new block!!!");
		//System.out.println(iCurrStart + "," + jCurrStart);
		for (int xShift=-searchSize; xShift<=searchSize; xShift++) {
			for (int yShift=-searchSize; yShift<=searchSize; yShift++) {
				
				//System.out.println("here1: "+xShift+ ", "+yShift);
				// determine the indices of the upper left corner of the candidate block
				int iOtherStart = iCurrStart+xShift;
				int jOtherStart = iCurrStart+yShift;
				
				// if search range exceeds the boundary, skip the current loop
				if (iOtherStart < 0 || jOtherStart < 0 || iOtherStart > height - macroBlockSize || jOtherStart > width - macroBlockSize) {
					continue;
				}
				
				// compute the difference of the current macroblock to the candidate block
				currDiff = 0;
				for (int i=0; i<macroBlockSize; i++) {
					for (int j=0; j<macroBlockSize; j++) {
						int rCurr = Byte.toUnsignedInt(currImgBytes[(iCurrStart + i) * width + (jCurrStart + j)]); 
						int gCurr = Byte.toUnsignedInt(currImgBytes[(iCurrStart + i) * width + (jCurrStart + j) + height*width]); 
						int bCurr = Byte.toUnsignedInt(currImgBytes[(iCurrStart + i) * width + (jCurrStart + j) + height*width*2]); 

						int rOther = Byte.toUnsignedInt(otherImgBytes[(iOtherStart + i) * width + (jOtherStart + j)]); 
						int gOther = Byte.toUnsignedInt(otherImgBytes[(iOtherStart + i) * width + (jOtherStart + j) + height*width]); 
						int bOther = Byte.toUnsignedInt(otherImgBytes[(iOtherStart + i) * width + (jOtherStart + j) + height*width*2]); 

						currDiff += Math.sqrt((rCurr-rOther) * (rCurr-rOther) + (gCurr-gOther) * (gCurr-gOther) + (bCurr-bOther) * (bCurr-bOther));
					}
				}
				//System.out.println("  "+iOtherStart + "," + jOtherStart+","+currDiff);
				// update the value of min diff and the corresponding motion
				if (res[0] > currDiff) {
					res[0] = currDiff; // update min diff
					res[1] = Math.sqrt((xShift * xShift) + (yShift * yShift)); // update motion
					// if (xShift != -10 || yShift != -10) {
					// 	System.out.println("here2: "+xShift+", "+yShift);
					// }
				}
			}
		}
		
		return res;
	};

	public double[] getDiffAndMotion(String currImgPath, String otherImgPath) {
		
		double sumDiff = 0;
		double sumMotion = 0;
		try
		{
			// read current and other images into memory (as byte[])
			int frameLength = width*height*3;

			RandomAccessFile currImgRaf = new RandomAccessFile(currImgPath, "r");
			RandomAccessFile otherImgRaf = new RandomAccessFile(otherImgPath, "r");

			currImgRaf.seek(0);
			otherImgRaf.seek(0);

			long len = frameLength;
			byte[] currImgBytes = new byte[(int) len];
			byte[] otherImgBytes = new byte[(int) len];

			currImgRaf.read(currImgBytes);
			otherImgRaf.read(otherImgBytes);
			
			// compute the sum of difference for the current image
			int numBlocksPerRow = Math.floorDiv(width, macroBlockSize);
			int numBlocksPerCol = Math.floorDiv(height, macroBlockSize);

			for (int i=0; i<numBlocksPerCol; i++) {
				for (int j=0; j<numBlocksPerRow; j++) {
					double[] res = getDiffAndMotionOfCurrBlock(i, j, currImgBytes, otherImgBytes);
					sumDiff += res[0];
					sumMotion += res[1];
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

		double[] res = {sumDiff, sumMotion};
		return res;
	}

	public static void main(String[] args) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
   		LocalDateTime now = LocalDateTime.now();  
   		System.out.println(dtf.format(now));  
		
		VideoAnalyzer ren = new VideoAnalyzer();
		
		int start_idx = 0;
		int end_idx = 0;
		int threshold = 4000000; // 4,000,000 is a very good value for threshold
		int min_shot_length = 30; // shots shorter than 1s will be ignored
		int backtracking_rate = 2; // rate of backtracking after detecting a new shot
		double avgMotion = 0;
		String rgbFileDir = "../soccer";
		for (int i=min_shot_length; i<16200; i+=min_shot_length) {
			String currImagePath = rgbFileDir + "/frame" + i + ".rgb";
			String prevImagePath = rgbFileDir + "/frame" + (i-min_shot_length) + ".rgb";

			double diff = ren.getDiffAndMotion(currImagePath, prevImagePath)[0];

			findNewShot:
			if (diff > threshold) { 

				// backtrack to find the first frame of the current shot
				int curr_idx = i;
				avgMotion = 0;
				int num_backtracks = 0;
				currImagePath = rgbFileDir + "/frame" + curr_idx + ".rgb";
				prevImagePath = rgbFileDir + "/frame" + (curr_idx - backtracking_rate) + ".rgb";

				double backtrackDiff = ren.getDiffAndMotion(currImagePath, prevImagePath)[0];
				while (backtrackDiff <= threshold) {
					// update current index
					curr_idx -= backtracking_rate;
					
					// avoid wasting time in case a very long shot occurs
					if ((num_backtracks += backtracking_rate) == min_shot_length) {
						break findNewShot;
					}

					currImagePath = rgbFileDir + "/frame" + curr_idx + ".rgb";
					prevImagePath = rgbFileDir + "/frame" + (curr_idx-backtracking_rate) + ".rgb";

					backtrackDiff = ren.getDiffAndMotion(currImagePath, prevImagePath)[0];
				}

				// update end index of the current shot
				end_idx = curr_idx - 2;
				
				compute the avg. motion of the current shot, incrementing by 10 each time
				for (int j = start_idx + 10; j <= end_idx; j += 10) {
					currImagePath = rgbFileDir + "/frame" + j + ".rgb";
					prevImagePath = rgbFileDir + "/frame" + (j - 10) + ".rgb";
					avgMotion += ren.getDiffAndMotion(currImagePath, prevImagePath)[1];
				}

				if ((end_idx - start_idx) / 10 != 0) {
					avgMotion /= (double)((end_idx - start_idx) / 10);
				}
				
				System.out.println("frame " + curr_idx + ":: " + "s.f.#: " + start_idx + ", e.f.#: " + end_idx + ", diff: " + backtrackDiff + ", avg. motion: " + avgMotion);
				start_idx = curr_idx;
				// starting frame #, ending frame #, motion rate, color variance
			}
		}

		

		now = LocalDateTime.now();
   		System.out.println(dtf.format(now));
		
	}


}

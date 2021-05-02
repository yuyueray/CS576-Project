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


	public void readImage(String imgPath, byte[] imgBytes) {
		try
		{
			// read current and other images into memory (as byte[])
			RandomAccessFile imgRaf = new RandomAccessFile(imgPath, "r");
			imgRaf.seek(0);
			imgRaf.read(imgBytes);
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


	public double getDiffOfCurrBlock(int idxRow, int idxCol, byte[] currImgBytes, byte[] otherImgBytes) {
		double minDiff = Double.MAX_VALUE;
		double currDiff = 0;

		// determine the indices of the upper left corner of the current macroblock
		int iCurrStart = idxRow * macroBlockSize;
		int jCurrStart = idxCol * macroBlockSize;

		// compute the difference of the current macroblock to the candidate block
		for (int i=0; i<macroBlockSize; i++) {
			for (int j=0; j<macroBlockSize; j++) {
				int rCurr = Byte.toUnsignedInt(currImgBytes[(iCurrStart + i) * width + (jCurrStart + j)]); 
				int gCurr = Byte.toUnsignedInt(currImgBytes[(iCurrStart + i) * width + (jCurrStart + j) + height*width]); 
				int bCurr = Byte.toUnsignedInt(currImgBytes[(iCurrStart + i) * width + (jCurrStart + j) + height*width*2]); 

				int rOther = Byte.toUnsignedInt(otherImgBytes[(iCurrStart + i) * width + (jCurrStart + j)]); 
				int gOther = Byte.toUnsignedInt(otherImgBytes[(iCurrStart + i) * width + (jCurrStart + j) + height*width]); 
				int bOther = Byte.toUnsignedInt(otherImgBytes[(iCurrStart + i) * width + (jCurrStart + j) + height*width*2]); 

				currDiff += Math.sqrt((rCurr-rOther) * (rCurr-rOther) + (gCurr-gOther) * (gCurr-gOther) + (bCurr-bOther) * (bCurr-bOther));
			}
		}

		// update the value of mininum diff
		minDiff = Math.min(minDiff, currDiff);

		return minDiff;
	};

	public double getDiff(String currImgPath, String otherImgPath) {

		// read images
		int frameLength = width*height*3;
		byte[] currImgBytes = new byte[(int) frameLength];
		byte[] otherImgBytes = new byte[(int) frameLength];
		readImage(currImgPath, currImgBytes);
		readImage(otherImgPath, otherImgBytes);

		double sumDiff = 0;
		// compute the sum of difference for the current image
		int numBlocksPerRow = Math.floorDiv(width, macroBlockSize);
		int numBlocksPerCol = Math.floorDiv(height, macroBlockSize);

		for (int i=0; i<numBlocksPerCol; i++) {
			for (int j=0; j<numBlocksPerRow; j++) {
				sumDiff += getDiffOfCurrBlock(i, j, currImgBytes, otherImgBytes);
			}
		}

		return sumDiff;
	}

	public double getMotionOfCurrBlock(int idxRow, int idxCol, byte[] currImgBytes, byte[] otherImgBytes) {

		double currDiff = 0;
		double minDiff = Double.MAX_VALUE;
		double motion = 0;

		// determine the indices of the upper left corner of the current macroblock
		int iCurrStart = idxRow * macroBlockSize;
		int jCurrStart = idxCol * macroBlockSize;

		for (int xShift=-searchSize; xShift<=searchSize; xShift++) {
			for (int yShift=-searchSize; yShift<=searchSize; yShift++) {

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

				// update the value of min diff and the corresponding motion
				if (minDiff > currDiff) {
					minDiff = currDiff; // update min diff
					motion = Math.sqrt((xShift * xShift) + (yShift * yShift)); // update motion
				}
			}
		}

		return motion;
	};


	public double getMotion(String fileDir, int startingFrameIdx, int endingFrameIdx) {
		
		double sumMotion = 0;
		int numFrames = (endingFrameIdx - startingFrameIdx) / 10;

		for (int currIdx=startingFrameIdx+10; currIdx<endingFrameIdx; currIdx+=10) {
			String currImgPath = fileDir + "/frame" + currIdx + ".rgb";
			String otherImgPath = fileDir + "/frame" + (currIdx-0) + ".rgb";
			
			int frameLength = width*height*3;
			byte[] currImgBytes = new byte[(int) frameLength];
			byte[] otherImgBytes = new byte[(int) frameLength];
			readImage(currImgPath, currImgBytes);
			readImage(otherImgPath, otherImgBytes);
			
			// compute the sum of difference for the current image
			int numBlocksPerRow = Math.floorDiv(width, macroBlockSize);
			int numBlocksPerCol = Math.floorDiv(height, macroBlockSize);

			for (int i=0; i<numBlocksPerCol; i++) {
				for (int j=0; j<numBlocksPerRow; j++) {
					sumMotion += getMotionOfCurrBlock(i, j, currImgBytes, otherImgBytes);
				}
			}
		}

		return sumMotion/(double)numFrames;
	}

	public double getColorStdDev(byte[] imgBytes) {

		long Rmean = 0, Gmean = 0, Bmean = 0;
		for (int i=0; i<height; i++) {
			for (int j=0; j<width; j++) {
				Rmean += Byte.toUnsignedInt(imgBytes[i * width + j]);
				Gmean += Byte.toUnsignedInt(imgBytes[i * width + j + height * width]);
				Bmean += Byte.toUnsignedInt(imgBytes[i * width + j + height * width * 2]);
			}
		}

		Rmean /= (width * height);
		Gmean /= (width * height);
		Bmean /= (width * height);

		long Rvar = 0, Gvar = 0, Bvar = 0;
		for (int i=0; i<height; i++) {
			for (int j=0; j<width; j++) {
				Rvar += Math.pow( Byte.toUnsignedInt(imgBytes[i * width + j]) - Rmean, 2);
				Gvar += Math.pow( Byte.toUnsignedInt(imgBytes[i * width + j + height * width]) - Gmean, 2);
				Bvar += Math.pow( Byte.toUnsignedInt(imgBytes[i * width + j + height * width * 2]) - Bmean, 2);
			}
		}

		Rvar /= (width * height);
		Gvar /= (width * height);
		Bvar /= (width * height);

		return Math.sqrt((Rvar + Gvar + Bvar) / 3);
	}

	public double getColorfulness(String fileDir, int startingFrameIdx, int endingFrameIdx) {

		String firstImgPath = fileDir + "/frame" + startingFrameIdx + ".rgb";
		String lastImgPath = fileDir + "/frame" + (endingFrameIdx-1) + ".rgb";
		
		int frameLength = width*height*3;
		byte[] firstImgBytes = new byte[(int) frameLength];
		byte[] lastImgBytes = new byte[(int) frameLength];

		readImage(firstImgPath, firstImgBytes);
		readImage(lastImgPath, lastImgBytes);

		double sumColorStdDev = 0;
		sumColorStdDev += getColorStdDev(firstImgBytes);
		sumColorStdDev += getColorStdDev(lastImgBytes);
		return sumColorStdDev / 2;
	}

	public static void main(String[] args) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
   		LocalDateTime now = LocalDateTime.now();  
   		System.out.println(dtf.format(now));  

		VideoAnalyzer ren = new VideoAnalyzer();

		/* 
			info of the current shot:
				frame#        starting frame no., 
				diff          difference, 
				motion        how fast, 
				colorfulness  how colorful,
				length        length
		*/
		int frameInfo[][] = new int[16200][5]; // a maximum of 16200 frames, each having 4 parameters
		frameInfo[0][0] = 0;
		frameInfo[0][1] = 0;
		for (int i=1; i<frameInfo.length; i++) {
			frameInfo[i][0] = -1;
		}

		String rgbFileDir = "../../../../project_dataset_2/frames_rgb_2/steel"; // ../../../../project_dataset_2/frames_rgb_2/concert_2
		int prevFrameIdx = 0;
		for (int i=1; i<16200; i++) {
			String currImagePath = rgbFileDir + "/frame" + i + ".rgb";
			String prevImagePath = rgbFileDir + "/frame" + (i-1) + ".rgb";

			double diff = ren.getDiff(currImagePath, prevImagePath);
			if (diff > 4000000 && i-prevFrameIdx >= 15) { // diff is large enough && shot is longer than 0.5 seconds

				frameInfo[i][0] = i;
				frameInfo[i][1] = (int)diff;
				frameInfo[prevFrameIdx][2] = (int)(ren.getMotion(rgbFileDir, prevFrameIdx, i)); // double to int
				frameInfo[prevFrameIdx][3] = (int)(ren.getColorfulness(rgbFileDir, prevFrameIdx, i));
				frameInfo[prevFrameIdx][4] = i - prevFrameIdx;

				System.out.println(prevFrameIdx + ": " + diff + ", " + frameInfo[prevFrameIdx][2] + ", " + frameInfo[prevFrameIdx][3] + ", " + frameInfo[prevFrameIdx][4]);

				prevFrameIdx = i;
			}
		}

		frameInfo[prevFrameIdx][2] = (int)(ren.getMotion(rgbFileDir, prevFrameIdx, 16200));
		frameInfo[prevFrameIdx][3] = (int)(ren.getColorfulness(rgbFileDir, prevFrameIdx, 16200));
		frameInfo[prevFrameIdx][4] = 16200 - prevFrameIdx;

		//System.out.println("Array length is: "+frameInfo.length+" | "+frameInfoIdx);

		now = LocalDateTime.now();
   		System.out.println(dtf.format(now));

	}


}

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

	public double getDiffOfCurrBlock(int idxRow, int idxCol, byte[] currImgBytes, byte[] otherImgBytes) {
		double minDiff = Double.MAX_VALUE;
		double currDiff = 0;
		
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
			}
		}
		
		return minDiff;
	};

	public double getDiff(String currImgPath, String otherImgPath) {
		
		double sumDiff = 0;
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
					sumDiff += getDiffOfCurrBlock(i, j, currImgBytes, otherImgBytes);
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
		return sumDiff;
	}

	public static void main(String[] args) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
   		LocalDateTime now = LocalDateTime.now();  
   		System.out.println(dtf.format(now));  
		
		VideoAnalyzer ren = new VideoAnalyzer();
		
		String rgbFileDir = "../soccer";
		for (int i=6000; i<7000; i++) {
			String currImagePath = rgbFileDir + "/frame" + i + ".rgb";
			String prevImagePath = rgbFileDir + "/frame" + (i-1) + ".rgb";

			double diff = ren.getDiff(currImagePath, prevImagePath);

			if (diff > 4000000) { // 4,000,000 is a very good value for threshold
				System.out.println(i + ": "+ diff);
			}
		}

		now = LocalDateTime.now();
   		System.out.println(dtf.format(now));
		
	}


}

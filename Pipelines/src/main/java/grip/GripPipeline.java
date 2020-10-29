package grip;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashMap;

import org.opencv.core.*;
import org.opencv.core.Core.*;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.*;
import org.opencv.objdetect.*;

/**
* GripPipeline class.
*
* <p>An OpenCV pipeline generated by GRIP.
*
* @author GRIP
*/
public class GripPipeline {

	private HashMap<String, Object> params = new HashMap<>();
	
	public void setParam(String name, Object val) {
		params.put(name, val);
	}

	public Object getParam(String name) {
		return params.get(name);
	}

	public Set<String> getParamNames() {
		return params.keySet();
	}

	//Outputs
	private Mat blur0Output = new Mat();
	private Mat blur1Output = new Mat();

	
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);	}

	public GripPipeline() {
		setParam("blur0Radius", 9.649122807017552);
		setParam("blur1Radius", 69.2982456140351);
	}

	/**
	 * This is the primary method that runs the entire pipeline and updates the outputs.
	 */
	public void process(Mat source0) {
		// Step Blur0:
		// TODO: make this a class variable
		Mat blur0Input = source0;
		// TODO: make this a class variable
		BlurType blur0Type = BlurType.get("Box Blur");
		// TODO: make this a class variable
		double blur0Radius = (double) getParam("blur0Radius");
		blur(blur0Input, blur0Type, blur0Radius, blur0Output);

		// Step Blur1:
		// TODO: make this a class variable
		Mat blur1Input = blur0Output;
		// TODO: make this a class variable
		BlurType blur1Type = BlurType.get("Gaussian Blur");
		// TODO: make this a class variable
		double blur1Radius = (double) getParam("blur1Radius");
		blur(blur1Input, blur1Type, blur1Radius, blur1Output);

	}

	/**
	 * This method is a generated getter for the output of a Blur.
	 * @return Mat output from Blur.
	 */
	public Mat blur0Output() {
		return blur0Output;
	}

	/**
	 * This method is a generated getter for the output of a Blur.
	 * @return Mat output from Blur.
	 */
	public Mat blur1Output() {
		return blur1Output;
	}


	/**
	 * An indication of which type of filter to use for a blur.
	 * Choices are BOX, GAUSSIAN, MEDIAN, and BILATERAL
	 */
	enum BlurType{
		BOX("Box Blur"), GAUSSIAN("Gaussian Blur"), MEDIAN("Median Filter"),
			BILATERAL("Bilateral Filter");

		private final String label;

		BlurType(String label) {
			this.label = label;
		}

		public static BlurType get(String type) {
			if (BILATERAL.label.equals(type)) {
				return BILATERAL;
			}
			else if (GAUSSIAN.label.equals(type)) {
			return GAUSSIAN;
			}
			else if (MEDIAN.label.equals(type)) {
				return MEDIAN;
			}
			else {
				return BOX;
			}
		}

		@Override
		public String toString() {
			return this.label;
		}
	}

	/**
	 * Softens an image using one of several filters.
	 * @param input The image on which to perform the blur.
	 * @param type The blurType to perform.
	 * @param doubleRadius The radius for the blur.
	 * @param output The image in which to store the output.
	 */
	private void blur(Mat input, BlurType type, double doubleRadius,
		Mat output) {
		int radius = (int)(doubleRadius + 0.5);
		int kernelSize;
		switch(type){
			case BOX:
				kernelSize = 2 * radius + 1;
				Imgproc.blur(input, output, new Size(kernelSize, kernelSize));
				break;
			case GAUSSIAN:
				kernelSize = 6 * radius + 1;
				Imgproc.GaussianBlur(input,output, new Size(kernelSize, kernelSize), radius);
				break;
			case MEDIAN:
				kernelSize = 2 * radius + 1;
				Imgproc.medianBlur(input, output, kernelSize);
				break;
			case BILATERAL:
				Imgproc.bilateralFilter(input, output, -1, radius, radius);
				break;
		}
	}




}

package it.andreamaglie.cannyedge;

import java.awt.Component;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;

/**
 * Implements Canny edge detection algorithm
 * 
 * @author admin
 * 
 */
class EdgeDetector extends Component {
	private static final long serialVersionUID = 1L;

	// scale a 8 bit di modulo e direzione
	public static final double ORI_SCALE = 40.0D;
	public static final double MAG_SCALE = 20.0D;

	final float ORIENT_SCALE = 40F;
	private int height;
	private int width;
	private int picsize;
	private int data[];
	private int derivative_mag[];
	private int magnitude[];
	private int orientation[];
	private Image sourceImage;
	private Image edgeImage;
	// soglia superiore
	private int threshold1;
	// soglia inferiore
	private int threshold2;
	private int threshold;
	private int widGaussianKernel;
	private float sigma;
	int j1;

	// costruttore
	public EdgeDetector() {
		threshold1 = 10;
		threshold2 = 1;
		setThreshold(128);
		setGaussKernel(3);
		setSigma((float) 1.0);
	}

	// processa l'immagine
	public void process() throws Exception {
		if (threshold < 0 || threshold > 255) {
			throw new Exception("Threshold out of range.");
		}

		if (widGaussianKernel < 3 || widGaussianKernel > 40) {
			throw new Exception("widGaussianKernel out of its range.");
		}

		width = sourceImage.getWidth(this);
		height = sourceImage.getHeight(this);
		picsize = width * height;
		data = new int[picsize];
		magnitude = new int[picsize];
		orientation = new int[picsize];

		canny(sigma, widGaussianKernel);

		thresholding(threshold1, threshold2);

		for (int i = 0; i < picsize; i++) {
			if (data[i] <= threshold)
				data[i] = 0xff000000;
			else
				data[i] = -1;
		}

		edgeImage = pixels2image(data);
		data = null;
		magnitude = null;
		orientation = null;
	}

	// Algoritmo di Canny
	// i = gaussian kernel
	private void canny(float f, int gkernel) {
		boolean flag = false;
		boolean flag1 = false;

		derivative_mag = new int[picsize];

		float convy[] = new float[picsize];
		float convx[] = new float[picsize];
		// array delle medie gaussiane
		float meanGauss[] = new float[gkernel];
		float af5[] = new float[gkernel];
		float tmp1, tmp2, tmp3, tmp4, tmp5;
		float tmp6, tmp7, tmp8, tmp9, tmp10;
		float tmp11;

		data = image2pixels(sourceImage);

		int k4 = 0;

		// calcolo dei valori discreti
		// della distribuzione gaussiana
		do {
			System.out.println("k4 = " + k4);
			if (k4 >= gkernel)
				break;

			if (gauss(k4, f) <= 0.005F)
				break;

			// media gaussiana
			meanGauss[k4] = gauss(k4, f) + gauss((float) k4 - 0.5F, f)
					+ gauss((float) k4 + 0.5F, f);
			meanGauss[k4] = meanGauss[k4] / 3F / (6.283185F * f * f);
			af5[k4] = gauss((float) k4 + 0.5F, f) - gauss((float) k4 - 0.5F, f);
			k4++;
		} while (true);

		// convoluzione lungo x e lungo y con la gaussiana
		int j = k4;
		j1 = width - (j - 1);
		int l = width * (j - 1);
		int i1 = width * (height - (j - 1));

		for (int l4 = j - 1; l4 < j1; l4++) {
			for (int l5 = l; l5 < i1; l5 += width) {
				int k1 = l4 + l5;

				tmp1 = (float) data[k1] * meanGauss[0];
				tmp2 = tmp1;

				int l6 = 1;
				int k7 = k1 - width;

				for (int i8 = k1 + width; l6 < j; i8 += width) {
					tmp1 += meanGauss[l6] * (float) (data[k7] + data[i8]);
					tmp2 += meanGauss[l6]
							* (float) (data[k1 - l6] + data[k1 + l6]);
					l6++;
					k7 -= width;
				}
				// convoluzione lungo x con la gaussiana
				convy[k1] = tmp1;
				// convoluzione lungo y con la gaussiana
				convx[k1] = tmp2;
			}
		}

		// convoluzione dello smoothed con la derivata
		float sconvy[] = new float[picsize];

		for (int i5 = j - 1; i5 < j1; i5++) {
			for (int i6 = l; i6 < i1; i6 += width) {
				tmp1 = 0.0F;
				int l1 = i5 + i6;
				for (int i7 = 1; i7 < j; i7++)
					tmp1 += af5[i7] * (convy[l1 - i7] - convy[l1 + i7]);

				sconvy[l1] = tmp1;
			}
		}

		convy = null;
		float sconvx[] = new float[picsize];
		for (int j5 = k4; j5 < width - k4; j5++) {
			for (int j6 = l; j6 < i1; j6 += width) {
				tmp1 = 0.0F;
				int i2 = j5 + j6;
				int j7 = 1;
				for (int l7 = width; j7 < j; l7 += width) {
					tmp1 += af5[j7] * (convx[i2 - l7] - convx[i2 + l7]);
					j7++;
				}
				sconvx[i2] = tmp1;
			}

		}
		convx = null;

		// non-maximal suppression
		j1 = width - j;
		l = width * j;
		i1 = width * (height - j);
		for (int k5 = j; k5 < j1; k5++) {
			for (int k6 = l; k6 < i1; k6 += width) {
				int j2 = k5 + k6;
				int k2 = j2 - width;
				int l2 = j2 + width;
				int i3 = j2 - 1;
				int j3 = j2 + 1;
				int k3 = k2 - 1;
				int l3 = k2 + 1;
				int i4 = l2 - 1;
				int j4 = l2 + 1;
				tmp1 = sconvy[j2];
				tmp2 = sconvx[j2];
				float f12 = modulus(tmp1, tmp2);

				int k = (int) ((double) f12 * MAG_SCALE);

				if (k >= 256)
					derivative_mag[j2] = 255;
				else
					derivative_mag[j2] = k;

				tmp3 = modulus(sconvy[k2], sconvx[k2]);
				tmp4 = modulus(sconvy[l2], sconvx[l2]);
				tmp5 = modulus(sconvy[i3], sconvx[i3]);
				tmp6 = modulus(sconvy[j3], sconvx[j3]);
				tmp7 = modulus(sconvy[k3], sconvx[k3]);
				tmp8 = modulus(sconvy[l3], sconvx[l3]);
				tmp9 = modulus(sconvy[i4], sconvx[i4]);
				tmp10 = modulus(sconvy[j4], sconvx[j4]);
				boolean vabene = false;

				// se y*x <= 0 (ci troviamo nel secondo o quarto quadrante)
				if (tmp1 * tmp2 <= 0) {
					// se y >= x
					if (Math.abs(tmp1) >= Math.abs(tmp2)) {

						if (Math.abs(tmp1 * f12) >= Math.abs(tmp2 * tmp8
								- (tmp1 + tmp2) * tmp6)
								&& Math.abs(tmp1 * f12) > Math.abs(tmp2 * tmp9
										- (tmp1 + tmp2) * tmp5)) {
							vabene = true;
						} else
							vabene = false;
					} else if (Math.abs(tmp2 * f12) >= Math.abs(tmp1 * tmp8
							- (tmp2 + tmp1) * tmp3)
							&& Math.abs(tmp2 * f12) > Math.abs(tmp1 * tmp9
									- (tmp2 + tmp1) * tmp4)) {
						vabene = true;
					} else
						vabene = false;
				} else {
					if (Math.abs(tmp1) >= Math.abs(tmp2)) {
						if (Math.abs(tmp1 * f12) >= Math.abs(tmp2 * tmp10
								+ (tmp1 - tmp2) * tmp6)
								&& Math.abs(tmp1 * f12) > Math.abs(tmp2 * tmp7
										+ (tmp1 - tmp2) * tmp5)) {
							vabene = true;
						} else
							vabene = false;
					} else if (Math.abs(tmp2 * f12) >= Math.abs(tmp1 * tmp10
							+ (tmp2 - tmp1) * tmp4)
							&& Math.abs(tmp2 * f12) > Math.abs(tmp1 * tmp7
									+ (tmp2 - tmp1) * tmp3)) {
						vabene = true;
					} else
						vabene = false;
				}
				if (vabene) {
					magnitude[j2] = derivative_mag[j2];
					orientation[j2] = (int) (Math.atan2(tmp2, tmp1) * ORI_SCALE);
				}
			}
		}

		derivative_mag = null;
		sconvy = null;
		sconvx = null;
	}

	// ritorna 0 se sono entrambi zero,
	// altrimenti ritorna il modulo
	private float modulus(float f, float f1) {
		if (f == 0.0F && f1 == 0.0F)
			return 0.0F;
		else
			return (float) Math.sqrt(f * f + f1 * f1);
	}

	// funzione gaussiana
	private float gauss(float f, float f1) {
		return (float) Math.exp((-f * f) / ((float) 2 * f1 * f1));
	}

	// thresholding con isteresi
	private void thresholding(int i, int j) {
		if (i < j) {
			System.out.println("Errore: soglia superiore < soglia inferiore!");
		} else {
			for (int k = 0; k < picsize; k++)
				data[k] = 0;

			// per ogni lato con magnitude maggiore della soglia superiore
			// traccia i lati che sono maggiori della soglia inferiore
			for (int l = 0; l < width; l++) {
				for (int i1 = 0; i1 < height; i1++)
					if (magnitude[l + width * i1] >= i)
						linking(l, i1, j);
			}
		}
	}

	// k è la soglia inferiore
	private boolean linking(int i, int j, int k) {
		j1 = i + 1;
		int k1 = i - 1;
		int l1 = j + 1;
		int i2 = j - 1;
		int j2 = i + j * width;
		if (l1 >= height)
			l1 = height - 1;
		if (i2 < 0)
			i2 = 0;
		if (j1 >= width)
			j1 = width - 1;
		if (k1 < 0)
			k1 = 0;

		if (data[j2] == 0) {
			data[j2] = magnitude[j2];
			boolean flag = false;
			int l = k1;

			do {
				if (l > j1)
					break;
				int i1 = i2;
				do {
					if (i1 > l1)
						break;
					int k2 = l + i1 * width;
					if ((i1 != j || l != i) && magnitude[k2] >= k
							&& linking(l, i1, k)) {
						flag = true;
						break;
					}
					i1++;
				} while (true);

				if (!flag)
					break;
				l++;
			} while (true);
			return true;
		} else {
			return false;
		}
	}

	private Image pixels2image(int ai[]) {
		MemoryImageSource memoryimagesource = new MemoryImageSource(width,
				height, ColorModel.getRGBdefault(), ai, 0, width);

		return Toolkit.getDefaultToolkit().createImage(memoryimagesource);
	}

	private int[] image2pixels(Image image) {
		int ai[] = new int[picsize];
		PixelGrabber pixelgrabber = new PixelGrabber(image, 0, 0, width,
				height, ai, 0, width);
		try {
			pixelgrabber.grabPixels();
		} catch (InterruptedException interruptedexception) {
			interruptedexception.printStackTrace();
		}
		boolean flag = false;
		int k1 = 0;
		do {
			if (k1 >= 16)
				break;
			int i = (ai[k1] & 0xff0000) >> 16;
			int k = (ai[k1] & 0xff00) >> 8;
			int i1 = ai[k1] & 0xff;
			if (i != k || k != i1) {
				flag = true;
				break;
			}
			k1++;
		} while (true);
		if (flag) {
			for (int l1 = 0; l1 < picsize; l1++) {
				int j = (ai[l1] & 0xff0000) >> 16;
				int l = (ai[l1] & 0xff00) >> 8;
				j1 = ai[l1] & 0xff;
				ai[l1] = (int) (0.29799999999999999D * (double) j
						+ 0.58599999999999997D * (double) l + 0.113D * (double) j1);
			}

		} else {
			for (int i2 = 0; i2 < picsize; i2++)
				ai[i2] = ai[i2] & 0xff;
		}
		return ai;
	}

	public void setSourceImage(Image image) {
		sourceImage = image;
	}

	public Image getEdgeImage() {
		return edgeImage;
	}

	public void setThreshold(int i) {
		threshold = i;
		System.out.println("Threshold: " + i);
	}

	public void setHighThreshold(int i) {
		threshold1 = i;
		System.out.println("High Threshold: " + i);
	}

	public void setLowThreshold(int i) {
		threshold2 = i;
		System.out.println("Low Threshold: " + i);
	}

	public void setGaussKernel(int i) {
		widGaussianKernel = i;
		System.out.println("Gauss kernel: " + i);
	}

	public void setSigma(float i) {
		sigma = i;
		System.out.println("sigma: " + i);
	}
}
package com.example.rosannacatte.cbirsoftwarever2;

import android.graphics.Color;
import android.provider.ContactsContract;
import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.opencv.core.CvType.CV_64F;
import static org.opencv.core.CvType.CV_64FC3;
import static org.opencv.core.CvType.CV_8UC;
import static org.opencv.core.CvType.CV_8UC3;

/**
 * Created by root on 20/01/17.
 */

public class ImageDescriptor {

    //CLASS FIELDS
    private static final int DIMENSION = 25;
    private static final int VECTOR_DIMENSION = 125;
    private static final int TOTAL_DIMENSION = 375;


    //Mat of int che contiene il numero di bins per il nostro istogramma (8*12*3)
    private MatOfInt mNumberBins = new MatOfInt(5, 5);


    //Mat of int che contiene gli indici per i channels (0 1 2)
    private MatOfInt[] mChannels = new MatOfInt[]{
            new MatOfInt(0, 1),
            new MatOfInt(0, 2),
            new MatOfInt(1, 2),

    };


    //Mat of float che contiene il range dell'istogramma
    private MatOfFloat[] mRange = new MatOfFloat[]{
            new MatOfFloat(0f, 180f, 0f, 256f),
            new MatOfFloat(0f, 180f, 0f, 256f),
            new MatOfFloat(0f, 256f, 0f, 256f)
    };


    //COSTRUTTORE
    /*
    public ImageDescriptor(MatOfInt mNumberBins, MatOfInt mChannels, MatOfFloat mRange){
        this.mNumberBins = mNumberBins;
        this.mChannels = mChannels;
        this.mRange = mRange;
    }*/


    //METODI ISTOGRAMMA DI COLORE
    //Prende in ingresso un immagine in formato HSV e calcola l'istogramma
    public String[] calculateHist(Mat immagine) {

        Mat mask1 = new Mat(600, 800, CvType.CV_8UC1);
        Mat mask2 = new Mat(600, 800, CvType.CV_8UC1);
        Mat mask3 = new Mat(600, 800, CvType.CV_8UC1);
        Mat mask4 = new Mat(600, 800, CvType.CV_8UC1);
        Mat mask5 = new Mat(600, 800, CvType.CV_8UC1);

        impostaMask(mask1, mask2, mask3, mask4, mask5);

        //Non avendo maschere per il momento passo un Mat object vuoto
        //Parametri:
        /*
        * 1) una lista di immagini su cui calcolare, noi ne passiamo solo una
        * 2) gli indici dei canali su cui calcolare l'istogramma, nel nostro caso sono 3 (0, 1, 2) per l'HSV
        * 3) una eventuale maschera, nel nostro caso non ne stiamo usando
        * 4) oggetto Mat di destinazione dell'istogramma
        * 5) un MatOfInt contenente il numero di bins per canale
        * 6) un MatOfFloat contenente il range di valori per canale
        * */
        ArrayList<Mat> immagini = new ArrayList<>();
        scalaImmagine(immagine);
        immagini.add(immagine);

        //Oggetto Mat che contiene l'istogramma.

        Mat hist1 = new Mat();
        Mat hist2 = new Mat();
        Mat hist3 = new Mat();
        Mat hist4 = new Mat();
        Mat hist5 = new Mat();

        //Ho bisogno di un vettore per contenere tutti i valori

        float[] data1 = new float[DIMENSION];
        float[] data2 = new float[DIMENSION];
        float[] data3 = new float[DIMENSION];
        float[] data4 = new float[DIMENSION];
        float[] data5 = new float[DIMENSION];

        float[] dataHSHVSV = new float[TOTAL_DIMENSION];
        float[][] data = new float[VECTOR_DIMENSION][3];

        for (int c = 0; c < 3; c++) {
            Imgproc.calcHist(immagini, mChannels[c], mask1, hist1, mNumberBins, mRange[c]);
            Imgproc.calcHist(immagini, mChannels[c], mask2, hist2, mNumberBins, mRange[c]);
            Imgproc.calcHist(immagini, mChannels[c], mask3, hist3, mNumberBins, mRange[c]);
            Imgproc.calcHist(immagini, mChannels[c], mask4, hist4, mNumberBins, mRange[c]);
            Imgproc.calcHist(immagini, mChannels[c], mask5, hist5, mNumberBins, mRange[c]);


            hist1.get(0, 0, data1);
            hist2.get(0, 0, data2);
            hist3.get(0, 0, data3);
            hist4.get(0, 0, data4);
            hist5.get(0, 0, data5);

            for (int j = 0; j < DIMENSION - 1; j++) {
                data[j][c] = data1[j];
                data[j + DIMENSION][c] = data2[j];
                data[j + 2 * DIMENSION][c] = data3[j];
                data[j + 3 * DIMENSION][c] = data4[j];
                data[j + 4 * DIMENSION][c] = data5[j];
            }


        }

        for (int i = 0; i < VECTOR_DIMENSION - 1; i++) {
            dataHSHVSV[i] = data[i][0];
            dataHSHVSV[i + VECTOR_DIMENSION] = data[i][1];
            dataHSHVSV[i + 2 * VECTOR_DIMENSION] = data[i][2];
        }


        //Vettore di feature che descrivono l'immagine
        //Usando un istogramma HSV con(8*12*3) salverò 288 valori
        //COME PRIMO TENTATIVO NON USO MASCHERE, DUNQUE UN UNICO VETTORE DI FEATURE
        String[] features = convertToString(dataHSHVSV);


        return features;
    }

    //Imposta la maschera le maschere da usare per il calcolo dell'istogramma
    private void impostaMask(Mat mask1, Mat mask2, Mat mask3, Mat mask4, Mat mask5) {

        //Azzero tutti i pixel
        mask1.setTo(new Scalar(0.0));
        mask2.setTo(new Scalar(255.0));
        mask3.setTo(new Scalar(255.0));
        mask4.setTo(new Scalar(255.0));
        mask5.setTo(new Scalar(255.0));


        //Inizializzo i punti per tracciare i rettangoli

        Point p1 = new Point(200.0, 150.0);
        Point p2 = new Point(600.0, 450.0);

        Point p3 = new Point(0, 0);
        Point p4 = new Point(400, 300);

        Point p5 = new Point(0, 300);
        Point p6 = new Point(400, 600);

        Point p7 = new Point(400, 0);
        Point p8 = new Point(800, 300);

        Point p9 = new Point(400, 300);
        Point p10 = new Point(800, 600);


        //Mask 1
        Imgproc.rectangle(mask1, p1, p2, new Scalar(255.0));

        //Mask 2
        Imgproc.rectangle(mask2, p1, p2, new Scalar(0.0));
        Imgproc.rectangle(mask2, p3, p4, new Scalar(0.0));
        Imgproc.rectangle(mask2, p5, p6, new Scalar(0.0));
        Imgproc.rectangle(mask2, p7, p8, new Scalar(0.0));

        //Mask 3
        Imgproc.rectangle(mask3, p1, p2, new Scalar(0.0));
        Imgproc.rectangle(mask3, p5, p6, new Scalar(0.0));
        Imgproc.rectangle(mask3, p7, p8, new Scalar(0.0));
        Imgproc.rectangle(mask3, p9, p10, new Scalar(0.0));

        //Mask 4
        Imgproc.rectangle(mask4, p1, p2, new Scalar(0.0));
        Imgproc.rectangle(mask4, p3, p4, new Scalar(0.0));
        Imgproc.rectangle(mask4, p5, p6, new Scalar(0.0));
        Imgproc.rectangle(mask4, p9, p10, new Scalar(0.0));

        //Mask 5
        Imgproc.rectangle(mask5, p1, p2, new Scalar(0.0));
        Imgproc.rectangle(mask5, p3, p4, new Scalar(0.0));
        Imgproc.rectangle(mask5, p7, p8, new Scalar(0.0));
        Imgproc.rectangle(mask5, p9, p10, new Scalar(0.0));


    }

    //Converte i dati contenuti nell'istogramma in stringhe salvabili nel shared preference
    private String[] convertToString(float[] data) {

        String[] stringVector = new String[TOTAL_DIMENSION];

        //Ciclo tutto il vettore di float e converto in stringhe
        for (int i = 0; i < stringVector.length; i++) {
            //Sto usando questa formattazione per permettermi di salvare i valore in un shared Preference successivamente
            stringVector[i] = String.format(i + "_%.0f", data[i]);
        }

        return stringVector;
    }

    //Il seguente metodo serve per scalare l'immagine passata come argomento
    private Mat scalaImmagine(Mat immagine) {
        //VOGLIO SCALARE UN IMMAGINE ALLA DIMENSIONE 800*600
        //Verifico se la dimensione è rispettata
        int height = immagine.height();
        int width = immagine.width();

        if (height != 600 || width != 800) {
            //Trovo il rapporto tra la misura desiderata e quelle attuale
            double heigthScaleSize = (double) 600 / height;
            double widthScaleSize = (double) 800 / width;

            Imgproc.resize(immagine, immagine, new Size(), widthScaleSize, heigthScaleSize, Imgproc.INTER_AREA);
        }

        return immagine;


    }




    //SETTER GETTER
    public void setmNumberBins(MatOfInt mNumberBins) {
        this.mNumberBins = mNumberBins;
    }

    public void setmChannels(MatOfInt[] mChannels) {
        this.mChannels = mChannels;
    }

    public void setmRange(MatOfFloat[] mRange) {
        this.mRange = mRange;
    }

    public MatOfInt getmNumberBins() {
        return this.mNumberBins;
    }

    public MatOfInt[] getmChannels() {
        return this.mChannels;
    }

    public MatOfFloat[] getmRange() {
        return this.mRange;
    }


    // METODI DESCRITTORE ORB

    public ImmagineOrb calculateOrb(Mat immagine) {
        //Immagine 600x800
        scalaImmagine(immagine);

        // Creazione Feature Detector
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);

        //Creazione estrattore di feature
        DescriptorExtractor descriptor = DescriptorExtractor.create(DescriptorExtractor.ORB);

        //Conversione immagine in scala di grigi
        Mat greyImage = new Mat();
        greyImage = immagine;
        Imgproc.cvtColor(immagine, greyImage, Imgproc.COLOR_RGB2GRAY);

        //Oggetto Mat che conterrà le features dell'immagine
        Mat descriptors = new Mat();
        // Oggetto Mat che conterrà i keypoint dell'immagine
        MatOfKeyPoint keypoints = new MatOfKeyPoint();

        //Individuazione punti chiave dell'immagine
        detector.detect(greyImage, keypoints);

        //Estrazione feature dai punti chiave
        descriptor.compute(greyImage, keypoints, descriptors);

        // Le informazioni sull'immagine vengono salvate in un oggetto Immagine Orb
        ImmagineOrb immagineAnalizzata = new ImmagineOrb(keypoints,descriptors,null);

        return immagineAnalizzata;

    }


}
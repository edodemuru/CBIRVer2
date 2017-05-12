package com.example.rosannacatte.cbirsoftwarever2;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * Created by root on 27/01/17.
 */

public class ImmagineDaMostrare {

    //FIELDS
    private String percorsoImmagine;
    private Mat immagineMAT;
    private Bitmap immagineBM;
    private String nomeImmagine;
    private int distanza;


    //COSTRUTTORE
    public ImmagineDaMostrare(String percorsoImmagine, int distanza){
        this.distanza = distanza;
        this.percorsoImmagine = percorsoImmagine;
        this.immagineMAT = Imgcodecs.imread(percorsoImmagine);
        ricavaBitMap();
        ricavaNomeImmagine();
    }


    //METODI
    private void ricavaNomeImmagine(){
        int index = percorsoImmagine.lastIndexOf("/");
        this.nomeImmagine = percorsoImmagine.substring(index + 1);
    }

    private void ricavaBitMap(){
        Imgproc.cvtColor(immagineMAT, immagineMAT, Imgproc.COLOR_BGR2RGB);
        immagineMAT = scalaImmagine(immagineMAT);
        immagineBM = Bitmap.createBitmap(immagineMAT.cols(), immagineMAT.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(immagineMAT, immagineBM);
    }

    //Il seguente metodo serve per scalare l'immagine passata come argomento
    private Mat scalaImmagine(Mat immagine){
        //VOGLIO SCALARE UN IMMAGINE ALLA DIMENSIONE 800*600
        //Verifico se la dimensione è rispettata
        int height = immagine.height();
        int width = immagine.width();

        if(height != 600 || width != 800){
            //Trovo il rapporto tra la misura desiderata e quelle attuale
            double heigthScaleSize = (double) 600/height;
            double widthScaleSize = (double) 800/width;

            Imgproc.resize(immagine, immagine, new Size(), widthScaleSize, heigthScaleSize, Imgproc.INTER_AREA);
        }

        return immagine;


    }


    //SETTER E GETTER
    public void setPercorsoImmagine(String percorsoImmagine){
        this.percorsoImmagine = percorsoImmagine;
        this.immagineMAT = Imgcodecs.imread(percorsoImmagine);
        Utils.matToBitmap(immagineMAT, immagineBM);
    }

    public String getPercorsoImmagine(){
        return this.percorsoImmagine;
    }

    public Bitmap getImmagineBM(){
        return this.immagineBM;
    }

    public String getNomeImmagine(){
        return this.nomeImmagine;
    }

    public int getDistanza(){
        return  this.distanza;
    }

}
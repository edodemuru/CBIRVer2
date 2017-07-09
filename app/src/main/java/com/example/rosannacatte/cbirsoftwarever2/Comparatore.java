package com.example.rosannacatte.cbirsoftwarever2;

import android.content.SharedPreferences;
import android.util.Log;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.imgcodecs.Imgcodecs;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.opencv.core.CvType.CV_8UC1;

/**
 * Created by root on 24/01/17.
 */

public class Comparatore {

    //CAMPI
    private static final String TAG = "Comparatore";
    private static final double SMALL_NUMBER = 0.000000000001;

    private static final int TOTAL_DIMENSION_WITH_HIST = 375;
    private static final int TOTAL_DIMENSION_WITH_ORB = 16000;


    //Elenco percorso immagini salvate sullo smartphone
    private ArrayList<String> listaPercorsoImmagini;


    //Shared preference su cui sono salvati i valori delle features
    private SharedPreferences preferences;

    //ImageDescriptor usato per ricavare gli istogrammi
    private ImageDescriptor imageDescriptor = new ImageDescriptor();

    private ArrayList<ImmagineOrb> immaginiAnalizzate = new ArrayList<>();


    //COSTRUTTORE
    public Comparatore(ArrayList<String> listaPercorsoImmagini, SharedPreferences preferences, ArrayList<ImmagineOrb> immaginiAnalizzate) {
        this.listaPercorsoImmagini = new ArrayList<>();
        this.listaPercorsoImmagini = listaPercorsoImmagini;
        this.preferences = preferences;
        this.immaginiAnalizzate = immaginiAnalizzate;
    }


    //METODI

    //Metodo principale che calcola il vettore di indici di somiglianza tra le immagini
    public ArrayList<ImmagineDaMostrare> calcolaDistanzaIst(Mat queryImage) {

        //Recupero i valori delle features dell'immagine query
        int[] valoriFeaturesQuery = recuperaValoreFeaturesHist(imageDescriptor.calculateHist(queryImage));

        //Elenco percorso immagini da mostrare
        ArrayList<ImmagineDaMostrare> listaPercorsoImmaginiDaMostrare = new ArrayList<>();

        //Adesso devo fare il confronto tra le features dell'immagine di query e le features delle immagini contenute nel dispositivo
        for (int i = 0; i < listaPercorsoImmagini.size(); i++) {

            String currentImage = listaPercorsoImmagini.get(i);

            if (Imgcodecs.imread(currentImage).channels() == 3) {

                //Recupero le features dallo shared preference sotto forma di set
                Set<String> setFeatures = preferences.getStringSet(currentImage, null);


                //Converto il set di stringhe in un array
                String[] arrayFeatures = setToArray(setFeatures);

                //Ottengo i valori delle features salvate nello shared preference
                int[] valoriFeaturesSavedImage = recuperaValoreFeaturesHist(arrayFeatures);

                //Calcolo la distanza tra i due vettori
                int currentDistanza = distanza(valoriFeaturesQuery, valoriFeaturesSavedImage);



                if (currentDistanza == 0) {
                    //In questo caso l'immagine è identica, non la mostro di nuovo

                } else {

                    // Imposto la distanza calcolata con l'istogramma, mentre quella relativa all'algoritmo orb a -1
                    ImmagineDaMostrare immagineDaMostrare = new ImmagineDaMostrare(currentImage);
                    immagineDaMostrare.setDistanzaIst(currentDistanza);
                    immagineDaMostrare.setDistanzaOrb(-1);

                    listaPercorsoImmaginiDaMostrare.add(immagineDaMostrare);
                }


            /*
            ImmagineDaMostrare immagineDaMostrare = new ImmagineDaMostrare(currentImage, currentDistanza);
            listaPercorsoImmaginiDaMostrare.add(immagineDaMostrare);
            */
            }

        }

        //Ho popolato la mappa di valori. Devo restituire l'arrayList di URI delle immagini da mostrare


        return ordinaArrayList(listaPercorsoImmaginiDaMostrare);
    }

    //Calcolo distanza tra query image e immagini indicizzate con algoritmo orb
    public ArrayList<ImmagineDaMostrare> calcolaDistanzaOrb(Mat queryImage) {

        //Calcolo keypoints e features per l'immagine di query
        ImmagineOrb queryImageData = imageDescriptor.calculateOrb(queryImage);

        // Inserisco i valori delle features dell'immagine di query in un oggetto Mat
        //Mat valoriFeaturesQueryMat = new Mat(500,32,CV_8UC1);
        //valoriFeaturesQueryMat.put(0,0,valoriFeaturesQuery);

        //Elenco percorso immagini da mostrare
        ArrayList<ImmagineDaMostrare> listaPercorsoImmaginiDaMostrare = new ArrayList<>();

        for (int i = 0; i < immaginiAnalizzate.size(); i++) {

            Mat descriptorsImage = immaginiAnalizzate.get(i).getDescriptors();
            MatOfKeyPoint keypointsImage = immaginiAnalizzate.get(i).getKeypoints();

            Log.i(TAG," numero di descrittori e di keypoints " + descriptorsImage.total() + " " + keypointsImage.total());

            Mat descriptorsQuery = queryImageData.getDescriptors();
            MatOfKeyPoint keypointsQuery = queryImageData.getKeypoints();


            //Calcolo della distanza tra i due vettori di byte calcolati

            //Creazione di un matcher
            DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

            MatOfDMatch filteredMatches = new MatOfDMatch();

            //MATCHING
            List<MatOfDMatch> matches = new ArrayList<MatOfDMatch>();

            // Calcola i due match migliori tra prima e seconda immagine
            matcher.knnMatch(descriptorsQuery, descriptorsImage, matches, 2);

            // Ratio test
            LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
            for (Iterator<MatOfDMatch> iterator = matches.iterator(); iterator.hasNext(); ) {
                MatOfDMatch matOfDMatch = (MatOfDMatch) iterator.next();
                if (matOfDMatch.toArray()[0].distance / matOfDMatch.toArray()[1].distance < 0.9) {
                    good_matches.add(matOfDMatch.toArray()[0]);
                }
            }

            // Individuazione coordinate dei keypoints di good_matches, per calcolare l'omografia
            // e rimuovere valori anomali con RANSAC
            List<Point> pts1 = new ArrayList<Point>();
            List<Point> pts2 = new ArrayList<Point>();
            for (int j = 0; j < good_matches.size(); j++) {
                pts1.add(keypointsQuery.toList().get(good_matches.get(j).queryIdx).pt);
                pts2.add(keypointsImage.toList().get(good_matches.get(j).trainIdx).pt);
            }

            // Conversione dei tipi di dato
            Mat outputMask = new Mat();
            MatOfPoint2f pts1Mat = new MatOfPoint2f();
            pts1Mat.fromList(pts1);
            MatOfPoint2f pts2Mat = new MatOfPoint2f();
            pts2Mat.fromList(pts2);

            // Find homography - here just used to perform match filtering with RANSAC, but could be used to e.g. stitch images
            // the smaller the allowed reprojection error (here 15), the more matches are filtered
            Mat Homog = Calib3d.findHomography(pts1Mat, pts2Mat, Calib3d.RANSAC, 15, outputMask, 2000, 0.995);

            // outputMask contiente 0 e 1 che indicano quali match sono stati filtrati
            LinkedList<DMatch> better_matches = new LinkedList<DMatch>();
            for (int j = 0; j < good_matches.size(); j++) {
                if (outputMask.get(i, 0)[0] != 0.0) {
                    better_matches.add(good_matches.get(i));
                }
            }

            float better_matches_size_float = (float) better_matches.size();
            float matches_size_float = (float) matches.size();

            Log.i(TAG, "Numero di buoni match " + better_matches_size_float + "Numero reale " + better_matches.size());
            Log.i(TAG, "Numero di match " + matches_size_float + " Numero reale " + matches.size());
            Log.i(TAG, "Divisione " + better_matches_size_float/matches_size_float);

            // Il calcolo della distanza tra le due immagini viene fatto come #good_matches / #all_matches

            float distanza = better_matches_size_float/matches_size_float;
            distanza = 1 - distanza;

            Log.i(TAG,"Distanza " + distanza);
            if(distanza == 0) {

            }else{
                //Imposto la distanza calcolata con orb, e quella calcolata con l'istogramma la imposto a -1
                ImmagineDaMostrare immagineDaMostrare = new ImmagineDaMostrare(immaginiAnalizzate.get(i).getPath());
                immagineDaMostrare.setDistanzaOrb(distanza);
                immagineDaMostrare.setDistanzaIst(-1);

                listaPercorsoImmaginiDaMostrare.add(immagineDaMostrare);


            }


        }

        // A questo punto restituisco l'arraylist all'interno
        // del quale le immagini saranno ordinate sulla base della distanza
        for(int i=0; i<listaPercorsoImmaginiDaMostrare.size(); i++ ){
          Log.i(TAG, "" + ordinaArrayList(listaPercorsoImmaginiDaMostrare).get(i).getDistanzaOrb());
        }

        return ordinaArrayList(listaPercorsoImmaginiDaMostrare);

    }

    //Metodo che recupera un vettore di interi da un vettore di stringhe
    private int[] recuperaValoreFeaturesHist(String[] listaValori) {

        //Vettore di interi che conterrà tutte le features con cui effettuare il calcolo della distanza
        int[] features = new int[TOTAL_DIMENSION_WITH_HIST];

        //Ciclo il vettore di stringhe recuperato dallo sharedPreference per ricavare i valori delle features
        for (int i = 0; i < listaValori.length; i++) {
            int index = listaValori[i].indexOf("_");
            features[i] = Integer.parseInt(listaValori[i].substring(index + 1));
        }

        return features;
    }

    private byte[] recuperaValoreFeaturesORB(String[] listaValori) {

        //Vettore di interi che conterrà tutte le features con cui effettuare il calcolo della distanza
        byte[] features = new byte[TOTAL_DIMENSION_WITH_ORB];

        //Recupero il valore dei byte che avevo codificato
        for (int i = 0; i < listaValori.length; i++) {
            int index = listaValori[i].indexOf("_");
            features[i] = listaValori[i].substring(index + 1).getBytes(StandardCharsets.ISO_8859_1)[0];


        }

        return features;


    }

    //Metodo per convertire un set di stringhe in un array di stringhe
    private String[] setToArray(Set<String> featureSet) {

        String[] features = new String[TOTAL_DIMENSION_WITH_HIST];

        //Recupero un iterator per iterare nel set di stringhe
        Iterator iterator = featureSet.iterator();

        int sentinel = 0;

        //Popolo l'array di stringhe
        while (iterator.hasNext()) {
            //Per prima cosa rielaboro l'Array per eliminare le porzioni di stringa aggiunte
            String stringa = iterator.next().toString();

            features[sentinel] = stringa;
            sentinel++;
        }

        //Il salvataggio sul sharedPreference non mantiene l'ordine, quindi devo ordinare utilizzando l'indice introdotto precedentemente
        features = ordinaArray(features);

        return features;
    }

    //Metodo per la calcolare la distanza tra i due vettori
    private int distanza(int[] valoriFeaturesQuery, int[] valoriFeaturesSavedImage) {
        double distance = 0;


        //CHI-SQUARED

        for (int i = 0; i < valoriFeaturesQuery.length; i++) {
            int differenza = valoriFeaturesQuery[i] - valoriFeaturesSavedImage[i];
            int somma = valoriFeaturesQuery[i] + valoriFeaturesSavedImage[i];

            double numeratore = Math.pow(differenza, 2);

            //Calcolo la distanza tra i due vettori, lo small number serve a prevenire divisioni per 0
            distance = distance + (numeratore / (somma + SMALL_NUMBER));
        }


        return (int) distance / 2;


        //INTERSECTION
        /*
        for(int i = 0; i < valoriFeaturesQuery.length; i++){
            if(valoriFeaturesQuery[i] < valoriFeaturesSavedImage[i]){
                distance = distance + valoriFeaturesQuery[i];
            }else{
                distance = distance + valoriFeaturesSavedImage[i];
            }
        }
        return  (int) distance;
        */

        //EUCLIDEA
        /*
        for(int i = 0; i < valoriFeaturesQuery.length; i++){
            int differenza = valoriFeaturesQuery[i] - valoriFeaturesSavedImage[i];
            double quadrato = Math.pow(differenza, 2);

            distance = distance + quadrato;
        }


        return  (int) Math.sqrt(distance);
        */

/*
        double A = 0;
        double B = 0;
        double numeratore = 0;



        for(int i = 0; i < valoriFeaturesQuery.length; i++){
            A = A + Math.pow(valoriFeaturesQuery[i], 2);
            B = B + Math.pow(valoriFeaturesSavedImage[i], 2);

            numeratore = numeratore + (valoriFeaturesQuery[i] * valoriFeaturesSavedImage[i]);
        }

        double denominatore = Math.sqrt(A * B);

        distance = numeratore/denominatore;


        return (int) (distance * 100);
*/
    }

    private String[] ordinaArray(String[] array) {


        int v1, v2;
        String s1, s2;

        for (int i = 0; i < array.length - 1; i++) {
            for (int k = 0; k < array.length - 1 - i; k++) {
                s1 = array[k];
                s2 = array[k + 1];

                v1 = recuperaInt(s1);
                v2 = recuperaInt(s2);

                if (v1 > v2) {
                    array[k + 1] = s1;
                    array[k] = s2;
                }

            }
        }


        return array;
    }

    private int recuperaInt(String stringa) {
        int index = stringa.indexOf("_");
        int valore = Integer.parseInt(stringa.substring(0, index));
        return valore;
    }

    private ArrayList<ImmagineDaMostrare> ordinaArrayList(ArrayList<ImmagineDaMostrare> arrayList) {

        if (arrayList.get(0).getDistanzaIst() != -1){
            for (int i = 0; i < arrayList.size() - 1; i++) {
                for (int j = 0; j < arrayList.size() - i - 1; j++) {

                    int distanza1 = arrayList.get(j).getDistanzaIst();
                    int distanza2 = arrayList.get(j + 1).getDistanzaIst();

                    if (distanza1 > distanza2) {
                        ImmagineDaMostrare tmp1 = arrayList.get(j);
                        ImmagineDaMostrare tmp2 = arrayList.get(j + 1);

                        arrayList.remove(j);
                        arrayList.add(j, tmp2);
                        arrayList.remove(j + 1);
                        arrayList.add(j + 1, tmp1);
                    }
                }
            }

        return arrayList;
    }
        else{
            //Calcolo distanza per Orb
            for (int i = 0; i < arrayList.size() - 1; i++) {
                for (int j = 0; j < arrayList.size() - i - 1; j++) {

                    float distanza1 = arrayList.get(j).getDistanzaOrb();
                    float distanza2 = arrayList.get(j + 1).getDistanzaOrb();

                    if (distanza1 > distanza2) {
                        ImmagineDaMostrare tmp1 = arrayList.get(j);
                        ImmagineDaMostrare tmp2 = arrayList.get(j + 1);

                        arrayList.remove(j);
                        arrayList.add(j, tmp2);
                        arrayList.remove(j + 1);
                        arrayList.add(j + 1, tmp1);
                    }
                }
            }

            return arrayList;



        }

    }
}
package com.example.rosannacatte.cbirsoftwarever2;

import android.content.SharedPreferences;
import android.util.Log;

import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.imgcodecs.Imgcodecs;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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

    private boolean stopComparazione;


    //COSTRUTTORE
    public Comparatore(ArrayList<String> listaPercorsoImmagini, SharedPreferences preferences, ArrayList<ImmagineOrb> immaginiAnalizzate) {
        this.listaPercorsoImmagini = new ArrayList<>();
        this.listaPercorsoImmagini = listaPercorsoImmagini;
        this.preferences = preferences;
        this.immaginiAnalizzate = immaginiAnalizzate;
        this.stopComparazione = false;
    }


    //METODI

    //Metodo principale che calcola il vettore di indici di somiglianza tra le immagini
    public ArrayList<ImmagineDaMostrare> calcolaDistanzaIst(Mat queryImage) {

        //Recupero i valori delle features dell'immagine query
        int[] valoriFeaturesQuery = recuperaValoreFeaturesHist(imageDescriptor.calculateHist(queryImage));

        //Elenco percorso immagini da mostrare
        ArrayList<ImmagineDaMostrare> listaPercorsoImmaginiDaMostrare = new ArrayList<>();

        //Adesso devo fare il confronto tra le features dell'immagine di query e le features delle immagini contenute nel dispositivo
        for (int i = 0; i < getListaPercorsoImmagini().size() && !stopComparazione; i++) {

            String currentImage = getListaPercorsoImmagini().get(i);

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
                    immagineDaMostrare.setDistanzaBoth(-1);

                    listaPercorsoImmaginiDaMostrare.add(immagineDaMostrare);
                }


            }

        }

        //Ho popolato la mappa di valori. Devo restituire l'arrayList di URI delle immagini da mostrare


        if (!this.stopComparazione)
            return ordinaArrayList(listaPercorsoImmaginiDaMostrare);
        else
            return null;
    }

    //Calcolo distanza tra query image e immagini indicizzate con algoritmo orb
    public ArrayList<ImmagineDaMostrare> calcolaDistanzaOrb(Mat queryImage) {

        //Calcolo keypoints e features per l'immagine di query
        ImmagineOrb queryImageData = imageDescriptor.calculateOrb(queryImage);

        //Elenco percorso immagini da mostrare
        ArrayList<ImmagineDaMostrare> listaPercorsoImmaginiDaMostrare = new ArrayList<>();

        for (int i = 0; i < immaginiAnalizzate.size() && !stopComparazione; i++) {

            Mat descriptorsImage = immaginiAnalizzate.get(i).getDescriptors();
            MatOfKeyPoint keypointsImage = immaginiAnalizzate.get(i).getKeypoints();

            Mat descriptorsQuery = queryImageData.getDescriptors();
            MatOfKeyPoint keypointsQuery = queryImageData.getKeypoints();

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

            float good_matches_size_float = (float) good_matches.size();
            float matches_size_float = (float) matches.size();

            // Il calcolo della distanza tra le due immagini viene fatto come #good_matches / #all_matches

            float distanza = good_matches_size_float / matches_size_float;
            distanza = 1 - distanza;

            Log.i(TAG, "Distanza " + distanza);
            if (distanza == 0) {

            } else {
                //Imposto la distanza calcolata con orb, e quella calcolata con l'istogramma la imposto a -1
                ImmagineDaMostrare immagineDaMostrare = new ImmagineDaMostrare(immaginiAnalizzate.get(i).getPath());
                immagineDaMostrare.setDistanzaOrb(distanza);
                immagineDaMostrare.setDistanzaIst(-1);
                immagineDaMostrare.setDistanzaBoth(-1);

                listaPercorsoImmaginiDaMostrare.add(immagineDaMostrare);


            }


        }

        // A questo punto restituisco l'arraylist all'interno
        // del quale le immagini saranno ordinate sulla base della distanza

        if (!this.stopComparazione)
            return ordinaArrayList(listaPercorsoImmaginiDaMostrare);
        else
            return null;

    }

    public ArrayList<ImmagineDaMostrare> calcolaDistanzaBoth(Mat queryImageIst, Mat queryImageOrb, float pesoIst, float pesoOrb) {


        //Calcolo distanza Istogramma

        //Recupero i valori delle features dell'immagine query
        int[] valoriFeaturesQueryIst = recuperaValoreFeaturesHist(imageDescriptor.calculateHist(queryImageIst));

        //Elenco percorso immagini da mostrare
        ArrayList<ImmagineDaMostrare> listaPercorsoImmaginiDaMostrare = new ArrayList<>();

        //Adesso devo fare il confronto tra le features dell'immagine di query e le features delle immagini contenute nel dispositivo
        for (int i = 0; i < getListaPercorsoImmagini().size() && !stopComparazione; i++) {

            String currentImage = getListaPercorsoImmagini().get(i);

            if (Imgcodecs.imread(currentImage).channels() == 3) {

                //Recupero le features dallo shared preference sotto forma di set
                Set<String> setFeatures = preferences.getStringSet(currentImage, null);


                if (setFeatures != null) {
                    //Converto il set di stringhe in un array
                    String[] arrayFeatures = setToArray(setFeatures);

                    //Ottengo i valori delle features salvate nello shared preference
                    int[] valoriFeaturesSavedImage = recuperaValoreFeaturesHist(arrayFeatures);

                    //Calcolo la distanza tra i due vettori
                    int currentDistanza = distanza(valoriFeaturesQueryIst, valoriFeaturesSavedImage);

                    Log.i(TAG, "Distanza Ist " + currentDistanza);


                    if (currentDistanza == 0) {
                        //In questo caso l'immagine è identica, non la mostro di nuovo

                    } else {

                        // Imposto la distanza calcolata con l'istogramma, mentre quella relativa all'algoritmo orb a -1
                        ImmagineDaMostrare immagineDaMostrare = new ImmagineDaMostrare(currentImage);
                        immagineDaMostrare.setDistanzaIst(currentDistanza);

                        listaPercorsoImmaginiDaMostrare.add(immagineDaMostrare);
                    }
                }


            }

        }

        //ORB

        //Calcolo keypoints e features per l'immagine di query
        ImmagineOrb valoriFeaturesQueryOrb = imageDescriptor.calculateOrb(queryImageOrb);


        for (int i = 0; i < immaginiAnalizzate.size() && !stopComparazione; i++) {

            Mat descriptorsImage = immaginiAnalizzate.get(i).getDescriptors();
            MatOfKeyPoint keypointsImage = immaginiAnalizzate.get(i).getKeypoints();

            Mat descriptorsQuery = valoriFeaturesQueryOrb.getDescriptors();
            MatOfKeyPoint keypointsQuery = valoriFeaturesQueryOrb.getKeypoints();


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


            float good_matches_size_float = (float) good_matches.size();
            float matches_size_float = (float) matches.size();

            // Il calcolo della distanza tra le due immagini viene fatto come #good_matches / #all_matches

            float distanza = good_matches_size_float / matches_size_float;
            distanza = 1 - distanza;

            Log.i(TAG, "Distanza Orb " + distanza);
            if (distanza == 0) {

            } else {
                //Imposto la distanza calcolata con orb

                for (ImmagineDaMostrare immagine : listaPercorsoImmaginiDaMostrare) {
                    if (immagine.getPercorsoImmagine().equals(immaginiAnalizzate.get(i).getPath())) {
                        immagine.setDistanzaOrb(distanza);


                    }


                }


            }


        }

        if (!this.stopComparazione) {
            listaPercorsoImmaginiDaMostrare = normalizzaMinMax(listaPercorsoImmaginiDaMostrare);
            listaPercorsoImmaginiDaMostrare = mediaPesata(listaPercorsoImmaginiDaMostrare, pesoIst, pesoOrb);


            return ordinaArrayList(listaPercorsoImmaginiDaMostrare);
        } else
            return null;


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
        return Integer.parseInt(stringa.substring(0, index));
    }

    private ArrayList<ImmagineDaMostrare> ordinaArrayList(ArrayList<ImmagineDaMostrare> arrayList) {

        // Se entrambi i valori di distanza sono diversi da -1
        if (arrayList.get(0).getDistanzaBoth() != -1) {

            // Ordino l'arrayList basandomi sulla distanza id entrambi i descrittori
            for (int i = 0; i < arrayList.size() - 1; i++) {
                for (int j = 0; j < arrayList.size() - i - 1; j++) {

                    float distanza1 = arrayList.get(j).getDistanzaBoth();
                    float distanza2 = arrayList.get(j + 1).getDistanzaBoth();

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

        if (arrayList.get(0).getDistanzaIst() != -1) {
            // Ordino l'ArrayList basandomi sulla distanza dell'istogramma

            for (int i = 0; i < arrayList.size() - 1; i++) {
                for (int j = 0; j < arrayList.size() - i - 1; j++) {

                    int distanza1 = (int) arrayList.get(j).getDistanzaIst();
                    int distanza2 = (int) arrayList.get(j + 1).getDistanzaIst();

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
        } else {
            //Ordino le distanze dell'ArrayList basandomi sull'algoritmo Orb
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

    private ArrayList<ImmagineDaMostrare> mediaPesata(ArrayList<ImmagineDaMostrare> listaPercorsiImmaginiDaMostrare, float pesoIst, float pesoOrb) {
        for (ImmagineDaMostrare immagine : listaPercorsiImmaginiDaMostrare) {
            float mediaPesata = (immagine.getDistanzaIst() * pesoIst) + (immagine.getDistanzaOrb() * pesoOrb);
            immagine.setDistanzaBoth(mediaPesata);

        }

        return listaPercorsiImmaginiDaMostrare;
    }

    // Questo metodo calcola la normalizzazione min max delle distanza calcolate prima con l'istogramma, poi con Orb
    private ArrayList<ImmagineDaMostrare> normalizzaMinMax(ArrayList<ImmagineDaMostrare> listaPercorsiImmaginiDaMostrare) {
        float[] minMax = ottieniMinMax(listaPercorsiImmaginiDaMostrare);
        float normIst;
        float normOrb;

        for (ImmagineDaMostrare immagine : listaPercorsiImmaginiDaMostrare) {
            normIst = ((immagine.getDistanzaIst() - minMax[1]) / (minMax[0] - minMax[1]));
            Log.i(TAG, "Distanza istogramma normalizzato " + normIst);
            immagine.setDistanzaIst(normIst);

            normOrb = ((immagine.getDistanzaOrb() - minMax[3]) / (minMax[2] - minMax[3]));
            Log.i(TAG, "Distanza Orb normalizzato " + normOrb);
            immagine.setDistanzaOrb(normOrb);

        }

        return listaPercorsiImmaginiDaMostrare;


    }


    // Metodo che si occupa di ottenere i valori massimi e minimi delle distanze calcolate, sia con l'istogramma che con ORB
    private float[] ottieniMinMax(ArrayList<ImmagineDaMostrare> listaPercorsiImmaginiDaMostrare) {
        float maxIst = Float.NEGATIVE_INFINITY;
        float minIst = Float.POSITIVE_INFINITY;

        float maxOrb = Float.NEGATIVE_INFINITY;
        float minOrb = Float.POSITIVE_INFINITY;

        float[] result = new float[4];

        for (ImmagineDaMostrare immagine : listaPercorsiImmaginiDaMostrare) {
            if (immagine.getDistanzaIst() > maxIst)
                maxIst = immagine.getDistanzaIst();

            if (immagine.getDistanzaIst() < minIst)
                minIst = immagine.getDistanzaIst();

            if (immagine.getDistanzaOrb() > maxOrb)
                maxOrb = immagine.getDistanzaOrb();

            if (immagine.getDistanzaOrb() < minOrb)
                minOrb = immagine.getDistanzaOrb();

        }

        result[0] = maxIst;
        result[1] = minIst;
        result[2] = maxOrb;
        result[3] = minOrb;


        return result;


    }

    public ArrayList<String> getListaPercorsoImmagini() {
        return listaPercorsoImmagini;
    }

    public void bloccaComparazione() {
        stopComparazione = true;
    }

}
package com.example.rosannacatte.cbirsoftwarever2;

import android.content.SharedPreferences;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by root on 24/01/17.
 */

public class Comparatore {

    //CAMPI
    private static final String TAG = "Comparatore";
    private static final double SMALL_NUMBER = 0.000000000001;

    private static final int TOTAL_DIMENSION = 375;

    private static final int immaginiAnalizzate=1;



    //Elenco percorso immagini salvate sullo smartphone
    private ArrayList<String> listaPercorsoImmagini;



    //Shared preference su cui sono salvati i valori delle features
    private SharedPreferences preferences;

    //ImageDescriptor usato per ricavare gli istogrammi
    private ImageDescriptor imageDescriptor = new ImageDescriptor();





    //COSTRUTTORE
    public Comparatore(ArrayList<String> listaPercorsoImmagini, SharedPreferences preferences){
        this.listaPercorsoImmagini = new ArrayList<>();
        this.listaPercorsoImmagini=listaPercorsoImmagini;
        this.preferences = preferences;
    }


    //METODI

    //Metodo principale che calcola il vettore di indici di somiglianza tra le immagini
    public ArrayList<ImmagineDaMostrare> calcolaDistanza(Mat queryImage){

        //Recupero i valori delle features dell'immagine query
        int[] valoriFeaturesQuery = recuperaValoreFeatures(imageDescriptor.calculateHist(queryImage));

        //Elenco percorso immagini da mostrare
        ArrayList<ImmagineDaMostrare> listaPercorsoImmaginiDaMostrare = new ArrayList<>();

        //Adesso devo fare il confronto tra le features dell'immagine di query e le features delle immagini contenute nel dispositivo
        for(int i = 0; i < listaPercorsoImmagini.size(); i++){

            String currentImage = listaPercorsoImmagini.get(i);

            if(Imgcodecs.imread(currentImage).channels() == 3) {

                //Recupero le features dallo shared preference sotto forma di set
                Set<String> setFeatures = preferences.getStringSet(currentImage, null);


                //Converto il set di stringhe in un array
                String[] arrayFeatures = setToArray(setFeatures);

                //Ottengo i valori delle features salvate nello shared preference
                int[] valoriFeaturesSavedImage = recuperaValoreFeatures(arrayFeatures);

                //Calcolo la distanza tra i due vettori
                int currentDistanza = distanza(valoriFeaturesQuery, valoriFeaturesSavedImage);


                if (currentDistanza == 0) {
                    //In questo caso l'immagine è identica, non la mostro di nuovo

                } else{

                    ImmagineDaMostrare immagineDaMostrare = new ImmagineDaMostrare(currentImage, currentDistanza);
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

    //Metodo che recupera un vettore di interi da un vettore di stringhe
    private int[] recuperaValoreFeatures(String[] listaValori){

        //Vettore di interi che conterrà tutte le features con cui effettuare il calcolo della distanza
        int[] features = new int[TOTAL_DIMENSION];

        //Ciclo il vettore di stringhe recuperato dallo sharedPreference per ricavare i valori delle features
        for(int i = 0; i < listaValori.length; i++){
            int index = listaValori[i].indexOf("_");
            features[i] = Integer.parseInt(listaValori[i].substring(index + 1));
        }

        return features;
    }

    //Metodo per convertire un set di stringhe in un array di stringhe
    private String[] setToArray(Set<String> featureSet){

        String[] features = new String[TOTAL_DIMENSION];

        //Recupero un iterator per iterare nel set di stringhe
        Iterator iterator = featureSet.iterator();

        int sentinel = 0;

        //Popolo l'array di stringhe
        while (iterator.hasNext()){
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
    private int distanza(int[] valoriFeaturesQuery, int[] valoriFeaturesSavedImage){
        double distance = 0;


        //CHI-SQUARED

        for(int i = 0; i < valoriFeaturesQuery.length; i++){
            int differenza = valoriFeaturesQuery[i] - valoriFeaturesSavedImage[i];
            int somma = valoriFeaturesQuery[i] + valoriFeaturesSavedImage[i];

            double numeratore = Math.pow(differenza, 2);

            //Calcolo la distanza tra i due vettori, lo small number serve a prevenire divisioni per 0
            distance = distance + (numeratore/(somma + SMALL_NUMBER));
        }


        return (int) distance/2;


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

    private String[] ordinaArray(String[] array){



        int v1, v2;
        String s1, s2;

        for(int i = 0; i < array.length - 1; i++){
            for(int k = 0; k < array.length - 1 - i; k++){
                s1 = array[k];
                s2 = array[k + 1];

                v1 = recuperaInt(s1);
                v2 = recuperaInt(s2);

                if(v1 > v2){
                    array[k + 1] = s1;
                    array[k] = s2;
                }

            }
        }



        return array;
    }

    private int recuperaInt(String stringa){
        int index = stringa.indexOf("_");
        int valore = Integer.parseInt(stringa.substring(0, index));
        return valore;
    }

    private ArrayList<ImmagineDaMostrare> ordinaArrayList(ArrayList<ImmagineDaMostrare> arrayList){
        for(int i = 0; i < arrayList.size() - 1; i++){
            for(int j = 0; j < arrayList.size() - i - 1; j++){

                int distanza1 = arrayList.get(j).getDistanza();
                int distanza2 = arrayList.get(j + 1).getDistanza();

                if(distanza1 > distanza2){
                    ImmagineDaMostrare tmp1 = arrayList.get(j);
                    ImmagineDaMostrare tmp2 = arrayList.get(j + 1);

                    arrayList.remove(j);
                    arrayList.add(j, tmp2);
                    arrayList.remove(j+1);
                    arrayList.add(j + 1, tmp1);
                }
            }
        }

        return arrayList;
    }
}
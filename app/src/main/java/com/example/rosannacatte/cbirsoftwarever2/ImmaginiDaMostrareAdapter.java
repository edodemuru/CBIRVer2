package com.example.rosannacatte.cbirsoftwarever2;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by root on 27/01/17.
 */

public class ImmaginiDaMostrareAdapter extends ArrayAdapter<ImmagineDaMostrare> {

    public ImmaginiDaMostrareAdapter(Context context, ArrayList<ImmagineDaMostrare> immagineDaMostrare){
        super(context, 0, immagineDaMostrare);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //Recupero il primo elemento da mostrare
        ImmagineDaMostrare immagine = getItem(position);

        //Verifico se la view della lista è già stata usata, altrimenti faccio l'inflate
        if(convertView == null){
            //Per fare l'infalte passo il file xml in cui ho definito la grafica dell'elemento della lista.
            //La viewGroup dove la lista si trova
            //e un boolean per indicare se va attaccato al parent o no
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }

        //Inizializzo tutti gli elementi grafici presenti nell'elemento della lista
        TextView nomeImmagine_textView = (TextView) convertView.findViewById(R.id.showImage_textView);
        ImageView immagine_imageView = (ImageView) convertView.findViewById(R.id.showImage_imageView);

        //Popolo l'elemento
        nomeImmagine_textView.setText(immagine.getNomeImmagine());
        immagine_imageView.setImageBitmap(immagine.getImmagineBM());

        //Restituisco la view popolata
        return convertView;
    }
}
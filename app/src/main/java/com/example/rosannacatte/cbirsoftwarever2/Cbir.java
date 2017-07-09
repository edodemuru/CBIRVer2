package com.example.rosannacatte.cbirsoftwarever2;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArraySet;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.w3c.dom.Text;

import java.util.ArrayList;

public class Cbir extends AppCompatActivity {

    //Costante usata per evidenziare i messaggi nel log
    private static final String TAG = "CBIR";

    private static final int TOTAL_DIMENSION_WITH_HIST = 375;

    private static final int TOTAL_DIMENSION_WITH_ORB = 16000;

    private static final String FEATURES_FILE_NAME = "featuresFile";

    private static final int SELECT_PICTURE = 1;

    private static final int PERMISSION_REQUEST_CODE = 200;


    //Quale descrittore viene utilizzato?
    private enum TipoDiDescrittore {
        ISTOGRAMMA, ORB, BOTH

    }



    TipoDiDescrittore tipo = TipoDiDescrittore.ISTOGRAMMA;

    // Numero di immagini escluse dal calcolo delle features
    public int immagini_Escluse = 0;

    //Mi serve un vettore di stringhe per il salvataggio degli URI delle immagini, non so a priori quanto sarà grande dunque uso un arrayList
    private ArrayList<String> listaPercorsiImmagini;

    //Inizializzo la sharedPreference
    private SharedPreferences preference;

    //Mi serve un sharedPreference editor
    private SharedPreferences.Editor editor;

    //Oggetto ImageDescriptor
    private ImageDescriptor imageDescriptor;

    //Creo un oggeto Comparatore per calcolare la distanza tra istogrammi
    private Comparatore comparatore;

    // ArrayList contente il percorso, i keypoints e le features delle immagini analizzate con l'algoritmo Orb
    ArrayList<ImmagineOrb> immaginiAnalizzate = new ArrayList<>();

    //Verifica la connessione con la libreria OpenCv
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS: {
                    Log.i(TAG, "OpenCv manager connected");
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }


        }
    };


    //COMPONENTI GRAFICHE

    //Bottone per l'inserimento di un immagine
    private FloatingActionButton insertQueryImageFAB;

    private TextView textView;
    private ImageView sadSmileImage;

    //Seekbar per scelta peso dei descrittori
    private SeekBar weightDescriptorSeekbar;

    //TextView che danno una stima in percentuale del peso dei descrittori
    private TextView weightProgressIstogrammaText;
    private TextView weightProgressLBPText;

    private RadioButton descriptorBothButton;

    private int weightIstogramma;
    private int weightLBP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cbir);

        weightDescriptorSeekbar=(SeekBar) findViewById(R.id.pesoDescrittori);
        weightProgressIstogrammaText= (TextView) findViewById(R.id.progressIstogramma);
        weightProgressLBPText=(TextView) findViewById(R.id.progressLBP);

        descriptorBothButton=(RadioButton) findViewById(R.id.descrittoreEntrambi);

        weightDescriptorSeekbar.setEnabled(false);


        weightDescriptorSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                weightIstogramma=progress;
                weightLBP=100-progress;

                weightProgressIstogrammaText.setText(weightIstogramma + "%");
                weightProgressLBPText.setText(weightLBP + "%");



            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        //Recupero un sharedPreference per indicizzare la galleria
        //Dato che mi serve un solo file uso getPreferences
        preference = getSharedPreferences(FEATURES_FILE_NAME, MODE_PRIVATE);

        //Richiamo un editor
        editor = preference.edit();


        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCv connected");
        } else {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
            Log.i(TAG, "Connecting to the openCv library asynchronously");
        }

        if (checkPermission()) {

            Log.i(TAG, "Permessi già verificati");



            //Adesso devo ricevere in input un immagine dell'utente e confrontarla con le features che ho già ricavato
            insertQueryImageFAB = (FloatingActionButton) findViewById(R.id.insert_query_image_button);
            insertQueryImageFAB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    //Devo recuperare tutti gli Uri delle immagini presenti in galleria
                    listaPercorsiImmagini = new ArrayList<>();
                    listaPercorsiImmagini = recuperaPercorsoImmagini();

                    //VA FATTO IL CONTROLLO SU PRECEDENTI SHAREDPREFERENCE

                    //Indicizzo
                    indicizza(listaPercorsiImmagini);

                    //Creo un intent per aprire la galleria e selezionare un immagine da anlizzare
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                    //Avvio l'activity per ricevere risultati
                    startActivityForResult(intent, SELECT_PICTURE);


                }
            });

        } else {

            requestPermission();


        }


    }

    public void onRadioButtonClicked(View view) {
        // Il radio button è stato premuto?
        boolean checked = ((RadioButton) view).isChecked();


        // Controlla quale radio button è stato premuto
        switch (view.getId()) {
            case R.id.descrittoreIstogramma:
                if (checked) {
                    Toast.makeText(getApplicationContext(), "Hai scelto Istogramma", Toast.LENGTH_SHORT).show();
                    tipo = TipoDiDescrittore.ISTOGRAMMA;

                    //Reset della seekbar
                    weightDescriptorSeekbar.setEnabled(false);
                    weightDescriptorSeekbar.setProgress(50);

                }
                break;
            case R.id.descrittoreLbp:
                if (checked) {
                    Toast.makeText(getApplicationContext(), "Hai scelto il descrittore ORB", Toast.LENGTH_SHORT).show();
                    tipo = TipoDiDescrittore.ORB;

                    //Reset della seekbar
                    weightDescriptorSeekbar.setEnabled(false);
                    weightDescriptorSeekbar.setProgress(50);
                }
                break;
            case R.id.descrittoreEntrambi:
                if (checked) {
                    Toast.makeText(getApplicationContext(), "Hai scelto entrambi i descrittori", Toast.LENGTH_SHORT).show();
                    tipo = TipoDiDescrittore.BOTH;

                    //Abilito la seekbar
                    weightDescriptorSeekbar.setEnabled(true);
                }
                break;

        }
    }

    //Metodo per verifica dei permessi
    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        return result == PackageManager.PERMISSION_GRANTED;
    }

    //Metodo per richiesta permessi
    private void requestPermission() {

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.i(TAG, "Permesso accettato");

                    //Devo recuperare tutti gli Uri delle immagini presenti in galleria
                    //listaPercorsiImmagini = new ArrayList<>();
                    //listaPercorsiImmagini = recuperaPercorsoImmagini();


                    //VA FATTO IL CONTROLLO SU PRECEDENTI SHAREDPREFERENCE

                    //Indicizzo
                    //indicizza(listaPercorsiImmagini);


                } else {

                    Log.i(TAG, "Richiesta rifiutata");
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    @Override
    protected void onResume() {
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCv connected");
        } else {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
            Log.i(TAG, "Connecting to the openCv library asynchronously");
        }
        super.onResume();
    }


    //Questo metodo recupera tutte le immagini presenti nella memoria interna del telefono
    //Android gestisce le immagini mediante l'utilizzo di un contentProvider che comunica con il database dove sono salvati tutti i percorsi delle immagini
    //Nel caso delle foto parliamo di MediaStore.Image
    //Noi comunichiamo direttamente con un contentResolver che si occupa di connetterci al conentProvider

    private ArrayList<String> recuperaPercorsoImmagini() {
        //URI per la tabella dove sono situati tutti i percorsi delle immagini
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        //Projection utile per eseguire la query sulla tabella puntata dall'URI
        //Mi interessa solo la colonna che contiene l'effettiva location in meemoria delle foto
        String[] projection = new String[]{MediaStore.Images.Media.DATA};

        //Il cursore è cio che comunica e che riceve i risultati dal contentResolver
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null, null);

        //A questo punto ho ottenuto ciò che mi serviva, faccio un controllo
        if (!cursor.moveToFirst()) {
            Log.i(TAG, "Le immagini non sono state caricate");
            return null;
        }

        Log.i(TAG, "Le immagini sono state caricate");

        //Il cursore contiene le colonne della tabella iniziale, mi serve l'indice della colonna dei paths
        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);


        do {
            //Ora posso recuperare le immagini
            //Recupero solo quelle immagini all'interno della cartella "Foto"
            if(cursor.getString(columnIndex).contains("/storage/emulated/0/DCIM/Camera/")){
            listaPercorsiImmagini.add(cursor.getString(columnIndex));

            }


        } while(cursor.moveToNext());


        return listaPercorsiImmagini;

    }

    //Il metodo seguente compie tutte le operazioni per l'indicizzazione
    private void indicizza(ArrayList<String> percorsoImmagini) {

        String percorsoImmagine;
        String[] features;
        Mat immagineDaIndicizzare = new Mat();
        ArraySet<String> listaFeatures;

        //Svuoto shared preference
        editor.clear();

        //Verifico che sia già stata inizializzato un shared preference, nel caso vuol dire che ho già effettuato l'indicizzazione
        //if(verificaSharedPreference(preference)){
          //  return;
        //}



        for (int i = 0; i < percorsoImmagini.size(); i++) {

            percorsoImmagine = percorsoImmagini.get(i);

            // L'immagine che devo indicizzare deve avere almeno 3 o 4 canali e depth == CV_8U o depth == CV_32F
            if (Imgcodecs.imread(percorsoImmagine).channels() == 3) {



                //Arrayset da salvare nel shared preference
                listaFeatures = new ArraySet<>();


                //L'utente sceglie solo l'istogramma come descrittore
                if (tipo.equals(TipoDiDescrittore.ISTOGRAMMA)) {
                    Log.i(TAG,"Calcolo features con istogramma di colore");

                    immagineDaIndicizzare = caricaImmagineIst(percorsoImmagine);
                    features = new String[TOTAL_DIMENSION_WITH_HIST];

                    imageDescriptor = new ImageDescriptor();
                    features = imageDescriptor.calculateHist(immagineDaIndicizzare);

                    for (int j = 0; j < features.length; j++) {

                        listaFeatures.add(features[j]);
                    }

                    //Sto salvando il vettore di features nel shared preference
                    editor.putStringSet(percorsoImmagine, listaFeatures);
                }

                //L'utente sceglie solo il local binary pattern come descrittore
                else if (tipo.equals(TipoDiDescrittore.ORB)) {
                    Log.i(TAG,"Calcolo features con ORB");

                    immagineDaIndicizzare = caricaImmagineOrb(percorsoImmagine);

                    imageDescriptor = new ImageDescriptor();
                    ImmagineOrb immagineAnalizzata = imageDescriptor.calculateOrb(immagineDaIndicizzare);
                    immagineAnalizzata.setPath(percorsoImmagine);

                    immaginiAnalizzate.add(immagineAnalizzata);
                }

                //L'utente sceglie entrambi i descrittori
                else if (tipo.equals(TipoDiDescrittore.BOTH)) {

                }





            } else {
                //Immagini che non è possibile indicizzare
                immagini_Escluse++;

            }

        }


        //Completo il salvataggio e l'indicizzazione
        editor.commit();

    }

    //Il seguente metodo recupera un'immagine e la salva in un oggetto di tipo Mat
    private Mat caricaImmagineIst(String uri) {
        //Carico  l'immagine
        Mat immagineOriginale = Imgcodecs.imread(uri);
        Mat immagineHSV = new Mat();

        //Conversiona immagine originale
        Imgproc.cvtColor(immagineOriginale, immagineHSV, Imgproc.COLOR_BGR2HSV);
        return immagineHSV;


    }

    private Mat caricaImmagineOrb(String uri){
        Mat immagineOriginale = Imgcodecs.imread(uri);

        return immagineOriginale;



    }


    //Il seguente metodo verifica se nel sharedPreference son già presenti dei dati salvati
    private boolean verificaSharedPreference(SharedPreferences preference) {
        //Per il momento mi limito a verificare che ci siano dati salvati, non mi interessa se ci sono state modifiche alla galleria
        if (preference.contains("sentinel")) {
            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        //ArrayList delle immagini da mostrare
        ArrayList<ImmagineDaMostrare> immaginiDaMostrare = new ArrayList<>();

        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                //Recupero l'uri dell'immagine selezionata
                Uri selectedImageUri = data.getData();
                String imagePath;

                //Inizializzo la stringa per la proiezione
                String[] projection = new String[]{MediaStore.Images.Media.DATA};

                Cursor cursor = getContentResolver().query(selectedImageUri, projection, null, null, null, null);

                if (cursor != null) {
                    //Indice della colonna dove sono salvati i percorsi delle immagini
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

                    cursor.moveToFirst();

                    //Stringa contenente il percorso di salvataggio dell'immagine
                    imagePath = cursor.getString(columnIndex);

                    //Inizializzo il comparatore
                    comparatore = new Comparatore(listaPercorsiImmagini, preference,immaginiAnalizzate);

                    if (tipo.equals(TipoDiDescrittore.ISTOGRAMMA)){
                        //Ho recuperato l'immagine da analizzare e da confrontare
                       Mat queryImage = caricaImmagineIst(imagePath);

                        //Ora passo quell'immagine a un metodo che esegua il confronto
                        immaginiDaMostrare = comparatore.calcolaDistanzaIst(queryImage);

                    }
                    else if(tipo.equals(TipoDiDescrittore.ORB)){
                        //Ho recuperato l'immagine da analizzare e da confrontare
                        Mat queryImage = caricaImmagineOrb(imagePath);

                        immaginiDaMostrare = comparatore.calcolaDistanzaOrb(queryImage);



                    }
                    else if(tipo.equals(TipoDiDescrittore.BOTH)){}

                    visualizzaRisulatato(immaginiDaMostrare);


                }


            }
        }


    }

    public void visualizzaRisulatato(ArrayList<ImmagineDaMostrare> immaginiDaMostrare) {


        //Adapter per popolare la listView
        ImmaginiDaMostrareAdapter adapter;

        //ArrayList da passare all'adapter
        ArrayList<ImmagineDaMostrare> immaginiDaMostrare_arrayList = new ArrayList<>();


        textView = (TextView) findViewById(R.id.insert_query_image_text_view);
        sadSmileImage = (ImageView) findViewById(R.id.sad_smile_image_view);
        ListView mostraImmagini = (ListView) findViewById(R.id.imagesList);

        //Resetto le componenti grafiche alla posizione originale
        textView.setText("Seleziona un'immagine");
        mostraImmagini.setVisibility(View.GONE);
        sadSmileImage.setVisibility(View.GONE);
        textView.setVisibility(View.VISIBLE);


        if (immaginiDaMostrare.isEmpty()) {
            //La ricerca di immagini simili ha avuto esito negativo
            textView.setText("Non ho trovato nessuna immagine simile!");
            sadSmileImage.setVisibility(View.VISIBLE);
        } else {


            //Ho trovato delle immagini simili, le mostro all'utente
            textView.setVisibility(View.GONE);
            mostraImmagini.setVisibility(View.VISIBLE);


            //Creo l'arrayListi di immagini da mostrare con i 5 migliori risultati

            for (int i = 0; i < 5; i++) {
                //Sto popolando l'array da passare all'adpter con le immagini da mostrare come risultato
                immaginiDaMostrare_arrayList.add(immaginiDaMostrare.get(i));
            }

            /*
            for(int i = 1; i < 6; i++){
                //Sto popolando l'array da passare all'adpter con le immagini da mostrare come risultato
                immaginiDaMostrare_arrayList.add(immaginiDaMostrare.get(immaginiDaMostrare.size() - i - 1));
            }
            */


            //Popolo la listView con le immagini simili
            adapter = new ImmaginiDaMostrareAdapter(this, immaginiDaMostrare_arrayList);

            mostraImmagini.setAdapter(adapter);
        }

    }


}


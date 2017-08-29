package com.example.rosannacatte.cbirsoftwarever2;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArraySet;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
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
import org.opencv.features2d.Params;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.w3c.dom.Text;

import java.util.ArrayList;

import static android.content.ContentValues.TAG;
import static android.content.Context.MODE_PRIVATE;

public class Cbir extends AppCompatActivity {

    //Costante usata per evidenziare i messaggi nel log
    private static final String TAG = "CBIR";

    private static final int TOTAL_DIMENSION_WITH_HIST = 375;

    private static final int TOTAL_DIMENSION_WITH_ORB = 16000;

    private static final String FEATURES_FILE_NAME = "featuresFile";

    private static final int SELECT_PICTURE = 1;

    private static final int PERMISSION_REQUEST_CODE = 200;

    private static final String IMAGESPATH = "/storage/emulated/0/DCIM/Camera/";


    //Quale descrittore viene utilizzato?
    private enum TipoDiDescrittore {
        ISTOGRAMMA, ORB, BOTH

    }


    private static TipoDiDescrittore tipoDescrittore;

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

    //TextView relativa alla scelta del descrittore da usare
    private TextView scegliDescr;

    //Bottone per l'inserimento di un immagine
    private FloatingActionButton insertQueryImageFAB;

    private TextView textView;
    private ImageView sadSmileImage;

    //Seekbar per scelta peso dei descrittori
    private SeekBar weightDescriptorSeekbar;

    //TextView che danno una stima in percentuale del peso dei descrittori
    private TextView weightProgressIstogrammaText;
    private TextView weightProgressORBText;

    // TextView istruzioni per il click del button
    private TextView instructions;

    // Progress Bar per indicizzazione immagini
    private ProgressBar progressIndicizzazione;

    private TextView attendere;

    // Peso delle features nella computazione
    private int weightIstogramma;
    private int weightOrb;

    //Flag per bloccare l'indicizzazione e la comparazione nel caso in cui l'applicazione venga riavviata
    private boolean stopIndicizzazione;
    public boolean stopComparazione;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cbir);

        // Inizializzazione elementi grafici
        scegliDescr = (TextView) findViewById(R.id.scegliDescr);

        weightDescriptorSeekbar = (SeekBar) findViewById(R.id.pesoDescrittori);
        weightProgressIstogrammaText = (TextView) findViewById(R.id.progressIstogramma);
        weightProgressORBText = (TextView) findViewById(R.id.progressORB);
        progressIndicizzazione = (ProgressBar) findViewById(R.id.progressIndicizzazione);

        //Inizializzo ed elimino dall'interfaccia il messaggio di attesa
        attendere = (TextView) findViewById(R.id.attendere);
        attendere.setVisibility(View.GONE);

        instructions = (TextView) findViewById(R.id.insert_query_image_text_view);

        //Abilito la seekbar
        weightDescriptorSeekbar.setEnabled(true);

        //Disabilito la progressBar
        progressIndicizzazione.setVisibility(View.GONE);
        progressIndicizzazione.setProgress(0);

        //Carico i valori di default, prelevandoli dall'interfaccia
        weightOrb = Integer.parseInt(weightProgressORBText.getText().toString().split("%")[0]);
        weightIstogramma = Integer.parseInt(weightProgressIstogrammaText.getText().toString().split("%")[0]);

        if (weightIstogramma == 0)
            tipoDescrittore = TipoDiDescrittore.ORB;
        else if (weightOrb == 0)
            tipoDescrittore = TipoDiDescrittore.ISTOGRAMMA;
        else
            tipoDescrittore = TipoDiDescrittore.BOTH;

        // Listener attivo per variazione della seekbar
        weightDescriptorSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                weightIstogramma = 100 - progress;
                weightOrb = progress;

                weightProgressIstogrammaText.setText(weightIstogramma + "%");
                weightProgressORBText.setText(weightOrb + "%");


            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                if (weightIstogramma == 0)
                    //Se il peso dell'istogramma è pari a 0, il tipo di descrittore risulterà orb
                    tipoDescrittore = TipoDiDescrittore.ORB;
                else if (weightOrb == 0)
                    tipoDescrittore = TipoDiDescrittore.ISTOGRAMMA;
                else
                    tipoDescrittore = TipoDiDescrittore.BOTH;

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

        // Verifica connessione
        if (checkPermission()) {

            Log.i(TAG, "Permessi già verificati");


            //Una volta premuto il floating action button, devo avviare l'indicizzazione
            insertQueryImageFAB = (FloatingActionButton) findViewById(R.id.insert_query_image_button);
            insertQueryImageFAB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //cambio icona al floating action button
                    insertQueryImageFAB.setImageResource(R.drawable.delete2);

                    //Devo recuperare tutti gli Uri delle immagini presenti in galleria
                    listaPercorsiImmagini = new ArrayList<>();
                    listaPercorsiImmagini = recuperaPercorsoImmagini();

                    // Attivazione elementi grafici per lo stato dell'indicizzazione
                    progressIndicizzazione.setMax(listaPercorsiImmagini.size() - 1);
                    progressIndicizzazione.setVisibility(View.VISIBLE);
                    attendere.setVisibility(View.VISIBLE);

                    weightDescriptorSeekbar.setEnabled(false);

                    instructions.setVisibility(View.GONE);

                    stopIndicizzazione = false;

                    // Utilizzo un thread per limitare il carico di lavoro sul thread principale
                    Thread thread = new Thread() {
                        @Override
                        public void run() {
                            indicizza(listaPercorsiImmagini);


                            if (!stopIndicizzazione) {

                                Log.i(TAG, "Tipo di descrittore " + tipoDescrittore + " dimensione immagini analizzate " + immaginiAnalizzate.size());
                                //Creo un intent per aprire la galleria e selezionare un'immagine da anlizzare
                                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                                //Avvio l'activity per ricevere risultati
                                startActivityForResult(intent, SELECT_PICTURE);
                            } else {
                                // Se l'applicazione viene riavviata devo eliminare gli elementi memorizzati nell'arrayList
                                immaginiAnalizzate = new ArrayList<ImmagineOrb>();
                                //stopIndicizzazione = false;


                            }


                        }

                    };


                    thread.start();

                    insertQueryImageFAB.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Codice per riavvio applicazione
                            Intent i = getBaseContext().getPackageManager()
                                    .getLaunchIntentForPackage(getBaseContext().getPackageName());
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            finish();
                            startActivity(i);

                        }

                    });


                }
            });

        } else {

            // Se i permessi non sono verificati vengono richiesti
            requestPermission();


        }


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Richiamato onDestroy");

        // Flag per blocco indicizzazione
        stopIndicizzazione = true;
        stopComparazione = true;

        // Se il sistema è in fase di comparazione dei risultati, blocco la comparazione
        if (comparatore != null)
            comparatore.bloccaComparazione();

        // Eliminazione dati presenti nello shared Preference
        editor.clear();
        editor.commit();

        // Attivazione garbage collector
        System.gc();

        // Per liberare parte della memoria del sistema, imposto a null alcuni degli oggetti utilizzati durante l'esecuzione del codice
        scegliDescr = null;
        insertQueryImageFAB = null;
        textView= null ;
        sadSmileImage = null;
        weightDescriptorSeekbar = null;
        weightProgressIstogrammaText = null;
        weightProgressORBText = null;
        instructions = null;
        progressIndicizzazione = null;
        attendere = null;
        listaPercorsiImmagini = null;
        preference = null;
        //editor = null;
        imageDescriptor = null;
        comparatore = null;
        //immaginiAnalizzate = null;



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

                } else {

                    Log.i(TAG, "Richiesta rifiutata");
                }
                return;
            }


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
            if (cursor.getString(columnIndex).contains(IMAGESPATH)) {
                listaPercorsiImmagini.add(cursor.getString(columnIndex));

            }


        } while (cursor.moveToNext());


        return listaPercorsiImmagini;

    }

    //Il metodo seguente compie tutte le operazioni per l'indicizzazione
    private void indicizza(ArrayList<String> percorsoImmagini) {

        // Percorso dell'immagine considerata
        String percorsoImmagine;
        // Vettore di features estratte con l'istogramma di colore, nel caso in cui tipoDescrittore = HIST
        String[] features_Ist_Only;
        //Vettore di features estratte con l'istogramma di colore, nel caso in cui tipoDescrittore = BOTH
        String[] features_Ist_Both;
        // Immagine da indicizzare con tipoDescrittore = BOTH
        Mat immagineDaIndicizzare;
        // Immagine da indicizzare con tipoDescrittore = HIST
        Mat immagineDaIndicizzare_Ist;
        // Immagine da indicizzare con tipoDescrittore = ORB
        Mat immagineDaIndicizzare_Orb;
        // Lista di features da salvare nello shared preference
        ArraySet<String> listaFeatures;


        //Svuoto shared preference
        editor.clear();


        for (int i = 0; i < percorsoImmagini.size() && !stopIndicizzazione; i++) {

            Log.i(TAG,"Avvio indicizzazione");
            // Incremento la progress bar di una unità
            progressIndicizzazione.incrementProgressBy(1);

            percorsoImmagine = percorsoImmagini.get(i);

            // L'immagine che devo indicizzare deve avere almeno 3 o 4 canali e depth == CV_8U o depth == CV_32F
            if (Imgcodecs.imread(percorsoImmagine).channels() == 3) {


                //Arrayset da salvare nel shared preference
                listaFeatures = new ArraySet<>();

                //L'utente sceglie solo l'istogramma come descrittore
                if (tipoDescrittore.equals(TipoDiDescrittore.ISTOGRAMMA)) {
                    Log.i(TAG, "Calcolo features con istogramma di colore");

                    immagineDaIndicizzare = caricaImmagineIst(percorsoImmagine);
                    features_Ist_Only = new String[TOTAL_DIMENSION_WITH_HIST];

                    imageDescriptor = new ImageDescriptor();
                    features_Ist_Only = imageDescriptor.calculateHist(immagineDaIndicizzare);

                    for (int j = 0; j < features_Ist_Only.length; j++) {

                        listaFeatures.add(features_Ist_Only[j]);
                    }

                    //Sto salvando il vettore di features nel shared preference
                    editor.putStringSet(percorsoImmagine, listaFeatures);
                }

                //L'utente sceglie solo il local binary pattern come descrittore
                else if (tipoDescrittore.equals(TipoDiDescrittore.ORB)) {
                    Log.i(TAG, "Calcolo features con ORB");

                    immagineDaIndicizzare = caricaImmagineOrb(percorsoImmagine);

                    imageDescriptor = new ImageDescriptor();
                    ImmagineOrb immagineAnalizzata = imageDescriptor.calculateOrb(immagineDaIndicizzare);
                    immagineAnalizzata.setPath(percorsoImmagine);

                    immaginiAnalizzate.add(immagineAnalizzata);
                }

                //L'utente sceglie entrambi i descrittori
                else if (tipoDescrittore.equals(TipoDiDescrittore.BOTH)) {
                    Log.i(TAG, "Calcolo features con istogramma di colore e con Orb");
                    Log.i(TAG, "Peso ORB " + weightOrb + " Peso Istogramma " + weightIstogramma);

                    //Calcolo Istogramma di colore
                    immagineDaIndicizzare_Ist = caricaImmagineIst(percorsoImmagine);

                    imageDescriptor = new ImageDescriptor();

                    features_Ist_Both = imageDescriptor.calculateHist(immagineDaIndicizzare_Ist);

                    for (int j = 0; j < features_Ist_Both.length; j++) {

                        listaFeatures.add(features_Ist_Both[j]);
                    }


                    //Sto salvando il vettore di features nel shared preference
                    editor.putStringSet(percorsoImmagine, listaFeatures);

                    //Calcolo features con Orb
                    immagineDaIndicizzare_Orb = caricaImmagineOrb(percorsoImmagine);
                    ImmagineOrb immagineAnalizzata = imageDescriptor.calculateOrb(immagineDaIndicizzare_Orb);
                    immagineAnalizzata.setPath(percorsoImmagine);
                    immaginiAnalizzate.add(immagineAnalizzata);


                }


            } else {
                // Immagini che non è possibile indicizzare

            }

        }


        if (!stopIndicizzazione) {
            //Completo il salvataggio e l'indicizzazione
            editor.commit();
        }

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

    private Mat caricaImmagineOrb(String uri) {
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
        //ArrayList<ImmagineDaMostrare> immaginiDaMostrare = new ArrayList<>();

        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {

                stopComparazione = false;

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
                    comparatore = new Comparatore(listaPercorsiImmagini, preference, immaginiAnalizzate);

                    // Questo oggetto contiene gli elementi di input necessari all'handler
                    InputBackgroundTask input = new InputBackgroundTask(imagePath, comparatore);

                    Log.i(TAG, "Tipo di descrittore " + tipoDescrittore.toString());

                    Log.i(TAG, "Numero immagini analizzate " + immaginiAnalizzate.size());

                    //Eseguo l'handler
                    new BackgroundTask().execute(input);


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

            scegliDescr.setVisibility(View.GONE);
            progressIndicizzazione.setVisibility(View.GONE);
            attendere.setVisibility(View.GONE);

            insertQueryImageFAB.setVisibility(View.VISIBLE);
            insertQueryImageFAB.setImageResource(R.drawable.goback2);


            //Creo l'arrayListi di immagini da mostrare con i 5 migliori risultati

            for (int i = 0; i < 5; i++) {
                //Sto popolando l'array da passare all'adpter con le immagini da mostrare come risultato
                immaginiDaMostrare_arrayList.add(immaginiDaMostrare.get(i));
            }


            //Popolo la listView con le immagini simili
            adapter = new ImmaginiDaMostrareAdapter(this, immaginiDaMostrare_arrayList);

            mostraImmagini.setAdapter(adapter);

            insertQueryImageFAB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Codice per riavvio applicazione
                    Intent i = getBaseContext().getPackageManager()
                            .getLaunchIntentForPackage(getBaseContext().getPackageName());
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    //i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    finish();
                    startActivity(i);


                }

            });
        }

    }


    private class BackgroundTask extends AsyncTask<InputBackgroundTask, Integer, ArrayList<ImmagineDaMostrare>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            instructions.setVisibility(View.GONE);
            weightDescriptorSeekbar.setEnabled(false);

            attendere.setText("Comparazione immagini in corso, attendere...");
            attendere.setPadding(250, 0, 0, 0);
            progressIndicizzazione.setIndeterminate(true);

            stopComparazione = false;
        }

        @Override
        protected ArrayList<ImmagineDaMostrare> doInBackground(InputBackgroundTask... values) {
            ArrayList<ImmagineDaMostrare> immaginiDaMostrare = new ArrayList<>();

            Comparatore comparatore = values[0].getComparatore();
            String imagePath = values[0].getImagePath();

            if (tipoDescrittore.equals(TipoDiDescrittore.ISTOGRAMMA)) {
                //Ho recuperato l'immagine da analizzare e da confrontare
                Mat queryImage = caricaImmagineIst(imagePath);
                //Ora passo quell'immagine a un metodo che esegua il confronto
                immaginiDaMostrare = comparatore.calcolaDistanzaIst(queryImage);

            } else if (tipoDescrittore.equals(TipoDiDescrittore.ORB)) {
                //Ho recuperato l'immagine da analizzare e da confrontare
                Mat queryImage = caricaImmagineOrb(imagePath);

                immaginiDaMostrare = comparatore.calcolaDistanzaOrb(queryImage);


            } else if (tipoDescrittore.equals(TipoDiDescrittore.BOTH)) {
                // Ho recuperato l'immagine da analizzare e confrontare
                Mat queryImage_Ist = caricaImmagineIst(imagePath);
                Mat queryImage_Orb = caricaImmagineOrb(imagePath);

                float pesoIst = (float) weightIstogramma / 100;
                float pesoOrb = (float) weightOrb / 100;

                immaginiDaMostrare = comparatore.calcolaDistanzaBoth(queryImage_Ist, queryImage_Orb, pesoIst, pesoOrb);


            }

            return immaginiDaMostrare;

        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate();
        }

        @Override
        protected void onPostExecute(ArrayList<ImmagineDaMostrare> immaginiDaMostrare) {
            super.onPostExecute(immaginiDaMostrare);

            if (!stopComparazione) {
                visualizzaRisulatato(immaginiDaMostrare);
                immaginiDaMostrare = new ArrayList<>();

            } else
                Log.i(TAG, "Blocco visualizzazione risultati");
        }


    }


}








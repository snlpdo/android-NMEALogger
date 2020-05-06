package fr.rg.android.nmealogger;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity implements OnNmeaMessageListener, LocationListener {

    public static final String TAG = "NMEALogger";

    static final String MULTICAST_ADDRESS = "224.1.2.3";
    static final int MULTICAST_PORT = 1234;

    TextView infoTV;
    Button startBT;
    EditText nmeaET;

    private static final int MAX_HIST = 40;
    String[] historique = new String[MAX_HIST];
    int i_hist = 0;

    boolean serveurStarted = false;
    int idxTrames = 0;
    boolean stopRequest = false;
    String trameToSend = "";
    String SERVER_OFF = "Cliquer pour multidiffuser les trames";
    String SERVER_ON = "Multidiffusion " + MULTICAST_ADDRESS + ":" + MULTICAST_PORT;

    Thread multicastThread = null;
    AtomicBoolean running = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        infoTV = (TextView) findViewById(R.id.infoTV);
        nmeaET = (EditText) findViewById(R.id.nmeaET);
        startBT = (Button) findViewById(R.id.startBT);

        // Activation de la surveillance NMEA
        LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            infoTV.setText("Accès GPS non autorisé");
            return;
        } else {
            locManager.addNmeaListener(this);
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10, 20, this);
            infoTV.setText(SERVER_OFF);
        }

        // Thread pour l'émission des trames NMEA en multidiffusion
        if (multicastThread == null) {
            multicastThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    running.set(true);

                    // Configuration de la socket et préparation du paquet à émettre
                    MulticastSocket s = null;
                    DatagramPacket pkt = null;
                    byte[] buf = null;
                    InetAddress group = null;


                    try {
                        s = new MulticastSocket(MULTICAST_PORT);
                        s.setLoopbackMode(false);
                        //s.setInterface(InetAddress.getByName("192.168.43.1"));
                        group = InetAddress.getByName(MULTICAST_ADDRESS);
                        s.joinGroup(group);
                        Log.d(TAG, "Interface utilisée: "+s.getInterface());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Émission périodique
                    while (running.get()) {
                        try {
                            if (serveurStarted && trameToSend != "") {
//                                synchronized (trameToSend) {
                                    buf = trameToSend.getBytes();
                                    trameToSend = "";
//                                }

                                pkt = new DatagramPacket(buf, buf.length, group, MULTICAST_PORT);
                                s.send(pkt);
                            }
                            if (stopRequest) {
                                buf = "END\n".getBytes();
                                pkt = new DatagramPacket(buf, buf.length, group, MULTICAST_PORT);
                                s.send(pkt);
                                stopRequest = false;
                            }
                        } catch (IOException e) {
                            infoTV.setText("Service terminé");
                        }
                    }
                }
            });
            multicastThread.start();
        }
    }

    @Override
    public void onNmeaMessage(String message, long timestamp) {
        idxTrames++;

        historique[i_hist] = message;
        i_hist++;
        if (i_hist == MAX_HIST) {
            i_hist = 0;
        }

        String content = "";
        int idx;
        for (int i = 0; i < MAX_HIST; i++) {
            idx = (i_hist + 1 + i) % MAX_HIST;
            if (historique[idx] == null)
                content += '\n';
            else
                content += historique[idx] + '\n';
        }
        nmeaET.setText(content);

        trameToSend += message + "\n";
    }

    @Override
    public void onLocationChanged(Location location) {
        infoTV.setText(location.getLatitude() + ", " + location.getLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public void handleStart(View v) {
        if (serveurStarted) {
            stopRequest = true;
            startBT.setText("Démarrer la multidiffusion");
            infoTV.setText(SERVER_OFF);
        } else {
            startBT.setText("Arrêter la multidiffusion");
            infoTV.setText(SERVER_ON);
        }
        serveurStarted = !serveurStarted;
    }

    @Override
    public void onDestroy() {
        if (multicastThread != null) {
            multicastThread.interrupt();
            multicastThread = null;
        }
        super.onDestroy();
    }
}

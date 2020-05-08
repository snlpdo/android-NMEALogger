package fr.rg.android.nmealogger

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import fr.rg.android.nmealogger.databinding.ActivityMainBinding
import java.io.IOException
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.util.concurrent.atomic.AtomicBoolean


class MainActivity : AppCompatActivity(), LocationListener {

    var serveurStarted = false
    var stopRequest = false
    var trameToSend = ""
    var SERVER_OFF = "Cliquer pour multidiffuser les trames"
    var SERVER_ON = "Multidiffusion $MULTICAST_ADDRESS:$MULTICAST_PORT"
    var multicastThread: Thread? = null
    var running = AtomicBoolean(false)

    var locManager: LocationManager? = null

    private lateinit var binding: ActivityMainBinding
    private val viewModel : NmeaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = NmeaFrameListAdapter()
        //val adapter = NmeaFrameAdapter(viewModel.histNmea)
        binding.nmeaList.adapter = adapter

        viewModel.histNmea.observe(this, Observer {
                // L'adaptateur notifie uniquement si la liste n'est plus la même
                adapter.submitList(it.toMutableList())
            })

        // Activation de la surveillance NMEA
        locManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locManager = null
            binding.infoText.text = getString(R.string.no_gps_permission)
            return
        } else {
            binding.infoText.text = SERVER_OFF
        }

        // Thread pour l'émission des trames NMEA en multidiffusion
        if (multicastThread == null) {
            multicastThread = Thread(Runnable {
                running.set(true)

                // Configuration de la socket et préparation du paquet à émettre
                var s: MulticastSocket? = null
                var pkt: DatagramPacket? = null
                var buf: ByteArray? = null
                var group: InetAddress? = null
                try {
                    s = MulticastSocket(MULTICAST_PORT)
                    s.loopbackMode = false
                    //s.setInterface(InetAddress.getByName("192.168.43.1"));
                    group = InetAddress.getByName(MULTICAST_ADDRESS)
                    s.joinGroup(group)
                    Log.d(TAG, "Interface utilisée: " + s.getInterface())
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                // Émission périodique
                while (running.get()) {
                    try {
                        if (serveurStarted && trameToSend !== "") {
//                                synchronized (trameToSend) {
                            buf = trameToSend.toByteArray()
                            trameToSend = ""
                            //                                }
                            pkt = DatagramPacket(buf, buf.size, group, MULTICAST_PORT)
                            s!!.send(pkt)
                        }
                        if (stopRequest) {
                            buf = "END\n".toByteArray()
                            pkt = DatagramPacket(buf, buf.size, group, MULTICAST_PORT)
                            s!!.send(pkt)
                            stopRequest = false
                        }
                    } catch (e: IOException) {
                        binding.infoText.text = "Service terminé"
                    }
                }
            })
            multicastThread!!.start()
        }
    }

    override fun onResume() {
        super.onResume()

        try {
            locManager?.addNmeaListener(viewModel)
            locManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10, 20f, this)
        } catch (unlikely: SecurityException) {
            Log.d(TAG, "Permissions insuffisantes pour LocationManager")
        }
    }

    override fun onPause() {
        super.onPause()

        try {
            locManager?.removeNmeaListener(viewModel)
            locManager?.removeUpdates(this)
        } catch (unlikely: SecurityException) {
            Log.d(TAG, "Permissions insuffisantes pour LocationManager")
        }
    }

    override fun onLocationChanged(location: Location) {
        binding.infoText.text = location.latitude.toString() + ", " + location.longitude
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    fun handleStart(v: View?) {
        if (serveurStarted) {
            stopRequest = true
            binding.startButton.text = getString(R.string.start_multicast)
            binding.infoText.text = SERVER_OFF
        } else {
            binding.startButton.text = getString(R.string.stop_multicast)
            binding.infoText.text = SERVER_ON
        }
        serveurStarted = !serveurStarted
    }

    public override fun onDestroy() {
        if (multicastThread != null) {
            multicastThread!!.interrupt()
            multicastThread = null
        }
        super.onDestroy()
    }

    companion object {
        const val TAG = "NMEALogger"
        const val MULTICAST_ADDRESS = "224.1.2.3"
        const val MULTICAST_PORT = 1234
    }
}


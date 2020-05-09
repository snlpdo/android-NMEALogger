package fr.rg.android.nmealogger

import android.location.OnNmeaMessageListener
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NmeaViewModel : ViewModel(), OnNmeaMessageListener {
//    private val nmeaHashMap = HashMap<String, NmeaFrame>()

    private val _histNmea: MutableLiveData<MutableList<NmeaFrame>> = MutableLiveData()
    init {
        _histNmea.value = mutableListOf()
    }
    val histNmea: LiveData<MutableList<NmeaFrame>>
        get() = _histNmea

    override fun onNmeaMessage(message: String, timestamp: Long) {
        // Compléter la liste
//        val key = message.split(',')[0].substring(1)
//        nmeaHashMap[key] = message

        _histNmea.value!!.add(NmeaFrame(timestamp, message))
        // les observateurs ne sont avertis que lorsque le mutateur est utilisé
        _histNmea.value = _histNmea.value
    }

}

data class NmeaFrame(var timeStamp: Long, var content: String)
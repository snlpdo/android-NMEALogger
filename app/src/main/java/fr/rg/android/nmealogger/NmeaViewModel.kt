package fr.rg.android.nmealogger

import android.location.OnNmeaMessageListener
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NmeaViewModel : ViewModel(), OnNmeaMessageListener {
    private val _histNmea: MutableLiveData<MutableList<String>> = MutableLiveData()
    init {
        _histNmea.value = mutableListOf()
    }
    val histNmea: LiveData<MutableList<String>>
        get() = _histNmea

    override fun onNmeaMessage(message: String, timestamp: Long) {
        _histNmea.value!!.add(message)
        // les observateurs ne sont avertis que lorsque le mutateur est utilisé
        _histNmea.value = _histNmea.value
    }

}
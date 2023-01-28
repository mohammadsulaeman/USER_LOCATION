package com.example.userlocation

import android.R.attr
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.mapboxsdk.geometry.LatLng
import java.util.*


class MainActivity : AppCompatActivity() {
    lateinit var textViewLocationName : TextView
    lateinit var textViewLocationLat : TextView
    lateinit var textViewLocationLong : TextView
    lateinit var buttonSearchLocation : Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textViewLocationLat = findViewById(R.id.textView_latitudeLocation)
        textViewLocationLong = findViewById(R.id.textView_longitudeLocation)
        textViewLocationName = findViewById(R.id.textView_namaLocation)
        buttonSearchLocation = findViewById(R.id.button_searchLocation)

        buttonSearchLocation.setOnClickListener{
            val intent : Intent = Intent(this@MainActivity, PicklocationActivity::class.java)
            intent.putExtra(PicklocationActivity.FORM_VIEW_INDICATOR, DESTINATIONAL_ID)
            startActivityForResult(intent,PicklocationActivity.LOCATION_PICKER_ID)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK)
        {
            if (requestCode == PicklocationActivity.LOCATION_PICKER_ID)
            {
                val addressSet = data!!.getStringExtra(PicklocationActivity.LOCATION_NAME)
                Log.e("TAG", "onActivityResult addressSet: ${addressSet}" )
                val latLng: LatLng? =
                    data.getParcelableExtra(PicklocationActivity.LOCATION_LATLNG)
                Log.e("TAG", "onActivityResult latLng:  ${latLng}")
                textViewLocationName.text = addressSet
                val latitude = java.lang.String.valueOf(Objects.requireNonNull((latLng)!!.latitude))
                Log.e("TAG", "onActivityResult latitude:  ${latitude}")
                val longitude = java.lang.String.valueOf(latLng.longitude)
                Log.e("TAG", "onActivityResult longitude:  ${longitude}"  )
                textViewLocationLat.text = latitude
                textViewLocationLong.text = longitude
            }
        }
    }

    companion object {
        const val DESTINATIONAL_ID = 1;
    }
}
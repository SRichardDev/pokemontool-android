package io.stanc.pogoradar.screen

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.fragment.app.Fragment

abstract class ParcelableArgumentFragment<DataObject: Parcelable>: Fragment() {
    private val TAG = javaClass.name

    protected var dataObject: DataObject? = null
        protected set(value) {
            field = value
            onDataObjectChanged(value)
        }
    protected abstract fun onDataObjectChanged(dataObject: DataObject?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getParcelable<DataObject>(PARCELABLE_EXTRA_DATA_OBJECT)?.let { dataObject ->
            Log.w(TAG, "Debug:: onCreate(), got dataObject: $dataObject")
            this.dataObject = dataObject
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.w(TAG, "Debug:: onSaveInstanceState(), save data: $dataObject")
        outState.putParcelable(PARCELABLE_EXTRA_DATA_OBJECT, dataObject)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        savedInstanceState?.getParcelable<DataObject>(PARCELABLE_EXTRA_DATA_OBJECT)?.let { dataObject ->
            Log.w(TAG, "Debug:: onViewStateRestored(), got dataObject: $dataObject")
            this.dataObject = dataObject
        }
        super.onViewStateRestored(savedInstanceState)
    }

    companion object {
        const val PARCELABLE_EXTRA_DATA_OBJECT = "parcelableExtraDataObject"
    }
}
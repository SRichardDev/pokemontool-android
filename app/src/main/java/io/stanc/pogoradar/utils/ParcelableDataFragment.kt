package io.stanc.pogoradar.utils

import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment

abstract class ParcelableDataFragment<DataObject: Parcelable>: Fragment() {
    private val TAG = javaClass.name

    protected var dataObject: DataObject? = null
        protected set(value) {
            field = value
            onDataChanged(value)
        }
    protected abstract fun onDataChanged(dataObject: DataObject?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getParcelable<DataObject>(PARCELABLE_EXTRA_DATA_OBJECT)?.let { dataObject ->
            this.dataObject = dataObject
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(PARCELABLE_EXTRA_DATA_OBJECT, dataObject)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        savedInstanceState?.getParcelable<DataObject>(PARCELABLE_EXTRA_DATA_OBJECT)?.let { dataObject ->
            this.dataObject = dataObject
        }
        super.onViewStateRestored(savedInstanceState)
    }

    companion object {
        const val PARCELABLE_EXTRA_DATA_OBJECT = "parcelableExtraDataObject"
    }
}
package com.ludoscity.findmybikes.ui.trip


import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.ui.main.FindMyBikesActivityViewModel
import com.ludoscity.findmybikes.utils.InjectorUtils
import java.text.NumberFormat

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [TripDetailsFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class TripDetailsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var tripDetailsProximityA: TextView
    private lateinit var tripDetailsProximityB: TextView
    private lateinit var tripDetailsProximitySearch: TextView
    private lateinit var tripDetailsProximityTotal: TextView
    private var tripDetailsSumSeparator: FrameLayout? = null
    private var tripDetailsBToDestinationRow: View? = null
    private lateinit var tripDetailsPinFinalDest: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val inflatedView = inflater.inflate(R.layout.fragment_trip_details, container, false)

        tripDetailsProximityA = inflatedView.findViewById(R.id.trip_details_proximity_a)
        tripDetailsProximityB = inflatedView.findViewById(R.id.trip_details_proximity_b)
        tripDetailsProximitySearch = inflatedView.findViewById(R.id.trip_details_proximity_search)
        tripDetailsProximityTotal = inflatedView.findViewById(R.id.trip_details_proximity_total)
        tripDetailsSumSeparator = inflatedView.findViewById(R.id.trip_details_sum_separator)
        tripDetailsBToDestinationRow = inflatedView.findViewById(R.id.trip_details_b_to_final_dest)
        tripDetailsPinFinalDest = inflatedView.findViewById(R.id.trip_details_final_dest)

        val activityModelFactory = InjectorUtils.provideMainActivityViewModelFactory(activity!!.application)

        val findMyBikesActivityModel = ViewModelProviders.of(activity!!, activityModelFactory).get(FindMyBikesActivityViewModel::class.java)

        //TODO: pass prebuilt factories to fragment (like table fragment ?)
        val modelFactory = InjectorUtils.provideTripFragmentViewModelFactory(activity!!.application,
                findMyBikesActivityModel.userLocation,
                findMyBikesActivityModel.stationALatLng,
                findMyBikesActivityModel.stationBLatLng,
                findMyBikesActivityModel.finalDestinationLatLng,
                findMyBikesActivityModel.isFinalDestinationFavorite,
                NumberFormat.getInstance())

        val fragmentModel = ViewModelProviders.of(this, modelFactory).get(TripFragmentViewModel::class.java)

        fragmentModel.isLastRowVisible.observe(this, Observer {
            if (it == true)
                tripDetailsBToDestinationRow!!.visibility = View.VISIBLE
            else
                tripDetailsBToDestinationRow!!.visibility = View.GONE
        })

        fragmentModel.locToStationAText.observe(this, Observer {
            tripDetailsProximityA.text = it ?: "XXmin"
        })

        fragmentModel.stationAToStationBText.observe(this, Observer {
            tripDetailsProximityB.text = it ?: "XXmin"
        })

        fragmentModel.stationBToFinalDestText.observe(this, Observer {
            tripDetailsProximitySearch.text = it ?: "XXmin"
        })

        fragmentModel.totalTripText.observe(this, Observer {
            tripDetailsProximityTotal.text = it ?: "XXmin"
        })

        fragmentModel.finalDestinationIconResId.observe(this, Observer {
            it?.let { resId ->
                tripDetailsPinFinalDest.setImageResource(resId)
            }
        })

        return inflatedView
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment TripDetailsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                TripDetailsFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}

package com.ludoscity.findmybikes.ui.trip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.databinding.FragmentTripDetailsBinding
import com.ludoscity.findmybikes.ui.main.FindMyBikesActivityViewModel
import com.ludoscity.findmybikes.utils.InjectorUtils
import java.text.NumberFormat

class TripDetailsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val binding: FragmentTripDetailsBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_trip_details, container, false)

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

        val tripFragmentViewModel = ViewModelProviders.of(this, modelFactory).get(TripFragmentViewModel::class.java)

        tripFragmentViewModel.isLastRowVisible.observe(this, Observer {
            if (it == true)
                binding.tripDetailsBToFinalDest.visibility = View.VISIBLE
            else
                binding.tripDetailsBToFinalDest.visibility = View.GONE
        })

        tripFragmentViewModel.locToStationAText.observe(this, Observer {
            binding.tripDetailsProximityA.text = it
        })

        tripFragmentViewModel.stationAToStationBText.observe(this, Observer {
            binding.tripDetailsProximityB.text = it
        })

        tripFragmentViewModel.stationBToFinalDestText.observe(this, Observer {
            binding.tripDetailsProximitySearch.text = it
        })

        tripFragmentViewModel.totalTripText.observe(this, Observer {
            binding.tripDetailsProximityTotal.text = it
        })

        tripFragmentViewModel.finalDestinationIconResId.observe(this, Observer {
            it?.let { resId ->
                binding.tripDetailsFinalDest.setImageResource(resId)
            }
        })

        tripFragmentViewModel.lastStartActivityIntent.observe(this, Observer {
            it?.let { intent ->
                tripFragmentViewModel.clearLastStartActivityRequest()
                startActivity(intent)
            }
        })

        binding.tripDetailsDirectionsLocToA.setOnClickListener {
            tripFragmentViewModel.locToStationADirectionsFabClick()
        }

        binding.tripDetailsDirectionsAToB.setOnClickListener {
            tripFragmentViewModel.stationAToStationBDirectionsFabClick()
        }

        binding.tripDetailsDirectionsBToDestination.setOnClickListener {
            tripFragmentViewModel.stationBTofinalDestinationDirectionsFabClick()
        }

        return binding.root
    }
}
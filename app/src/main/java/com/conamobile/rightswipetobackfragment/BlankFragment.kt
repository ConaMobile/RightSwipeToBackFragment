package com.conamobile.rightswipetobackfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.conamobile.rightswipetobackfragment.fragula.extensions.addFragment
import com.conamobile.rightswipetobackfragment.fragula.extensions.getCallback
import com.fragula.listener.OnFragmentNavigatorListener
import kotlinx.android.synthetic.main.fragment_blank.*
import kotlinx.android.synthetic.main.fragment_blank.view.*

class BlankFragment : Fragment(), OnFragmentNavigatorListener {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_blank, container, false)

        view.text.setOnClickListener {
            addFragment<BlankFragment2>()
            getCallback<ExampleCallback>().onSuccess()
        }

        return view
    }

    override fun onOpenedFragment() {

    }

    override fun onReturnedFragment() {

    }
}
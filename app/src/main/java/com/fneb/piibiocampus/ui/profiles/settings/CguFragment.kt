package com.fneb.piibiocampus.ui.profiles.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.utils.setTopBarTitle

class CguFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_cgu, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setTopBarTitle(R.string.titleCgu)
    }

    override fun onResume() {
        super.onResume()
        setTopBarTitle(R.string.titleCgu)
    }
}
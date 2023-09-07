package com.prac.ai.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.prac.ai.R
import com.prac.ai.databinding.FragmentSafetyBinding

class SafetyFragment : Fragment() {
    lateinit var fragmentSafetyBinding: FragmentSafetyBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentSafetyBinding = FragmentSafetyBinding.inflate(layoutInflater,container,false)
        return fragmentSafetyBinding.root
    }


}
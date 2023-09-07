package com.prac.ai.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.prac.ai.R
import com.prac.ai.databinding.FragmentInfoBinding

class InfoFragment : Fragment() {
    lateinit var fragmentInfoBinding: FragmentInfoBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentInfoBinding = FragmentInfoBinding.inflate(layoutInflater,container,false)
        return fragmentInfoBinding.root
    }


}
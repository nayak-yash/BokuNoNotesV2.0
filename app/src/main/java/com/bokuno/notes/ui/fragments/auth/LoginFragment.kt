package com.bokuno.notes.ui.fragments.auth

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.bokuno.notes.R
import com.bokuno.notes.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var mAuth: FirebaseAuth


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(mAuth.currentUser!=null){
            findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
        }
        binding.btnLogin.setOnClickListener {
            val email=binding.etEmail.text.toString().trim()
            val pass=binding.etPassword.text.toString().trim()
            if(email.isNotEmpty() && pass.isNotEmpty()){
                binding.btnLogin.isClickable=false
                mAuth.signInWithEmailAndPassword(email,pass).addOnSuccessListener {
                    findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
                }
                    .addOnFailureListener {
                        binding.btnLogin.isClickable=true
                        val errorMessage = it.localizedMessage
                        val cropIndex=errorMessage.indexOf('.')
                        if(cropIndex!=-1){
                            errorMessage.substring(0,cropIndex)
                        }
                        Toast.makeText(activity,errorMessage, Toast.LENGTH_SHORT).show()
                    }
            }
            else{
                Toast.makeText(activity,"Fields are not complete", Toast.LENGTH_SHORT).show()
            }

        }

        binding.btnSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
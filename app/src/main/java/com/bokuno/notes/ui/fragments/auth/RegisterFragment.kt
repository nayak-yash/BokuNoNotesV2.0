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
import com.bokuno.notes.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var mAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAuth = FirebaseAuth.getInstance()
        binding.btnRegister.setOnClickListener{
            val email=binding.etEmail.text.toString().trim()
            val pass=binding.etPassword.text.toString().trim()
            val confirmPass=binding.etPasswordConfirm.text.toString().trim()
            if(email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()){
                if(pass == confirmPass){
                    binding.btnRegister.isClickable=false
                    mAuth.createUserWithEmailAndPassword(email,pass).addOnSuccessListener{
                        findNavController().navigate(R.id.action_registerFragment_to_mainFragment)
                    }
                        .addOnFailureListener {
                            binding.btnRegister.isClickable=true
                            when(it) {
                                is FirebaseAuthWeakPasswordException ->
                                    Toast.makeText(
                                        activity,
                                        "Select a strong password",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                else -> {
                                    val errorMessage = it.localizedMessage
                                    val cropIndex=errorMessage.indexOf('.')
                                    if(cropIndex!=-1){
                                        errorMessage.substring(0,cropIndex)
                                    }
                                    Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        }
                }
                else{
                    Toast.makeText(activity,"Confirm password and password are not same", Toast.LENGTH_SHORT).show()
                }
            }
            else{
                Toast.makeText(activity,"Fields are not complete", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSignIn.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
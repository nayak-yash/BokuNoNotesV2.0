package com.bokuno.notes

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.bokuno.notes.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding:ActivityRegisterBinding
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mAuth=FirebaseAuth.getInstance()
        binding.btnRegister.setOnClickListener{
            val email=binding.etEmail.text.toString().trim()
            val pass=binding.etPassword.text.toString().trim()
            val confirmPass=binding.etPasswordConfirm.text.toString().trim()
            if(email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()){
                if(pass == confirmPass){
                    binding.btnRegister.isClickable=false
                    mAuth.createUserWithEmailAndPassword(email,pass).addOnSuccessListener{
                            val loginIntent = Intent(this,LoginActivity::class.java)
                            startActivity(loginIntent)
                            finish()
                    }
                        .addOnFailureListener {
                            binding.btnRegister.isClickable=true
                            when(it) {
                                is FirebaseAuthWeakPasswordException ->
                                    Toast.makeText(
                                        this,
                                        "Select a strong password",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                else -> {
                                    val errorMessage = it.localizedMessage
                                    val cropIndex=errorMessage.indexOf('.')
                                    if(cropIndex!=-1){
                                        errorMessage.substring(0,cropIndex)
                                    }
                                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        }
                }
                else{
                    Toast.makeText(this,"Confirm password and password are not same",Toast.LENGTH_SHORT).show()
                }
            }
            else{
                Toast.makeText(this,"Fields are not complete",Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnSignIn.setOnClickListener {
            finish()
        }
    }
}
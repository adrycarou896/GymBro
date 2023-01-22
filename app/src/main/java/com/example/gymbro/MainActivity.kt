package com.example.gymbro

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.RecyclerView
import com.example.gymbro.databinding.ActivityMainBinding
import com.example.gymbro.model.Exercise
import com.example.gymbro.model.ExerciseInfoItem
import com.example.gymbro.model.MuscleGroup
import com.example.gymbro.model.Util
import com.example.gymbro.model.enums.WeightType
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.lang.Exception
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityMainBinding
    private lateinit var mDatabase: DatabaseReference
    private lateinit var mAuthListener: FirebaseAuth.AuthStateListener
    private var mUserId: String? = null
    private var mFirebaseAuth: FirebaseAuth? = null
    private var mAuthLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
            if(it.resultCode == Activity.RESULT_OK){
                //val value = it.data?.getStringExtra("input")
                Toast.makeText(this, "Bienvenido...", Toast.LENGTH_LONG).show()
            }else{
                //Si canceló el FireBase UI (fué para atras)
                if(IdpResponse.fromResultIntent(it.data) == null){
                    finish()//Finalizamos la activity
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setUpAuth()

        //Hace referencia al nodo principal que se encuentra en bbdd
        // (cuando no se trata de US se debe especificar la url)
        mDatabase = FirebaseDatabase.getInstance("https://gym-bro-7064d-default-rtdb.europe-west1.firebasedatabase.app").reference

        //mDatabase.child("usuario1").child("Pierna").setValue("uoo")
    }

    private fun setUpAuth(){
        mFirebaseAuth = FirebaseAuth.getInstance()
        mAuthListener = FirebaseAuth.AuthStateListener {
            val user = it.currentUser
            if(user == null){//Si el usuario no se ha logueado
                //Lanzar la vista para poder hacerlo
                val intent = AuthUI.getInstance().createSignInIntentBuilder()
                    .setAvailableProviders(
                        Arrays.asList(
                            AuthUI.IdpConfig.EmailBuilder().build(),
                            AuthUI.IdpConfig.GoogleBuilder().build()
                        ))
                    .build()
                mAuthLauncher.launch(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mFirebaseAuth?.addAuthStateListener(mAuthListener)

        mUserId = FirebaseAuth.getInstance().currentUser?.uid
        if(mUserId != null){
            val muscleGroup = MuscleGroup("Espalda")
            val musclerGroupKey = addMuscleGroup(muscleGroup)

            val exercise = Exercise("Pull Down", musclerGroupKey)
            addExercise(exercise)

            findExercisesByMuscleGroupKey(musclerGroupKey)

            /*val musclerGroupKey = "-NMQobAVw4xoMBaddole"
            val exercise = Exercise("Remo", musclerGroupKey)
            addExercise(exercise)

            findExercisesByMuscleGroupKey(musclerGroupKey)*/
        }

    }

     /*
        Para liberar recursos
     */
    override fun onPause() {
        super.onPause()
        mFirebaseAuth?.removeAuthStateListener(mAuthListener)
    }

    fun addMuscleGroup(muscleGroup: MuscleGroup):String{
        val key = mDatabase.push().key!!
        val reference:DatabaseReference? = getMuscleGroupKeyChild(key)?.child(Util.NAME_FIELD);
        reference?.setValue(muscleGroup.name)
            ?: Toast.makeText(this, "Error adding the muscle group", Toast.LENGTH_LONG).show()
        return key
    }

    fun addExercise(exercise: Exercise):String{
        val exerciseKey = mDatabase.push().key!!
        val reference:DatabaseReference? =
            getExerciseKeyChild(exercise.muscleGroupKey, exerciseKey)?.child(Util.NAME_FIELD)
        reference?.setValue(exercise.name)
            ?: Toast.makeText(this, "Error adding the exercise", Toast.LENGTH_LONG).show()
        return exerciseKey;
    }

    fun findExercisesByMuscleGroupKey(muscleGroupKey:String){
        getMuscleGroupKeyChild(muscleGroupKey)?.child(Util.EXERCISES)
            ?.addValueEventListener(object: ValueEventListener{
                override fun onDataChange(exerciseGroup: DataSnapshot) {
                    if(exerciseGroup.exists()){
                        for(exercise in exerciseGroup.children){
                            val exerciseId = exercise.key
                            updateExerciseName(muscleGroupKey, exerciseId.toString())
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    fun updateExerciseName(muscleGroupKey: String, exerciseKey: String){
        getExerciseKeyChild(muscleGroupKey, exerciseKey)
            ?.addValueEventListener(object: ValueEventListener{
                override fun onDataChange(exercise: DataSnapshot) {
                    for(exerciseField in exercise.children){
                        mBinding.textViewName.text =
                            mBinding.textViewName.text.toString() +
                                    exerciseField.value.toString() + ","
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    /*fun addExerciseInfo(exerciseInfoItems: List<ExerciseInfoItem>){
        try{
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if(userId != null){
                for(infoItem in exerciseInfoItems){
                    mDatabase.child(userId).child(muscleGroupName).child(exerciseName)
                        .child(infoItem.order.toString()).child("series")
                        .setValue(infoItem.series.toString())

                    mDatabase.child(userId).child(muscleGroupName).child(exerciseName)
                        .child(infoItem.order.toString()).child("reps")
                        .setValue(infoItem.reps.toString())

                    mDatabase.child(userId).child(muscleGroupName).child(exerciseName)
                        .child(infoItem.order.toString()).child("weight")
                        .setValue(infoItem.weight.toString())

                    mDatabase.child(userId).child(muscleGroupName).child(exerciseName)
                        .child(infoItem.order.toString()).child("weightType")
                        .setValue(infoItem.weightType.toString())
                }
            }
        }catch (ex: Exception){
            Toast.makeText(this, "Error searching exercises", Toast.LENGTH_LONG).show()
        }
    }*/

    fun getExerciseKeyChild(muscleGroupKey: String, exerciseKey:String):DatabaseReference?{
        return getMuscleGroupKeyChild(muscleGroupKey)?.child(Util.EXERCISES)
            ?.child(exerciseKey)
    }

    fun getMuscleGroupKeyChild(key: String):DatabaseReference?{
        return mUserId?.let { mDatabase.child(mUserId.toString()).child(Util.MUSCLE_GROUPS)
            .child(key) }
    }
}
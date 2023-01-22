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
                val value = it.data?.getStringExtra("input")
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

        val muscleGroup = MuscleGroup(1, "Espalda")
        addMuscleGroup(muscleGroup)

        val exercise = Exercise(1, "Pull Down", 1)
        addExercise(exercise)

        findExercisesByMuscleGroupId(muscleGroup.id)

        /*addExercise("Espalda", "Latt pulldown")
        addExercise("Espalda", "Remo")
        findExercisesByMuscleGroup("Espalda")

        val infoItem1 = ExerciseInfoItem(1, 2, 10, null, 45.5F, WeightType.KG, null)
        val infoItems = listOf(infoItem1)
        addExerciseInfo("Espalda", "Latt pulldown", infoItems)*/

    }

     /*
        Para liberar recursos
     */
    override fun onPause() {
        super.onPause()
        mFirebaseAuth?.removeAuthStateListener(mAuthListener)
        mUserId = FirebaseAuth.getInstance().currentUser?.uid
    }

    fun addMuscleGroup(muscleGroup: MuscleGroup){
        val reference:DatabaseReference? = getMuscleGroupIdChild(muscleGroup.id)?.child(Util.NAME_FIELD);
        reference?.setValue(muscleGroup.name)
            ?: Toast.makeText(this, "Error adding the muscle group", Toast.LENGTH_LONG).show()
    }

    fun addExercise(exercise: Exercise){
        val reference:DatabaseReference? =
            getExerciseIdChild(exercise.muscleGroupId, exercise.id)?.child(Util.NAME_FIELD)
        reference?.setValue(exercise.name)
            ?: Toast.makeText(this, "Error adding the exercise", Toast.LENGTH_LONG).show()
    }

    fun findExercisesByMuscleGroupId(muscleGroupId:Long){
        try{
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if(userId != null){
                mDatabase.child(userId).child(Util.MUSCLE_GROUPS).child(muscleGroupId.toString())
                    .child(Util.EXERCISES).addValueEventListener(object: ValueEventListener{
                    override fun onDataChange(exercise: DataSnapshot) {
                        if(exercise.exists()){
                            for(exercise in exercise.children){
                                val exerciseId = exercise.key
                                exercise.child(exerciseId.toString())

                                updateExerciseName(userId, muscleGroupId.toString(), exerciseId.toString())
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                    }
                });
            }
        }catch (ex: Exception){
            Toast.makeText(this, "Error searching exercises", Toast.LENGTH_LONG).show()
        }
    }

    fun updateExerciseName(userId:String, muscleGroupId: String, exerciseId: String){
        mDatabase.child(userId).child(Util.MUSCLE_GROUPS).child(muscleGroupId)
            .child(Util.EXERCISES).child(exerciseId)
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(exerciseInfo: DataSnapshot) {
                    for(node in exerciseInfo.children){
                        mBinding.textViewName.text = node.value.toString()
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

    fun getExerciseIdChild(muscleGroupId: Long, exerciseId:Long):DatabaseReference?{
        return getMuscleGroupIdChild(muscleGroupId)?.child(Util.EXERCISES)
            ?.child(exerciseId.toString())
    }

    fun getMuscleGroupIdChild(muscleGroupId:Long):DatabaseReference?{
        return mUserId?.let { mDatabase.child(mUserId.toString()).child(Util.MUSCLE_GROUPS)
            .child(muscleGroupId.toString()) }
    }
}
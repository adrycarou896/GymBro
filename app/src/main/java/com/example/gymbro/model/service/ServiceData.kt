package com.example.gymbro.model.service

import android.widget.Toast
import com.example.gymbro.databinding.ActivityMainBinding
import com.example.gymbro.model.Exercise
import com.example.gymbro.model.MuscleGroup
import com.example.gymbro.model.Util
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ServiceData {

    private var database: DatabaseReference
    private var userId: String? = null

    constructor(){
        database = FirebaseDatabase.getInstance("https://gym-bro-7064d-default-rtdb.europe-west1.firebasedatabase.app").reference
        userId = FirebaseAuth.getInstance().currentUser?.uid
    }

    fun addMuscleGroup(muscleGroup: MuscleGroup):String{
        val key = database.push().key!!
        val reference: DatabaseReference? = getMuscleGroupKeyChild(key)?.child(Util.NAME_FIELD);
        reference?.setValue(muscleGroup.name)
        return key
    }

    fun addExercise(exercise: Exercise):String{
        val exerciseKey = database.push().key!!
        val reference:DatabaseReference? =
            getExerciseKeyChild(exercise.muscleGroupKey, exerciseKey)?.child(Util.NAME_FIELD)
        reference?.setValue(exercise.name)
        return exerciseKey;
    }

    fun findExercisesByMuscleGroupKey(muscleGroupKey:String, mBinding: ActivityMainBinding){
        getMuscleGroupKeyChild(muscleGroupKey)?.child(Util.EXERCISES)
            ?.addValueEventListener(object: ValueEventListener{
                override fun onDataChange(exerciseGroup: DataSnapshot) {
                    if(exerciseGroup.exists()){
                        for(exercise in exerciseGroup.children){
                            val exerciseId = exercise.key
                            updateExerciseName(muscleGroupKey, exerciseId.toString(), mBinding)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    private fun updateExerciseName(muscleGroupKey: String, exerciseKey: String,
                           mBinding: ActivityMainBinding){
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

    private fun getMuscleGroupKeyChild(key: String):DatabaseReference?{
        return userId?.let { database.child(userId.toString()).child(Util.MUSCLE_GROUPS)
            .child(key) }
    }

    private fun getExerciseKeyChild(muscleGroupKey: String, exerciseKey:String):DatabaseReference?{
        return getMuscleGroupKeyChild(muscleGroupKey)?.child(Util.EXERCISES)
            ?.child(exerciseKey)
    }

    fun getUserId() : String?{
        return userId
    }
}
package com.example.gymbro.model

import com.example.gymbro.model.enums.TimeType
import com.example.gymbro.model.enums.WeightType

class ExerciseInfoItem(val exerciseId: Long,
                       val order:Long,
                       val series: Int,
                       val reps: Int, val time: Float?,
                       val weight: Float,
                       val weightType: WeightType, timeType: TimeType?)
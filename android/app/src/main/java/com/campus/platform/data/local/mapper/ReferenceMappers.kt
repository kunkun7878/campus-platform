package com.campus.platform.data.local.mapper

import com.campus.platform.data.local.entity.CampusEntity
import com.campus.platform.data.local.entity.SchoolEntity
import com.campus.platform.data.model.Campus
import com.campus.platform.data.model.School

fun School.toEntity(): SchoolEntity = SchoolEntity(
    id = id,
    name = name,
    abbreviation = abbreviation,
    city = city,
    province = province,
    createdAt = createdAt,
)

fun SchoolEntity.toDto(): School = School(
    id = id,
    name = name,
    abbreviation = abbreviation,
    city = city,
    province = province,
    createdAt = createdAt,
)

fun Campus.toEntity(): CampusEntity = CampusEntity(
    id = id,
    schoolId = schoolId,
    name = name,
    address = address,
    createdAt = createdAt,
)

fun CampusEntity.toDto(): Campus = Campus(
    id = id,
    schoolId = schoolId,
    name = name,
    address = address,
    createdAt = createdAt,
)

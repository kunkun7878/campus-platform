package com.campus.platform.data.school

import com.campus.platform.data.model.Campus
import com.campus.platform.data.model.School
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 学校与校区数据仓库。
 *
 * 封装 Supabase PostgREST 查询，提供学校和校区列表的获取、
 * 以及用户选校/切换校区的操作。
 */
@Singleton
class SchoolRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {

    /** 获取所有学校 */
    suspend fun getSchools(): List<School> {
        return supabase.postgrest
            .from("schools")
            .select()
            .decodeList<School>()
    }

    /** 获取所有学校（Flow） */
    fun getSchoolsFlow(): Flow<List<School>> = flow {
        emit(getSchools())
    }

    /** 获取指定学校的校区列表 */
    suspend fun getCampuses(schoolId: String): List<Campus> {
        return supabase.postgrest
            .from("campuses")
            .select { filter { eq("school_id", schoolId) } }
            .decodeList<Campus>()
    }

    /** 获取指定学校的校区列表（Flow） */
    fun getCampusesFlow(schoolId: String): Flow<List<Campus>> = flow {
        emit(getCampuses(schoolId))
    }

    /** 获取所有校区（含学校信息，用于学校-校区两级选择器） */
    suspend fun getAllCampusesWithSchools(): Map<School, List<Campus>> {
        val schools = getSchools()
        val result = mutableMapOf<School, List<Campus>>()
        for (school in schools) {
            result[school] = getCampuses(school.id)
        }
        return result
    }
}

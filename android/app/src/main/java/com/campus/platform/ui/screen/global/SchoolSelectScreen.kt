package com.campus.platform.ui.screen.global

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.model.Campus
import com.campus.platform.data.model.School
import com.campus.platform.data.school.SchoolRepository
import kotlinx.coroutines.launch

/**
 * 学校-校区选择页。
 *
 * 两级选择器：先选学校 → 再选校区。
 * - 四川师范大学（3 校区）：显示两级选择
 * - 四川邮电职业技术学院（1 校区）：可跳过校区选择，直接确认
 * - 选校确认后回调写入 profiles，后续不可更改学校
 *
 * @param authRepository 用于写入用户选校
 * @param schoolRepository 用于查询学校/校区列表
 * @param onSchoolSelected 选校完成回调（导航至首页）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SchoolSelectScreen(
    authRepository: AuthRepository,
    schoolRepository: SchoolRepository,
    onSchoolSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 学校列表
    var schools by remember { mutableStateOf<List<School>>(emptyList()) }
    var allCampuses by remember { mutableStateOf<Map<String, List<Campus>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // 选择状态
    var selectedSchool by remember { mutableStateOf<School?>(null) }
    var selectedCampus by remember { mutableStateOf<Campus?>(null) }

    // 加载学校数据
    LaunchedEffect(Unit) {
        try {
            val schoolList = schoolRepository.getSchools()
            schools = schoolList
            val campusMap = mutableMapOf<String, List<Campus>>()
            for (s in schoolList) {
                campusMap[s.id] = schoolRepository.getCampuses(s.id)
            }
            allCampuses = campusMap
        } catch (e: Exception) {
            error = e.message ?: "加载学校列表失败"
        } finally {
            isLoading = false
        }
    }

    // 当选中的学校变化时，如果只有一个校区自动选中
    LaunchedEffect(selectedSchool) {
        val school = selectedSchool ?: return@LaunchedEffect
        val campuses = allCampuses[school.id] ?: emptyList()
        if (campuses.size == 1) {
            selectedCampus = campuses.first()
        } else {
            // 多校区时清除之前的校区选择
            selectedCampus = null
        }
    }

    fun confirmSelection() {
        val school = selectedSchool ?: return
        val campus = selectedCampus ?: return
        scope.launch {
            isSubmitting = true
            try {
                val uid = authRepository.currentUserId() ?: return@launch
                authRepository.selectSchool(uid, school.id, campus.id)
                onSchoolSelected()
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar(e.message ?: "选校失败，请重试")
                }
            } finally {
                isSubmitting = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "选择学校",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (selectedSchool == null) "请先选择您所在的学校"
                else "请选择您的校区",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else if (error != null) {
                Text(
                    text = error ?: "加载失败",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                // ── 步骤指示器 ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "1. 选择学校",
                        fontWeight = if (selectedSchool == null) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedSchool == null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "→",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "2. 选择校区",
                        fontWeight = if (selectedSchool != null && selectedCampus == null) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedSchool != null && selectedCampus == null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── 学校列表 ──
                if (selectedSchool == null) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        items(schools) { school ->
                            val campusCount = allCampuses[school.id]?.size ?: 0
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSchool = school },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                ) {
                                    Text(
                                        text = school.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = buildString {
                                            school.abbreviation?.let { append("$it · ") }
                                            append("${campusCount} 个校区")
                                            school.city?.let { append(" · $it") }
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                // ── 校区列表 ──
                if (selectedSchool != null) {
                    // 已选学校卡片
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "已选：${selectedSchool?.name ?: ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            TextButtonStyled(
                                text = "重新选择",
                                onClick = { selectedSchool = null; selectedCampus = null },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val campuses = selectedSchool?.let { allCampuses[it.id] } ?: emptyList()

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        items(campuses) { campus ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCampus = campus },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedCampus?.id == campus.id)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                ),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                ) {
                                    Text(
                                        text = campus.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    if (!campus.address.isNullOrBlank()) {
                                        Text(
                                            text = campus.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 确认按钮 ──
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { confirmSelection() },
                    enabled = selectedSchool != null && selectedCampus != null && !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("确认选择")
                    }
                }
            }
        }
    }
}

/**
 * 简化的 TextButton，用颜色区分而不是 Material3 样式。
 */
@Composable
private fun TextButtonStyled(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

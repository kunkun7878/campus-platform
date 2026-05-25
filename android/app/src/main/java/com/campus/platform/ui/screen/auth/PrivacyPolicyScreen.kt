package com.campus.platform.ui.screen.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/** 隐私政策页面 — 静态展示 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("隐私政策") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text(
                text = "校园聚合平台隐私政策",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.padding(vertical = 16.dp))
            Text(
                text = PRIVACY_TEXT,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
            )
        }
    }
}

private val PRIVACY_TEXT = """
更新日期：2025年1月1日
生效日期：2025年1月1日

校园聚合平台（以下简称"我们"）深知个人信息对您的重要性，我们将按照法律法规的规定，保护您的个人信息安全。

一、我们收集的信息
1.1 注册信息：手机号码、学校信息、学号等用于账号注册和身份验证。
1.2 用户资料：昵称、头像、个人简介等由您主动提供的信息。
1.3 交易信息：二手交易记录、跑腿订单信息等用于服务完成的必要信息。
1.4 位置信息：用于跑腿服务和失物招领的位置匹配，仅在您授权后获取。
1.5 设备信息：设备型号、操作系统版本等用于服务优化和问题排查。

二、信息的使用
2.1 我们使用您的信息用于以下目的：
  （1）提供、维护和改善我们的服务；
  （2）处理交易和发送相关通知；
  （3）身份验证和安全保障；
  （4）响应您的客服请求；
  （5）根据法律法规的要求进行信息披露。

三、信息的存储
3.1 您的信息存储在中国境内的服务器上。
3.2 我们采用加密传输（TLS）和安全存储措施保护您的数据。
3.3 账号注销后，您的个人信息将在合理期限内匿名化处理或删除。

四、信息的共享
4.1 我们不会将您的信息出售给第三方。
4.2 在以下情况下，我们可能共享您的信息：
  （1）获得您的明确同意；
  （2）法律法规要求的披露；
  （3）与合作伙伴共享必要信息以提供服务。
4.3 跑腿订单中的联系方式将在任务完成后对双方隐藏。

五、您的权利
5.1 您有权访问、更正、删除您的个人信息。
5.2 您有权撤回对信息收集的授权（可能影响部分功能使用）。
5.3 您有权注销账号，注销后相关数据将按法律规定处理。

六、未成年人保护
6.1 我们重视对未成年人个人信息的保护。
6.2 如您是未成年人，请在法定监护人的陪同下使用本平台。

七、政策更新
7.1 我们可能适时更新本隐私政策，更新后将在平台公示。
7.2 重大变更将通过站内消息或短信通知。

八、联系我们
如您对本隐私政策有任何疑问或建议，请通过平台的"意见反馈"功能联系我们。
""".trimIndent()

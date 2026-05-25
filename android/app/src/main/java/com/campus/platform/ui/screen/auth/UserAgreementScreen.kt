package com.campus.platform.ui.screen.auth

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

/** 用户协议页面 — 静态展示 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAgreementScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("用户协议") },
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
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text(
                text = "校园聚合平台用户协议",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
            )
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.padding(vertical = 16.dp),
            )
            Text(
                text = AGREEMENT_TEXT,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
            )
        }
    }
}

private val AGREEMENT_TEXT = """
欢迎使用校园聚合平台（以下简称"本平台"）。

一、总则
1.1 本协议是用户（以下简称"您"）与本平台之间关于使用本平台服务所订立的协议。
1.2 您在注册或使用本平台服务前，请务必审慎阅读、充分理解本协议各条款内容。
1.3 当您勾选"同意协议"并完成注册后，即表示您已充分阅读、理解并接受本协议的全部内容。

二、账号注册与管理
2.1 您在注册时应提供真实、准确、完整的个人资料，并在信息变更时及时更新。
2.2 您应妥善保管账号和密码，因账号密码保管不善造成的损失由您自行承担。
2.3 每个手机号仅可注册一个账号，同一用户不得注册多个账号。

三、用户行为规范
3.1 您承诺在使用本平台服务过程中遵守国家法律法规及学校相关规定。
3.2 您不得利用本平台从事以下行为：
  （1）发布、传播违法或不良信息；
  （2）侵犯他人知识产权、商业秘密等合法权益；
  （3）欺诈、虚假交易等损害其他用户利益的行为；
  （4）干扰本平台正常运营的行为。

四、平台服务
4.1 本平台提供的服务包括但不限于：二手交易、跑腿服务、失物招领、社区交流等。
4.2 本平台有权根据运营需要调整服务内容和范围。
4.3 对于跑腿服务，本平台仅提供信息撮合服务，不对服务质量承担保证责任。

五、责任限制
5.1 本平台对因不可抗力、系统维护、网络故障等原因造成的服务中断不承担责任。
5.2 用户之间因交易产生的纠纷，应自行协商解决，本平台可提供必要的协助。

六、隐私保护
6.1 本平台重视用户隐私保护，具体见《隐私政策》。

七、协议修改
7.1 本平台有权在必要时修改本协议，修改后的协议将在平台公示。
7.2 如您不同意修改后的协议，应停止使用本平台服务。

八、其他
8.1 本协议未涉及的问题参见国家有关法律法规。
8.2 本协议最终解释权归本平台所有。

如您对本协议有任何疑问，请联系平台客服。
""".trimIndent()

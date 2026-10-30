package com.flymind.ui.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flymind.ui.theme.Bg
import com.flymind.ui.theme.Fill
import com.flymind.ui.theme.Separator
import com.flymind.ui.theme.Surface
import com.flymind.ui.theme.Teal
import com.flymind.ui.theme.TextPrimary
import com.flymind.ui.theme.TextSecondary
import com.flymind.ui.theme.TextTertiary

data class IosTabItem(val label: String, val icon: ImageVector)

/**
 * Barra de abas do iOS: 49 dp de altura, fundo branco, fio de 0,5 dp em cima,
 * ícones de 25 dp e rótulos de 10 dp. Sem pílula de seleção e sem ripple.
 */
@Composable
fun IosTabBar(
    items: List<IosTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().background(Surface)) {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(Separator))
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(49.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val tint = if (selected) Teal else TextTertiary
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .height(49.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.size(25.dp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.label,
                        fontSize = 10.sp,
                        color = tint,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

/**
 * Cabeçalho de título grande: 34 dp em negrito com subtítulo de 13 dp, como a
 * primeira dobra dos apps do iOS. A ação à direita fica alinhada ao título.
 */
@Composable
fun IosLargeHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Bg)
            .statusBarsPadding()
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 34.sp,
                lineHeight = 41.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
                color = TextPrimary,
            )
            if (subtitle != null) {
                Text(subtitle, fontSize = 13.sp, lineHeight = 18.sp, color = TextSecondary)
            }
        }
        if (trailing != null) {
            Row(verticalAlignment = Alignment.CenterVertically) { trailing() }
        }
    }
}

/**
 * Barra de navegação compacta: botão de voltar em pílula cinza, título
 * centralizado de 17 dp e ações à direita.
 */
@Composable
fun IosNavBar(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().background(Bg).statusBarsPadding()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 8.dp),
        ) {
            if (onBack != null) {
                Row(
                    Modifier
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Fill)
                        .clickable(onClick = onBack)
                        .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.ChevronLeft,
                        contentDescription = "Voltar",
                        tint = Teal,
                        modifier = Modifier.size(20.dp),
                    )
                    Text("Voltar", fontSize = 15.sp, color = Teal)
                }
            }
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center),
            )
            if (trailing != null) {
                Row(Modifier.align(Alignment.CenterEnd)) { trailing() }
            }
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(Separator))
    }
}

/** Aviso do iOS: cápsula escura flutuante com texto branco de 15 dp. */
@Composable
fun IosSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState, modifier) { data ->
        Box(
            Modifier
                .padding(horizontal = 24.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xE62C2C2E))
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            Text(
                data.visuals.message,
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            )
        }
    }
}

/** Botão circular cinza usado no canto da barra de título. */
@Composable
fun IosCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Teal,
    size: Int = 30,
) {
    Box(
        modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Fill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size((size * 0.55f).dp),
        )
    }
}


package com.dlust.ydhl2026.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.dlust.ydhl2026.ui.components.EchoMascot
import com.dlust.ydhl2026.ui.components.MascotGaze
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BrandBlue = Color(0xFF1B3FA0)
private val ErrorRed = Color(0xFFD33A31)
private val TextGray = Color(0xFF8A8F99)
private val BorderGray = Color(0xFFD9DDE3)

/** 模拟登录固定凭据，接后端时只需替换 attemptLogin 中的校验逻辑 */
private const val MOCK_CREDENTIAL = "123456"

private const val COUNTDOWN_SECONDS = 60

private fun isPhoneValid(phone: String): Boolean =
  phone.length == 11 && phone.startsWith("1") && phone.all { it.isDigit() }

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var phone by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var smsCode by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var useSmsLogin by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var smsSentHint by remember { mutableStateOf(false) }
  var countdown by remember { mutableIntStateOf(0) }
  var isLoading by remember { mutableStateOf(false) }
  var gaze by remember { mutableStateOf(MascotGaze.CENTER) }
  var typePulse by remember { mutableIntStateOf(0) }
  val brandColor = BrandBlue
  var isDark by remember { mutableStateOf(false) }

  val bgColor = if (isDark) Color(0xFF0F1115) else Color.White
  val textMain = if (isDark) Color(0xFFE8EAF0) else Color(0xFF22262E)
  val textSecondary = if (isDark) Color(0xFF9AA3B2) else TextGray
  val borderColor = if (isDark) Color(0xFF343A46) else BorderGray
  val errorColor = if (isDark) Color(0xFFFF6E63) else ErrorRed

  LaunchedEffect(countdown) {
    if (countdown > 0) {
      delay(1000)
      countdown--
    }
  }

  val phoneValid = isPhoneValid(phone)
  val loginEnabled = when {
    isLoading -> false
    !phoneValid -> false
    useSmsLogin -> smsCode.isNotEmpty()
    else -> password.isNotEmpty()
  }

  fun attemptLogin() {
    if (!phoneValid) {
      errorMessage = "请输入正确的手机号"
      return
    }
    scope.launch {
      isLoading = true
      errorMessage = null
      delay(600)
      isLoading = false
      val passed = if (useSmsLogin) smsCode == MOCK_CREDENTIAL else password == MOCK_CREDENTIAL
      if (passed) {
        onLoginSuccess()
      } else {
        errorMessage = if (useSmsLogin) "验证码错误，请重新输入" else "密码错误，请重新输入"
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(bgColor)
      .statusBarsPadding()
      .imePadding()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 28.dp)
  ) {
    Spacer(Modifier.height(12.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      DarkModeButton(
        isDark = isDark,
        borderColor = borderColor,
        onClick = { isDark = !isDark }
      )
      // 测试入口：跳过账号校验直接进首页，方便频繁重装后快速验证页面
      Text(
        text = "跳过",
        color = textSecondary,
        fontSize = 13.sp,
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable(onClick = onLoginSuccess)
          .padding(horizontal = 10.dp, vertical = 6.dp)
      )
    }
    Spacer(Modifier.height(28.dp))
    Text(
      text = "Welcome",
      color = brandColor,
      fontSize = 28.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.align(Alignment.CenterHorizontally)
    )
    Spacer(Modifier.height(6.dp))
    EchoMascot(
      gaze = gaze,
      coveringEyes = gaze == MascotGaze.PASSWORD && !passwordVisible,
      isSad = errorMessage != null,
      typePulseKey = typePulse,
      faceColor = brandColor,
      modifier = Modifier.align(Alignment.CenterHorizontally)
    )
    Spacer(Modifier.height(20.dp))

    OutlinedTextField(
      value = phone,
      onValueChange = { input ->
        phone = input.filter { it.isDigit() }.take(11)
        typePulse++
        errorMessage = null
      },
      label = { Text("手机号") },
      textStyle = TextStyle(fontSize = 14.sp),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      singleLine = true,
      shape = RoundedCornerShape(12.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = textMain,
        unfocusedTextColor = textMain,
        focusedBorderColor = brandColor,
        unfocusedBorderColor = borderColor,
        cursorColor = brandColor,
        focusedLabelColor = brandColor,
        unfocusedLabelColor = textSecondary
      ),
      modifier = Modifier
        .fillMaxWidth()
        .onFocusChanged { if (it.isFocused) gaze = MascotGaze.PHONE }
    )

    Spacer(Modifier.height(24.dp))
    LoginMethodTab(
      useSmsLogin = useSmsLogin,
      brandColor = brandColor,
      textSecondary = textSecondary,
      onSelect = { useSmsLogin = it; errorMessage = null }
    )
    Spacer(Modifier.height(16.dp))

    if (useSmsLogin) {
      OutlinedTextField(
        value = smsCode,
        onValueChange = { input ->
          smsCode = input.filter { it.isDigit() }.take(6)
          typePulse++
          errorMessage = null
        },
        label = { Text("验证码") },
        textStyle = TextStyle(fontSize = 14.sp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = textMain,
          unfocusedTextColor = textMain,
          focusedBorderColor = brandColor,
          unfocusedBorderColor = borderColor,
          cursorColor = brandColor,
          focusedLabelColor = brandColor,
          unfocusedLabelColor = textSecondary
        ),
        trailingIcon = {
          TextButton(
            onClick = {
              countdown = COUNTDOWN_SECONDS
              smsSentHint = true
              Toast.makeText(context, "验证码已发送（模拟）：123456", Toast.LENGTH_SHORT).show()
            },
            enabled = countdown == 0 && phoneValid
          ) {
            Text(
              text = if (countdown > 0) "${countdown}s 后重发" else "获取验证码",
              color = if (countdown == 0 && phoneValid) brandColor else textSecondary,
              fontSize = 12.sp
            )
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .onFocusChanged { if (it.isFocused) gaze = MascotGaze.SMS }
      )
      if (smsSentHint) {
        Spacer(Modifier.height(6.dp))
        Text(
          text = "验证码已发送（模拟）：123456",
          color = brandColor,
          fontSize = 11.sp
        )
      }
    } else {
      OutlinedTextField(
        value = password,
        onValueChange = { input ->
          password = input
          typePulse++
          errorMessage = null
        },
        label = { Text("密码") },
        textStyle = TextStyle(fontSize = 14.sp),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        visualTransformation =
          if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = textMain,
          unfocusedTextColor = textMain,
          focusedBorderColor = brandColor,
          unfocusedBorderColor = borderColor,
          cursorColor = brandColor,
          focusedLabelColor = brandColor,
          unfocusedLabelColor = textSecondary
        ),
        trailingIcon = {
          TextButton(onClick = { passwordVisible = !passwordVisible }) {
            Text(
              text = if (passwordVisible) "隐藏" else "显示",
              color = brandColor,
              fontSize = 12.sp
            )
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .onFocusChanged { if (it.isFocused) gaze = MascotGaze.PASSWORD }
      )
    }

    errorMessage?.let { message ->
      Spacer(Modifier.height(10.dp))
      Text(
        text = message,
        color = errorColor,
        fontSize = 12.sp
      )
    }

    Spacer(Modifier.height(24.dp))
    Button(
      onClick = { attemptLogin() },
      enabled = loginEnabled,
      shape = RoundedCornerShape(21.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = brandColor,
        disabledContainerColor = brandColor.copy(alpha = 0.35f)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .height(42.dp)
    ) {
      if (isLoading) {
        CircularProgressIndicator(
          color = Color.White,
          strokeWidth = 2.dp,
          modifier = Modifier.size(18.dp)
        )
      } else {
        Text(
          text = "登录",
          color = Color.White,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }

    Spacer(Modifier.height(40.dp))
    Text(
      text = "其他登录方式",
      color = textSecondary,
      fontSize = 11.sp,
      modifier = Modifier.align(Alignment.CenterHorizontally)
    )
    Spacer(Modifier.height(12.dp))
    Row(
      modifier = Modifier.align(Alignment.CenterHorizontally),
      horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      OtherLoginCircle("微", borderColor, textSecondary) {
        Toast.makeText(context, "微信登录暂未接入，敬请期待", Toast.LENGTH_SHORT).show()
      }
      OtherLoginCircle("Q", borderColor, textSecondary) {
        Toast.makeText(context, "QQ 登录暂未接入，敬请期待", Toast.LENGTH_SHORT).show()
      }
    }
    Spacer(Modifier.height(16.dp))
    AgreementLine(
      brandColor = brandColor,
      textSecondary = textSecondary,
      onTapLink = {
        Toast.makeText(context, "文档整理中，敬请期待", Toast.LENGTH_SHORT).show()
      }
    )
    Spacer(Modifier.height(16.dp))
  }
}

@Composable
private fun DarkModeButton(isDark: Boolean, borderColor: Color, onClick: () -> Unit) {
  val knobBg = if (isDark) Color(0xFF2A2E36) else Color(0xFFF0F1F4)
  Box(
    modifier = Modifier
      .size(32.dp)
      .background(knobBg, CircleShape)
      .border(width = 2.dp, color = borderColor, shape = CircleShape)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.size(16.dp)) {
      // 月亮：实心圆 + 背景色偏移圆剪出月牙
      val r = size.minDimension / 2f
      val moonColor = if (isDark) Color.White else Color(0xFF20242C)
      drawCircle(color = moonColor, radius = r, center = center)
      drawCircle(
        color = knobBg,
        radius = r * 0.82f,
        center = center + Offset(r * 0.5f, -r * 0.38f)
      )
    }
  }
}

@Composable
private fun LoginMethodTab(
  useSmsLogin: Boolean,
  brandColor: Color,
  textSecondary: Color,
  onSelect: (Boolean) -> Unit
) {
  Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
    TabItem(text = "密码登录", selected = !useSmsLogin, brandColor = brandColor, textSecondary = textSecondary) { onSelect(false) }
    TabItem(text = "验证码登录", selected = useSmsLogin, brandColor = brandColor, textSecondary = textSecondary) { onSelect(true) }
  }
}

@Composable
private fun TabItem(
  text: String,
  selected: Boolean,
  brandColor: Color,
  textSecondary: Color,
  onClick: () -> Unit
) {
  Column(
    modifier = Modifier.clickable(onClick = onClick),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = text,
      color = if (selected) brandColor else textSecondary,
      fontSize = 13.sp,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
    )
    Spacer(Modifier.height(4.dp))
    Box(
      modifier = Modifier
        .width(20.dp)
        .height(2.dp)
        .background(if (selected) brandColor else Color.Transparent)
    )
  }
}

@Composable
private fun OtherLoginCircle(
  label: String,
  borderColor: Color,
  labelColor: Color,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(36.dp)
      .border(width = 1.dp, color = borderColor, shape = CircleShape)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      color = labelColor,
      fontSize = 13.sp
    )
  }
}

@Composable
private fun AgreementLine(brandColor: Color, textSecondary: Color, onTapLink: () -> Unit) {
  Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = Alignment.Center
  ) {
    Row {
      Text(text = "登录即代表同意 ", color = textSecondary, fontSize = 10.sp)
      Text(
        text = "《用户协议》",
        color = brandColor,
        fontSize = 10.sp,
        modifier = Modifier.clickable(onClick = onTapLink)
      )
      Text(text = " 和 ", color = textSecondary, fontSize = 10.sp)
      Text(
        text = "《隐私政策》",
        color = brandColor,
        fontSize = 10.sp,
        modifier = Modifier.clickable(onClick = onTapLink)
      )
    }
  }
}

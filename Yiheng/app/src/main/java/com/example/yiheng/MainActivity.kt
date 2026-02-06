package com.example.yiheng

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.yiheng.ui.theme.YihengTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YihengTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding -> // 修正：fillMaxSize() 需要作为函数调用
                    Column(modifier = Modifier.padding(innerPadding)) {
                        // 私房菜按钮
                        Button(onClick = { openPrivateDishActivity() }) {
                            Text(text = "私房菜")
                        }
                        // 纪念日记录按钮
                        Button(onClick = { openAnniversaryActivity() }, modifier = Modifier.padding(top = 16.dp)) {
                            Text(text = "纪念日记录")
                        }
                    }
                }
            }
        }
    }

    // 跳转到私房菜页面
    private fun openPrivateDishActivity() {
        val intent = Intent(this, PrivateDishActivity::class.java)
        startActivity(intent)
    }

    // 跳转到纪念日记录页面
    private fun openAnniversaryActivity() {
        val intent = Intent(this, AnniversaryActivity::class.java)
        startActivity(intent)
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    YihengTheme {
        Column {
            Button(onClick = {}) {
                Text(text = "私房菜")
            }
            Button(onClick = {}) {
                Text(text = "纪念日记录")
            }
        }
    }
}

package com.example.yiheng

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.yiheng.ui.theme.YihengTheme

class AnniversaryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YihengTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        // 显示纪念日记录标题
                        Text(text = "纪念日记录")
                        // 在这里可以添加更多的纪念日记录信息
                        Text(text = "2026-02-04: 纪念日1")
                        Text(text = "2026-03-15: 纪念日2")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AnniversaryPreview() {
    YihengTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(it)) {
                Text(text = "纪念日记录")
                Text(text = "2026-02-04: 纪念日1")
                Text(text = "2026-03-15: 纪念日2")
            }
        }
    }
}
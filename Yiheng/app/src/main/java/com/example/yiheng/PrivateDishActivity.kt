@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.yiheng

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.yiheng.ui.theme.YihengTheme
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// --- 数据模型 ---

data class Dish(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val imageUri: String?,
    val price: String,
    val firstMakeDate: String,
    val remark: String,
    val steps: List<String>,
    val category: String,
    val mainIngredients: String? = null
)

enum class OilLevel(val label: String) { NORMAL("正常"), LESS("少油"), BOILED("水煮") }
enum class DietaryRestriction(val label: String) { NONE("无"), NO_ONION("去葱"), NO_GARLIC("去蒜"), NO_GINGER("去姜") }
enum class SpicyLevel(val label: String) { MILD("微辣"), MEDIUM("中辣"), EXTRA("麻辣") }

data class OrderItem(
    val dishId: Long,
    val dishName: String,
    val dishImage: String?,
    val oilLevel: OilLevel,
    val dietaries: List<DietaryRestriction>,
    val spicy: SpicyLevel,
    val steps: List<String>
)

data class Order(
    val id: Long = System.currentTimeMillis(),
    val items: List<OrderItem>,
    val timestamp: String,
    var reviewImages: List<String> = emptyList(),
    var reviewText: String? = null,
    var rating: Int = 0,
    var isCompleted: Boolean = false
)

// --- 持久化 (已升级版本以防旧数据冲突) ---
object DishStore {
    private const val PREF_NAME = "PrivateDishesV4"
    private const val KEY_DISHES = "dishes"
    private const val KEY_CATEGORIES = "categories"
    private const val KEY_ORDERS = "orders"

    fun saveDishes(context: Context, dishes: List<Dish>) {
        val json = JSONArray()
        dishes.forEach { d ->
            json.put(JSONObject().apply {
                put("id", d.id); put("name", d.name); put("imageUri", d.imageUri ?: "")
                put("price", d.price); put("date", d.firstMakeDate); put("remark", d.remark)
                put("steps", JSONArray(d.steps)); put("category", d.category); put("ingredients", d.mainIngredients ?: "")
            })
        }
        context.getSharedPreferences(PREF_NAME, 0).edit().putString(KEY_DISHES, json.toString()).apply()
    }

    fun loadDishes(context: Context): List<Dish> {
        val s = context.getSharedPreferences(PREF_NAME, 0).getString(KEY_DISHES, null) ?: return emptyList()
        val list = mutableListOf<Dish>()
        try {
            val arr = JSONArray(s)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val steps = o.getJSONArray("steps")
                list.add(Dish(
                    o.getLong("id"), o.getString("name"), o.optString("imageUri", "").takeIf { it.isNotEmpty() },
                    o.getString("price"), o.getString("date"), o.getString("remark"),
                    List(steps.length()) { steps.getString(it) }, o.getString("category"), o.optString("ingredients", "")
                ))
            }
        } catch (e: Exception) {}
        return list
    }

    fun saveOrders(context: Context, orders: List<Order>) {
        val json = JSONArray()
        orders.forEach { r ->
            val items = JSONArray()
            r.items.forEach { itm ->
                items.put(JSONObject().apply {
                    put("id", itm.dishId); put("name", itm.dishName); put("img", itm.dishImage ?: "")
                    put("oil", itm.oilLevel.name); put("spicy", itm.spicy.name)
                    put("diet", JSONArray(itm.dietaries.map { it.name }))
                    put("steps", JSONArray(itm.steps))
                })
            }
            json.put(JSONObject().apply {
                put("id", r.id); put("items", items); put("time", r.timestamp)
                put("imgs", JSONArray(r.reviewImages)); put("txt", r.reviewText ?: "")
                put("rating", r.rating); put("done", r.isCompleted)
            })
        }
        context.getSharedPreferences(PREF_NAME, 0).edit().putString(KEY_ORDERS, json.toString()).apply()
    }

    fun loadOrders(context: Context): List<Order> {
        val s = context.getSharedPreferences(PREF_NAME, 0).getString(KEY_ORDERS, null) ?: return emptyList()
        val list = mutableListOf<Order>()
        try {
            val arr = JSONArray(s)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val itmsArr = o.getJSONArray("items")
                val itms = List(itmsArr.length()) { j ->
                    val io = itmsArr.getJSONObject(j)
                    val dArr = io.getJSONArray("diet")
                    val sArr = io.getJSONArray("steps")
                    OrderItem(
                        io.getLong("id"), io.getString("name"), io.optString("img", "").takeIf { it.isNotEmpty() },
                        OilLevel.valueOf(io.getString("oil")),
                        List(dArr.length()) { DietaryRestriction.valueOf(dArr.getString(it)) },
                        SpicyLevel.valueOf(io.getString("spicy")),
                        List(sArr.length()) { sArr.getString(it) }
                    )
                }
                val iArr = o.getJSONArray("imgs")
                list.add(Order(
                    o.getLong("id"), itms, o.getString("time"),
                    List(iArr.length()) { iArr.getString(it) }, o.optString("txt").takeIf { it.isNotEmpty() },
                    o.getInt("rating"), o.getBoolean("done")
                ))
            }
        } catch (e: Exception) {}
        return list
    }

    fun saveCats(context: Context, cats: List<String>) {
        context.getSharedPreferences(PREF_NAME, 0).edit().putString(KEY_CATEGORIES, JSONArray(cats).toString()).apply()
    }

    fun loadCats(context: Context): List<String> {
        val s = context.getSharedPreferences(PREF_NAME, 0).getString(KEY_CATEGORIES, null) ?: return listOf("荤菜", "素菜")
        val arr = JSONArray(s)
        return List(arr.length()) { arr.getString(it) }
    }
}

// --- App ---
class PrivateDishActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { YihengTheme { PrivateDishApp() } }
    }
}

@Composable
fun PrivateDishApp() {
    val context = LocalContext.current
    var dishList: List<Dish> by remember { mutableStateOf(DishStore.loadDishes(context)) }
    var categoryList by remember { mutableStateOf(DishStore.loadCats(context)) }
    var orderHistory by remember { mutableStateOf(DishStore.loadOrders(context)) }
    val currentCart = remember { mutableStateListOf<OrderItem>() }

    var editingDish: Dish? by remember { mutableStateOf(null) }
    var isAdding by remember { mutableStateOf(false) }
    var viewingDish by remember { mutableStateOf<Dish?>(null) }
    var isManagingCats by remember { mutableStateOf(false) }
    var isHistoryView by remember { mutableStateOf(false) }
    var viewingOrder by remember { mutableStateOf<Order?>(null) }
    var selectedCat by remember { mutableStateOf(categoryList.firstOrNull() ?: "") }
    var searchQuery by remember { mutableStateOf("") }
    var specDish by remember { mutableStateOf<Dish?>(null) }

    LaunchedEffect(dishList) { DishStore.saveDishes(context, dishList) }
    LaunchedEffect(categoryList) { DishStore.saveCats(context, categoryList) }
    LaunchedEffect(orderHistory) { DishStore.saveOrders(context, orderHistory) }

    if (specDish != null) {
        SpecDialog(specDish!!, { specDish = null }, { currentCart.add(it); specDish = null })
    }

    Box(Modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant))
    )) {
        // 使用 AnimatedContent 增加页面切换动感
        AnimatedContent(
            targetState = when {
                viewingOrder != null -> 0
                isHistoryView -> 1
                isManagingCats -> 2
                isAdding || editingDish != null -> 3
                viewingDish != null -> 4
                else -> 5
            },
            label = "screen_transition"
        ) { state ->
            when (state) {
                0 -> OrderDetailScreen(viewingOrder!!, { viewingOrder = null }, { img, txt, rate ->
                    orderHistory = orderHistory.map { if (it.id == viewingOrder!!.id) it.copy(reviewImages = img, reviewText = txt, rating = rate, isCompleted = true) else it }
                    viewingOrder = null
                }, { orderHistory = orderHistory.filter { it.id != viewingOrder!!.id }; viewingOrder = null })
                1 -> HistoryScreen(orderHistory, { isHistoryView = false }, { viewingOrder = it }, { orderToDelete -> orderHistory = orderHistory.filter { it.id != orderToDelete.id } })
                2 -> CategoryScreen(categoryList, { isManagingCats = false }, { newCats -> categoryList = newCats })
                3 -> AddDishScreen(editingDish, categoryList, { isAdding = false; editingDish = null }, {
                    dishList = if (editingDish != null) dishList.map { d -> if (d.id == editingDish!!.id) it else d } else dishList + it
                    isAdding = false; editingDish = null
                })
                4 -> DishDetailScreen(viewingDish!!, { viewingDish = null }, { editingDish = it; viewingDish = null }, { dishList = dishList.filter { d -> d.id != viewingDish!!.id }; viewingDish = null })
                5 -> MainScreen(dishList, categoryList, selectedCat, searchQuery, { selectedCat = it }, { searchQuery = it }, { isAdding = true }, { isManagingCats = true }, { viewingDish = it }, { specDish = it })
            }
        }

        // 底部悬浮控制台
        if (viewingOrder == null && !isHistoryView && !isManagingCats && !isAdding && editingDish == null && viewingDish == null) {
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)) {
                CartBar(currentCart, {
                    val newOrder = Order(items = currentCart.toList(), timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    orderHistory = listOf(newOrder) + orderHistory
                    currentCart.clear()
                    viewingOrder = newOrder
                }, { currentCart.remove(it) })
            }
            
            FloatingActionButton(
                onClick = { isHistoryView = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp).shadow(12.dp, CircleShape),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            ) { Icon(Icons.Default.History, "历程") }
        }
    }
}

@Composable
fun MainScreen(
    dishes: List<Dish>, cats: List<String>, selected: String, query: String,
    onCat: (String) -> Unit, onSearch: (String) -> Unit, onAdd: () -> Unit, onManage: () -> Unit,
    onDish: (Dish) -> Unit, onSpec: (Dish) -> Unit
) {
    val filtered = dishes.filter { (it.category == selected || selected.isEmpty()) && it.name.contains(query, true) }
    var searchVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))) {
                TopAppBar(
                    title = { Text("吾家私房菜", fontWeight = FontWeight.Black, fontSize = 26.sp) },
                    actions = {
                        IconButton(onClick = { searchVisible = !searchVisible }) { Icon(Icons.Default.Search, null) }
                        IconButton(onClick = onAdd) { Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp)) }
                    }
                )
                AnimatedVisibility(visible = searchVisible) {
                    OutlinedTextField(
                        value = query, onValueChange = onSearch,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("搜寻今日美味...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        shape = CircleShape
                    )
                }
            }
        },
        containerColor = Color.Transparent
    ) { p ->
        Row(Modifier.padding(p).fillMaxSize()) {
            Column(Modifier.width(96.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).verticalScroll(rememberScrollState())) {
                cats.forEach { c ->
                    val isSel = c == selected
                    Box(Modifier.fillMaxWidth().clickable { onCat(c) }.background(if (isSel) MaterialTheme.colorScheme.surface else Color.Transparent).padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Text(c, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, color = if (isSel) MaterialTheme.colorScheme.primary else Color.Gray)
                    }
                }
                IconButton(onClick = onManage, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp)) { Icon(Icons.Default.Settings, null, tint = Color.LightGray) }
            }
            LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
                items(filtered) { d ->
                    DishCard(d, onDish, onSpec)
                }
            }
        }
    }
}

@Composable
fun DishCard(dish: Dish, onClick: (Dish) -> Unit, onSpec: (Dish) -> Unit) {
    Card(
        onClick = { onClick(dish) },
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(28.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = rememberAsyncImagePainter(dish.imageUri ?: Icons.Default.RestaurantMenu),
                contentDescription = null,
                modifier = Modifier.size(95.dp).clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(dish.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                if (!dish.mainIngredients.isNullOrBlank()) Text(dish.mainIngredients, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("¥${dish.price}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
            Button(
                onClick = { onSpec(dish) }, 
                shape = RoundedCornerShape(16.dp), 
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) { Text("定制", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun CartBar(items: List<OrderItem>, onOrder: () -> Unit, onRemove: (OrderItem) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (expanded && items.isNotEmpty()) {
            Card(
                modifier = Modifier.width(360.dp).padding(bottom = 12.dp).animateContentSize(), 
                shape = RoundedCornerShape(32.dp), 
                elevation = CardDefaults.cardElevation(20.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("今日待点 (${items.size})", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = { expanded = false }) { Icon(Icons.Default.Close, null) }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    LazyColumn(Modifier.heightIn(max = 260.dp)) {
                        items(items) { itm ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                // 购物车内的菜品缩略图
                                Image(
                                    painter = rememberAsyncImagePainter(itm.dishImage ?: Icons.Default.RestaurantMenu),
                                    contentDescription = null,
                                    modifier = Modifier.size(45.dp).clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(itm.dishName, fontWeight = FontWeight.Bold)
                                    Text("${itm.oilLevel.label} · ${itm.spicy.label}", fontSize = 11.sp, color = Color.Gray)
                                }
                                IconButton(onClick = { onRemove(itm) }) { Icon(Icons.Default.RemoveCircle, null, tint = Color.Red.copy(alpha = 0.6f)) }
                            }
                        }
                    }
                    Button(onClick = onOrder, modifier = Modifier.fillMaxWidth().height(54.dp).padding(top = 12.dp), shape = RoundedCornerShape(16.dp)) { 
                        Text("确认并下单", fontWeight = FontWeight.Bold, fontSize = 16.sp) 
                    }
                }
            }
        }
        Surface(
            modifier = Modifier.width(220.dp).height(64.dp).clickable { if (items.isNotEmpty()) expanded = !expanded }.shadow(16.dp, CircleShape),
            shape = CircleShape, color = if(items.isEmpty()) Color.LightGray else MaterialTheme.colorScheme.primary
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                BadgedBox(badge = { if(items.isNotEmpty()) Badge { Text("${items.size}") } }) {
                    Icon(Icons.Default.RestaurantMenu, null, tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Text(if(items.isEmpty()) "空空如也" else "查看今日菜单", fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
    }
}

@Composable
fun AddDishScreen(editing: Dish?, cats: List<String>, onBack: () -> Unit, onSave: (Dish) -> Unit) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var price by remember { mutableStateOf(editing?.price ?: "") }
    var cat by remember { mutableStateOf(editing?.category ?: (cats.firstOrNull() ?: "")) }
    var remark by remember { mutableStateOf(editing?.remark ?: "") }
    var ingredients by remember { mutableStateOf(editing?.mainIngredients ?: "") }
    var stepsText by remember { mutableStateOf(editing?.steps?.joinToString("\n") ?: "") }
    var imgUri by remember { mutableStateOf(editing?.imageUri ?: "") }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { imgUri = it.toString() }
    }
    
    // 核心修改：步骤管理
    var steps by remember { mutableStateOf(editing?.steps.orEmpty().ifEmpty { listOf("") }) }
    
    Scaffold(
        topBar = { TopAppBar(title = { Text(if(editing==null) "新增珍馐" else "优化菜品", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) },
        // 修复：将保存按钮固定在底部
        bottomBar = {
             Button(
                onClick = {
                    if (name.isNotBlank() && price.isNotBlank()) {
                        onSave(Dish(
                            id = editing?.id ?: System.currentTimeMillis(),
                            name = name, imageUri = imgUri.takeIf { it.isNotBlank() }, price = price,
                            firstMakeDate = editing?.firstMakeDate ?: LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            remark = remark, category = cat, mainIngredients = ingredients,
                            steps = steps.filter { it.isNotBlank() } // 过滤掉空步骤
                        ))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("保存菜谱", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        }
    ) { p ->
        // 主内容区，现在可以完全滚动
        Column(Modifier.padding(p).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // 图片上传区域
            Box(
                Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                contentAlignment = Alignment.Center
            ) {
                if (imgUri.isNotBlank()) {
                    Image(rememberAsyncImagePainter(imgUri), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.3f)), contentAlignment = Alignment.Center) {
                        Text("点击更换照片", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("上传本地照片", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }
                }
            }

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("菜名") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("估价 (¥)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            
            Text("所属分类", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cats) { c ->
                    FilterChip(selected = cat == c, onClick = { cat = c }, label = { Text(c) }, shape = CircleShape)
                }
            }
            
            OutlinedTextField(value = ingredients, onValueChange = { ingredients = it }, label = { Text("核心食材") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            OutlinedTextField(value = remark, onValueChange = { remark = it }, label = { Text("独门秘籍/备注") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            
            Text("烹饪秘籍 (每条一步)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            // 步骤列表管理区域
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 使用 animateContentSize 确保动态增删时的动画平滑
                steps.forEachIndexed { index, step ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().animateContentSize()) {
                        OutlinedTextField(
                            value = step,
                            onValueChange = { newStep ->
                                steps = steps.toMutableList().apply { this[index] = newStep }
                            },
                            label = { Text("步骤 ${index + 1}") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        if (steps.size > 1) {
                            IconButton(onClick = { steps = steps.filterIndexed { i, _ -> i != index } }) {
                                Icon(Icons.Default.RemoveCircle, null, tint = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
                
                Button(
                    onClick = { steps = steps + "" },
                    modifier = Modifier.align(Alignment.Start),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(4.dp))
                    Text("添加新步骤")
                }
            }
            // 底部留白，确保最后一个输入框不会被 bottomBar 遮挡
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun HistoryScreen(orders: List<Order>, onBack: () -> Unit, onOrderClick: (Order) -> Unit, onDeleteOrder: (Order) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("时光菜单", fontWeight = FontWeight.Black) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }) { p ->
        if (orders.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("还没有任何美食足迹...") }
        else LazyColumn(modifier = Modifier.padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(orders) { o ->
                Card(
                    onClick = { onOrderClick(o) }, 
                    shape = RoundedCornerShape(24.dp), 
                    modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            // 布局优化：通过 padding(end = 40.dp) 为右上角删除图标留出位置，解决重叠
                            Row(modifier = Modifier.fillMaxWidth().padding(end = 40.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(o.timestamp, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(if (o.isCompleted) "已品鉴" else "待处理", color = if(o.isCompleted) Color.Gray else MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(o.items.joinToString(" + ") { it.dishName }, maxLines = 1, color = Color.Gray, fontSize = 14.sp, overflow = TextOverflow.Ellipsis)
                            if (o.isCompleted) {
                                Row(Modifier.padding(top = 12.dp)) { repeat(5) { i -> Icon(Icons.Default.Star, null, tint = if(i < o.rating) Color(0xFFFFC107) else Color.LightGray, modifier = Modifier.size(20.dp)) } }
                            }
                        }
                        // 删除按钮位置微调
                        IconButton(onClick = { onDeleteOrder(o) }, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) { 
                            Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.4f), modifier = Modifier.size(22.dp)) 
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderDetailScreen(order: Order, onBack: () -> Unit, onReview: (List<String>, String?, Int) -> Unit, onDelete: () -> Unit) {
    var curIdx by remember { mutableStateOf(0) }
    var showReview by remember { mutableStateOf(false) }
    val item = order.items.getOrNull(curIdx)

    if (showReview) {
        ReviewDialog(order.reviewImages, order.reviewText, order.rating, { showReview = false }, { img, txt, rate -> onReview(img, txt, rate); showReview = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.dishName ?: "详情", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { p ->
        Column(Modifier.padding(p).padding(16.dp).verticalScroll(rememberScrollState())) {
            item?.let {
                Card(shape = RoundedCornerShape(28.dp)) {
                    Image(rememberAsyncImagePainter(it.dishImage ?: Icons.Default.RestaurantMenu), null, Modifier.fillMaxWidth().height(260.dp), contentScale = ContentScale.Crop)
                }
                Row(modifier = Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestionChip(onClick = {}, label = { Text(it.oilLevel.label) }, shape = CircleShape)
                    SuggestionChip(onClick = {}, label = { Text(it.spicy.label) }, shape = CircleShape)
                    it.dietaries.forEach { d -> SuggestionChip(onClick = {}, label = { Text(d.label) }, shape = CircleShape) }
                }
                Text("制作流程", fontWeight = FontWeight.Black, fontSize = 22.sp)
                it.steps.forEachIndexed { i, s ->
                    Row(Modifier.padding(vertical = 10.dp)) {
                        Text("${i+1}", modifier = Modifier.size(30.dp).background(MaterialTheme.colorScheme.primary, CircleShape).padding(top = 2.dp), color = Color.White, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(16.dp))
                        Text(s, fontSize = 18.sp, lineHeight = 24.sp)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { if(curIdx > 0) curIdx-- }, enabled = curIdx > 0) { Text("上一步") }
                if (item != null && curIdx < order.items.size - 1) Button(onClick = { curIdx++ }, shape = RoundedCornerShape(12.dp)) { Text("下一步") }
                else Button(onClick = { showReview = true }, shape = RoundedCornerShape(12.dp)) { Text(if(order.isCompleted) "修改评价" else "大功告成") }
            }
            if (order.isCompleted) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))
                Text("成品合影", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                LazyRow(modifier = Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(order.reviewImages) { img -> Image(rememberAsyncImagePainter(img), null, Modifier.size(130.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop) }
                }
                if(!order.reviewText.isNullOrBlank()) Text(order.reviewText!!, color = Color.DarkGray, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))
                Row { repeat(5) { i -> Icon(Icons.Default.Star, null, tint = if(i < order.rating) Color(0xFFFFC107) else Color.LightGray, modifier = Modifier.size(24.dp)) } }
                TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.End)) { Text("彻底删除这条足迹", color = Color.Red) }
            }
        }
    }
}

@Composable
fun ReviewDialog(
    reviewImages: List<String>,
    reviewText: String?,
    rating: Int,
    onDismiss: () -> Unit,
    onConfirm: (List<String>, String?, Int) -> Unit
) {
    var images by remember { mutableStateOf(reviewImages) }
    var text by remember { mutableStateOf(reviewText ?: "") }
    var currentRating by remember { mutableIntStateOf(rating) }

    // 修复闪退：使用 PickMultipleVisualMedia 处理图片选择
    val multiPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) {
            images = images + uris.map { it.toString() }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("评价我的作品", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row {
                    repeat(5) { i ->
                        IconButton(onClick = { currentRating = i + 1 }) {
                            Icon(Icons.Default.Star, null, tint = if(i < currentRating) Color(0xFFFFC107) else Color.LightGray, modifier = Modifier.size(36.dp))
                        }
                    }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("记录此刻的心情或经验...") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(16.dp)
                )
                
                Text("上传作品集", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    item {
                        Box(
                            Modifier.size(100.dp).border(1.dp, Color.LightGray, RoundedCornerShape(16.dp)).clickable {
                                multiPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }, 
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) }
                    }
                    items(images) { img ->
                        Box {
                            Image(
                                painter = rememberAsyncImagePainter(img), 
                                contentDescription = null, 
                                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(onClick = { images = images - img }, modifier = Modifier.align(Alignment.TopEnd).size(26.dp).background(Color.Black.copy(0.5f), CircleShape)) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(images, text.takeIf { it.isNotBlank() }, currentRating) },
                enabled = currentRating > 0,
                shape = RoundedCornerShape(16.dp)
            ) { Text("保存足迹", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        shape = RoundedCornerShape(32.dp)
    )
}

@Composable
fun CategoryScreen(cats: List<String>, onBack: () -> Unit, onUpdate: (List<String>) -> Unit) {
    var newCatName by remember { mutableStateOf("") }
    Scaffold(
        topBar = { TopAppBar(title = { Text("管理我的分类") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }
    ) { p ->
        Column(Modifier.padding(p).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newCatName, onValueChange = { newCatName = it }, label = { Text("输入新分类") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                Spacer(Modifier.width(12.dp))
                Button(onClick = { if(newCatName.isNotBlank()) { onUpdate(cats + newCatName); newCatName = "" } }, shape = RoundedCornerShape(16.dp)) { Text("添加") }
            }
            Spacer(Modifier.height(24.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(cats) { c ->
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(c, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            IconButton(onClick = { onUpdate(cats.filter { it != c }) }) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.6f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpecDialog(dish: Dish, onDismiss: () -> Unit, onConfirm: (OrderItem) -> Unit) {
    var oil by remember { mutableStateOf(OilLevel.NORMAL) }
    var spicy by remember { mutableStateOf(SpicyLevel.MILD) }
    val dietaries = remember { mutableStateListOf<DietaryRestriction>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("定制您的口味 - ${dish.name}", fontWeight = FontWeight.Black) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column {
                    Text("油量偏好", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OilLevel.entries.forEach { level ->
                            FilterChip(selected = oil == level, onClick = { oil = level }, label = { Text(level.label) }, shape = CircleShape)
                        }
                    }
                }
                Column {
                    Text("忌口/特殊要求", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DietaryRestriction.entries.forEach { restriction ->
                            FilterChip(
                                selected = if (restriction == DietaryRestriction.NONE) dietaries.isEmpty() else dietaries.contains(restriction),
                                onClick = {
                                    if (restriction == DietaryRestriction.NONE) dietaries.clear()
                                    else { if (dietaries.contains(restriction)) dietaries.remove(restriction) else dietaries.add(restriction) }
                                },
                                label = { Text(restriction.label) },
                                shape = CircleShape
                            )
                        }
                    }
                }
                Column {
                    Text("辣度调整", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpicyLevel.entries.forEach { level ->
                            FilterChip(selected = spicy == level, onClick = { spicy = level }, label = { Text(level.label) }, shape = CircleShape)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(OrderItem(dish.id, dish.name, dish.imageUri, oil, dietaries.toList(), spicy, dish.steps)) }, shape = RoundedCornerShape(16.dp)) { Text("确认选择") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        shape = RoundedCornerShape(32.dp)
    )
}

@Composable
fun DishDetailScreen(dish: Dish, onBack: () -> Unit, onEdit: (Dish) -> Unit, onDelete: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(dish.name, fontWeight = FontWeight.ExtraBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { onEdit(dish) }) { Icon(Icons.Default.Edit, null) }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                }
            )
        }
    ) { p ->
        Column(Modifier.padding(p).padding(16.dp).verticalScroll(rememberScrollState())) {
            Card(shape = RoundedCornerShape(32.dp), elevation = CardDefaults.cardElevation(6.dp)) {
                Image(
                    painter = rememberAsyncImagePainter(dish.imageUri ?: Icons.Default.RestaurantMenu),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("¥${dish.price}", fontSize = 34.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                SuggestionChip(onClick = {}, label = { Text(dish.category) }, shape = CircleShape)
            }
            if (!dish.mainIngredients.isNullOrBlank()) {
                Text("主要食材: ${dish.mainIngredients}", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp), fontSize = 16.sp)
            }
            Text("备注: ${dish.remark}", modifier = Modifier.padding(vertical = 8.dp), fontSize = 16.sp)
            Text("首次入谱: ${dish.firstMakeDate}", fontSize = 12.sp, color = Color.Gray)
            
            HorizontalDivider(Modifier.padding(vertical = 24.dp))
            Text("烹饪秘籍", fontWeight = FontWeight.Black, fontSize = 24.sp)
            dish.steps.forEachIndexed { i, s ->
                Row(Modifier.padding(vertical = 12.dp)) {
                    Text("${i+1}", modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary, CircleShape).padding(top = 2.dp), color = Color.White, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(16.dp))
                    Text(s, fontSize = 18.sp, lineHeight = 26.sp)
                }
            }
        }
    }
}

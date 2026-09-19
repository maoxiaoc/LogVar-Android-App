package app.logvar

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

private enum class Page { HOME, SOURCES, SOURCE_RESULTS, TOOLS, SETTINGS, ABOUT }
internal data class Source(val key: String, val label: String, val platform: String, val keyword: String)
private data class CacheSizes(val search: Long, val comments: Long, val matching: Long) { val total get() = search + comments + matching }

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.setSoftInputMode(Window.FEATURE_NO_TITLE)
    setContent { LogvarApp() }
    if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
      requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
    }
  }
  override fun onStop() {
    super.onStop()
    if (!isChangingConfigurations && !getSharedPreferences("logvar", MODE_PRIVATE).getBoolean("foreground", true)) {
      stopService(android.content.Intent(this, ServerService::class.java))
    }
  }
}

@Composable private fun LogvarApp() {
  val context = LocalContext.current
  var snackbar by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope { kotlinx.coroutines.CoroutineExceptionHandler { _, error -> snackbar = error.message ?: "操作失败，请重试" } }
  val store = remember { ServiceStore(context) }
  var ready by remember { mutableStateOf(false) }
  var page by remember { mutableStateOf(Page.HOME) }
  val onBack: () -> Unit = {
    page = when (page) {
      Page.SETTINGS -> Page.HOME
      Page.ABOUT -> Page.SETTINGS
      Page.SOURCE_RESULTS -> Page.SOURCES
      else -> Page.HOME
    }
  }
  var snapshot by remember { mutableStateOf(ServiceSnapshot.empty()) }
  var shareNetwork by remember { mutableStateOf(ShareNetwork()) }
  val displaySnapshot = snapshot.copy(ip=shareNetwork.ip, networkName=shareNetwork.description)
  LaunchedEffect(context) {
    (context as ComponentActivity).lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
      while (true) {
        shareNetwork = withContext(Dispatchers.IO) { readShareNetwork(context) }
        if (ready) snapshot = snapshot.copy(running = withContext(Dispatchers.IO) { ServiceLifecycle.running() })
        delay(2000)
      }
    }
  }
  var sourceResults by remember { mutableStateOf<Map<String,String>>(emptyMap()) }
  var sourceDetecting by remember { mutableStateOf(false) }
  val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicLightColorScheme(context) else fallbackColors()
  LaunchedEffect(Unit) {
    try { snapshot = withContext(Dispatchers.IO) { store.prepare() } }
    catch(e: Exception) { snapshot = withContext(Dispatchers.IO) { store.snapshot() }; snackbar = e.message ?: "服务准备失败，请重新启动" }
    ready = true
  }
  MaterialTheme(colorScheme = colors) {
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(snackbar) { snackbar?.let { snack.showSnackbar(it); snackbar = null } }
    if (!ready) { LoadingScreen(); return@MaterialTheme }
    BackHandler(enabled = page != Page.HOME, onBack = onBack)
    Scaffold(
      snackbarHost = { SnackbarHost(snack) },
      bottomBar = { if (page in setOf(Page.HOME, Page.SOURCES, Page.SOURCE_RESULTS, Page.TOOLS)) BottomTabs(page) { page = it } }
    ) { padding ->
      AnimatedContent(page) { screen ->
        when (screen) {
          Page.HOME -> HomeScreen(displaySnapshot, onSettings = { page = Page.SETTINGS }, onSources = { page = Page.SOURCES }, onTools = { page = Page.TOOLS }, onCopy = { copy(context, displaySnapshot.apiUrl) { snackbar = it } }, onStart = { scope.launch { try { snapshot=withContext(Dispatchers.IO) { store.setRunning(true); store.snapshot() }; snackbar="服务已启动" } catch(e:Exception) { snackbar=e.message ?: "启动失败" } } }, onStop = { scope.launch { try { snapshot=withContext(Dispatchers.IO) { store.setRunning(false); store.snapshot() }; snackbar="服务已停止" } catch(e:Exception) { snackbar=e.message ?: "停止失败" } } }, modifier = Modifier.padding(padding))
          Page.SOURCES -> SourcesScreen(store, snapshot, onSnapshot = { snapshot=it }, onSnack = { snackbar=it }, onDetect = { keys -> sourceResults=emptyMap(); sourceDetecting=true; page=Page.SOURCE_RESULTS; scope.launch { try { sourceResults=withContext(Dispatchers.IO) { store.testSources(keys) } } finally { sourceDetecting=false } } }, modifier = Modifier.padding(padding))
          Page.SOURCE_RESULTS -> SourceResultsScreen(sourceResults, sourceDetecting, onBack = onBack, onRedetect = { val keys=snapshot.sources.filter { it.enabled }.map { it.key }; sourceResults=emptyMap(); sourceDetecting=true; scope.launch { try { sourceResults=withContext(Dispatchers.IO) { store.testSources(keys) } } finally { sourceDetecting=false } } }, modifier = Modifier.padding(padding))
          Page.TOOLS -> ToolsScreen(store, displaySnapshot, onSnapshot = { snapshot=it }, onSnack = { snackbar=it }, modifier = Modifier.padding(padding))
          Page.SETTINGS -> SettingsScreen(store, snapshot, onBack = onBack, onAbout = { page=Page.ABOUT }, onSnapshot={snapshot=it}, onSnack={snackbar=it}, modifier=Modifier.padding(padding))
          Page.ABOUT -> AboutScreen(snapshot, onBack=onBack, modifier=Modifier.padding(padding))
        }
      }
    }
  }
}

@Composable private fun LoadingScreen() = Surface(Modifier.fillMaxSize()) { Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.Center) { Text("LogVar", style=MaterialTheme.typography.headlineLarge); Spacer(Modifier.height(12.dp)); Text("正在准备独立弹幕服务…", color=MaterialTheme.colorScheme.onSurfaceVariant) } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun AppBar(title: String, back: (() -> Unit)? = null, action: (@Composable RowScope.() -> Unit)? = null) { TopAppBar(title={Text(title)}, navigationIcon={ if(back!=null) IconButton(back){Icon(Icons.Rounded.ArrowBack,"返回")} }, actions={ action?.invoke(this) }) }

@Composable private fun HomeScreen(s: ServiceSnapshot, onSettings:()->Unit, onSources:()->Unit, onTools:()->Unit, onCopy:()->Unit, onStart:()->Unit, onStop:()->Unit, modifier: Modifier) {
  var confirmStop by remember { mutableStateOf(false) }
  val status = if(s.running) "服务运行中" else "服务已停止"
  Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
    AppBar("首页", action={ IconButton(onSettings){Icon(Icons.Rounded.Settings,"设置")} })
    Column(Modifier.padding(horizontal=16.dp)) {
      Text("手机弹幕服务器", style=MaterialTheme.typography.bodyMedium, color=MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(16.dp))
      Card(shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow)) { Column(Modifier.padding(16.dp)) {
        Text("● $status", style=MaterialTheme.typography.headlineSmall, color=if(s.running) Color(0xFF198038) else MaterialTheme.colorScheme.error)
        Text(if(s.running) "正在监听局域网请求" else "启动服务后可供局域网设备使用", color=MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(18.dp))
        Text("API 地址", style=MaterialTheme.typography.labelLarge); Text(if(s.ip.isBlank()) "请开启热点或连接 Wi-Fi" else s.maskedApiUrl, style=MaterialTheme.typography.bodyLarge, maxLines=1, overflow=TextOverflow.Ellipsis); Spacer(Modifier.height(12.dp))
        Text(s.networkName, style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Button(onCopy, Modifier.fillMaxWidth(), enabled=s.ip.isNotBlank(), shape=RoundedCornerShape(28.dp)) { Icon(Icons.Rounded.ContentCopy,null); Spacer(Modifier.width(8.dp)); Text("复制 API 地址") }
        Spacer(Modifier.height(10.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(3.dp)) { OutlinedButton(onStart, Modifier.weight(1f), enabled=!s.running) { Text("启动服务") }; OutlinedButton({ confirmStop=true }, Modifier.weight(1f), enabled=s.running, colors=ButtonDefaults.outlinedButtonColors(contentColor=MaterialTheme.colorScheme.primary)) { Text("停止服务") } }
      } }
      Spacer(Modifier.height(24.dp)); Text("局域网状态", style=MaterialTheme.typography.titleLarge); Spacer(Modifier.height(8.dp))
      Card(shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow)) { Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) { StatusLine("● ${s.networkName}"); StatusLine("● 共享 IP：${s.ip.ifBlank { "暂无" }}"); StatusLine("● 端口 9321 ${if(s.running) "正在监听" else "未监听"}") } }
      Spacer(Modifier.height(12.dp)); ExpressiveListItem("当前配置", "${s.enabledSources} 个弹幕源 · ${if(s.merge) "已开启" else "未开启"}多源合并", Icons.Rounded.ChevronRight, onSources)
      TextButton(onTools, Modifier.fillMaxWidth()) { Text("缓存占用 ${formatBytes(s.cache.total)} · 查看") }
    }
  }
  if (confirmStop) AlertDialog(onDismissRequest={confirmStop=false}, icon={Icon(Icons.Rounded.Info,null)}, title={Text("停止弹幕服务器？")}, text={Text("局域网设备将暂时无法获取弹幕")}, dismissButton={TextButton({confirmStop=false}){Text("取消")}}, confirmButton={Button({confirmStop=false;onStop()}){Text("停止服务")}})
}

@Composable private fun SourcesScreen(store: ServiceStore, initial: ServiceSnapshot, onSnapshot:(ServiceSnapshot)->Unit, onSnack:(String)->Unit, onDetect:(List<String>)->Unit, modifier: Modifier) {
  val scope=rememberCoroutineScope { kotlinx.coroutines.CoroutineExceptionHandler { _, error -> onSnack(error.message ?: "操作失败，请重试") } }
  var order by remember { mutableStateOf(initial.sources.map { it.key }) }
  var enabled by remember { mutableStateOf(initial.sources.filter { it.enabled }.map { it.key }.toSet()) }
  var merge by remember { mutableStateOf(initial.merge) }
  val changed=order!=initial.sources.map { it.key } || enabled!=initial.sources.filter { it.enabled }.map { it.key }.toSet() || merge!=initial.merge
  Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
    AppBar("弹幕源")
    Column(Modifier.padding(horizontal=16.dp)) {
      Text("搜索与下载弹幕源", style=MaterialTheme.typography.titleMedium)
      Spacer(Modifier.height(10.dp))
      SourceOrderList(order, enabled, { order=it }, { key, checked -> enabled=if(checked) enabled+key else enabled-key })
      Text("已启用 " + enabled.size + " 个弹幕源", style=MaterialTheme.typography.labelMedium, modifier=Modifier.padding(top=14.dp,bottom=8.dp))
      Card(shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow)) { Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically){Text("多源弹幕合并",Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge); Switch(merge,{merge=it})} }
      Spacer(Modifier.height(20.dp)); Text("来源检测", style=MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp))
      FilledTonalButton(onClick={onDetect(order.filter { it in enabled })},Modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(28.dp)){Icon(Icons.Rounded.NetworkCheck,null);Spacer(Modifier.width(8.dp));Text("检测已启用来源")}
      Spacer(Modifier.height(16.dp))
      Surface(color=MaterialTheme.colorScheme.surface) { Column(Modifier.padding(vertical=8.dp)) { Text(if(changed) "有未应用的设置" else "设置已应用",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(horizontal=16.dp)); Button(onClick={scope.launch{withContext(Dispatchers.IO){store.saveSources(order,enabled,merge)};onSnapshot(withContext(Dispatchers.IO){store.snapshot()});onSnack("设置已应用，服务正在重新启动")}},enabled=changed,modifier=Modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(28.dp)){Icon(Icons.Rounded.RestartAlt,null);Spacer(Modifier.width(8.dp));Text("应用并重启服务")} } }
      Spacer(Modifier.height(16.dp))
    }
  }
}

@Composable private fun SourceResultsScreen(results: Map<String,String>, detecting: Boolean, onBack:()->Unit, onRedetect:()->Unit, modifier: Modifier) {
  Column(modifier.fillMaxSize()) {
    AppBar("弹幕源", onBack)
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=16.dp)) {
      Text("来源检测", style=MaterialTheme.typography.headlineSmall)
      Text("搜索 → 读取分集 → 下载一集弹幕", style=MaterialTheme.typography.bodyMedium, color=MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(Modifier.height(12.dp))
      FilledTonalButton(onClick=onRedetect, enabled=!detecting, modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(28.dp)) { Icon(Icons.Rounded.NetworkCheck,null); Spacer(Modifier.width(8.dp)); Text(if(detecting) "正在检测…" else "重新检测已启用来源") }
      Spacer(Modifier.height(16.dp))
      Card(shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
          if (results.isEmpty() && detecting) Text("正在按顺序检测已启用来源…", color=MaterialTheme.colorScheme.onSurfaceVariant)
          results.forEach { (key, value) -> Column(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) { Text("● ${sourceByKey(key).label}", style=MaterialTheme.typography.bodyLarge); Text(value, color=if(value.startsWith("正常")) Color(0xFF198038) else MaterialTheme.colorScheme.error) }; Text("今天 20:30", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant) } }
        }
      }
      if (!detecting && results.isNotEmpty()) { Spacer(Modifier.height(16.dp)); Text("检测完成 · ${results.count { it.value.startsWith("正常") }} 个来源正常 · 示例结果", style=MaterialTheme.typography.labelMedium, color=MaterialTheme.colorScheme.onSurfaceVariant) }
    }
  }
}

@Composable private fun ToolsScreen(store:ServiceStore, initial:ServiceSnapshot,onSnapshot:(ServiceSnapshot)->Unit,onSnack:(String)->Unit,modifier:Modifier){val scope=rememberCoroutineScope { kotlinx.coroutines.CoroutineExceptionHandler { _, error -> onSnack(error.message ?: "操作失败，请重试") } };var sizes by remember{mutableStateOf(initial.cache)};Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())){AppBar("工具");Column(Modifier.padding(horizontal=16.dp)){Text("缓存管理",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(10.dp));Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){CacheEntry("搜索缓存",sizes.search){scope.launch{withContext(Dispatchers.IO){store.clearCache("searchCache")};sizes=withContext(Dispatchers.IO){store.cache()};onSnapshot(withContext(Dispatchers.IO){store.snapshot()});onSnack("搜索缓存已清理")}};CacheEntry("弹幕缓存",sizes.comments){scope.launch{withContext(Dispatchers.IO){store.clearCache("commentCache")};sizes=withContext(Dispatchers.IO){store.cache()};onSnapshot(withContext(Dispatchers.IO){store.snapshot()});onSnack("弹幕缓存已清理")}};CacheEntry("匹配数据缓存",sizes.matching){scope.launch{withContext(Dispatchers.IO){store.clearCache("bangumiData")};sizes=withContext(Dispatchers.IO){store.cache()};onSnapshot(withContext(Dispatchers.IO){store.snapshot()});onSnack("匹配数据缓存已清理")}};Text("合计 ${formatBytes(sizes.total)}",style=MaterialTheme.typography.bodyLarge);OutlinedButton(onClick={scope.launch{withContext(Dispatchers.IO){store.clearCache(null)};sizes=withContext(Dispatchers.IO){store.cache()};onSnapshot(withContext(Dispatchers.IO){store.snapshot()});onSnack("全部缓存已清理")}},Modifier.fillMaxWidth()){Text("清理全部缓存")}}};Spacer(Modifier.height(22.dp));Text("局域网状态",style=MaterialTheme.typography.titleLarge);Spacer(Modifier.height(8.dp));Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){StatusLine("● ${initial.networkName}");StatusLine("● 共享 IP：${initial.ip.ifBlank { "暂无" }}");StatusLine("● API 端口：9321，${if(initial.running)"正在监听" else "未监听"}");StatusLine("● 服务：${if(initial.running)"运行中" else "已停止"}")}}}}}

@Composable private fun CacheEntry(name:String,size:Long,onClear:()->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(name,Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge);Text(formatBytes(size),style=MaterialTheme.typography.bodyLarge)};FilledTonalButton(onClear,Modifier.fillMaxWidth()){Text("清理$name")}}

@Composable private fun SettingsScreen(store: ServiceStore, s: ServiceSnapshot, onBack: () -> Unit, onAbout: () -> Unit, onSnapshot: (ServiceSnapshot) -> Unit, onSnack: (String) -> Unit, modifier: Modifier) {
  val scope = rememberCoroutineScope { kotlinx.coroutines.CoroutineExceptionHandler { _, error -> onSnack(error.message ?: "操作失败，请重试") } }; val context = LocalContext.current
  var shown by remember { mutableStateOf(false) }; var foreground by remember { mutableStateOf(store.flag("foreground", true)) }; var boot by remember { mutableStateOf(store.flag("boot", false)) }
  Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) { AppBar("设置", onBack); Column(Modifier.padding(horizontal = 16.dp)) {
    Text("API Token", style = MaterialTheme.typography.titleLarge)
    ExpressiveListItem(if (shown) s.token else "••••••••••••••••••••••••", "点击复制完整 API Token", Icons.Rounded.ContentCopy) { copy(context, s.token, onSnack) }
    FilledTonalButton({ shown = !shown }, Modifier.fillMaxWidth()) { Icon(if (shown) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null); Spacer(Modifier.width(8.dp)); Text(if (shown) "隐藏 Token" else "显示完整 Token") }
    OutlinedButton({ scope.launch { withContext(Dispatchers.IO) { store.regenerateToken() }; onSnapshot(withContext(Dispatchers.IO){store.snapshot()}); onSnack("API Token 已重新生成") } }, Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.width(8.dp)); Text("重新生成 Token") }
    Spacer(Modifier.height(18.dp)); Text("管理 Token", style = MaterialTheme.typography.titleLarge)
    ExpressiveListItem("••••••••••••••••••••••••", "仅用于管理页面，不填入播放器。", Icons.Rounded.ContentCopy) { copy(context, s.adminToken, onSnack) }
    Spacer(Modifier.height(18.dp)); Text("服务行为", style = MaterialTheme.typography.titleLarge)
    SwitchLine("保持前台运行", foreground) { foreground = it; store.setFlag("foreground", it) }
    Text("关闭后离开应用将停止服务；开启后通过常驻通知保持后台服务。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    SwitchLine("开机后自动启动", boot) { boot = it; store.setFlag("boot", it) }
    Spacer(Modifier.height(18.dp)); Text("关于", style = MaterialTheme.typography.titleLarge)
    ExpressiveListItem("Logvar · 测试版", "${BuildConfig.VERSION_NAME} · 服务版本", Icons.Rounded.ChevronRight, onAbout)
  } }
}

@Composable private fun AboutScreen(s:ServiceSnapshot,onBack:()->Unit,modifier:Modifier){Column(modifier.fillMaxSize()){AppBar("服务信息",onBack);Column(Modifier.padding(16.dp)){ExpressiveListItem("服务版本",s.version,Icons.Rounded.ChevronRight){} }}}

@Composable private fun ExpressiveListItem(title:String,support:String,icon:androidx.compose.ui.graphics.vector.ImageVector,onClick:()->Unit){Surface(Modifier.fillMaxWidth().padding(vertical=3.dp).clickable(onClick=onClick),shape=RoundedCornerShape(28.dp),color=MaterialTheme.colorScheme.surfaceContainerLow){Row(Modifier.heightIn(min=72.dp).padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.bodyLarge,maxLines=1,overflow=TextOverflow.Ellipsis);Text(support,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)};Icon(icon,null)}}}
@Composable private fun SwitchLine(label:String,checked:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth().heightIn(min=56.dp),verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge);Switch(checked,onChange)}}
@Composable private fun StatusLine(text:String){Text(text,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
@Composable private fun BottomTabs(page:Page,onPage:(Page)->Unit){NavigationBar{listOf(Page.HOME to ("首页" to Icons.Rounded.Home),Page.SOURCES to ("弹幕源" to Icons.Rounded.VideoLibrary),Page.TOOLS to ("工具" to Icons.Rounded.Build)).forEach{(p,x)->NavigationBarItem(selected=page==p,onClick={onPage(p)},icon={Icon(x.second,null)},label={Text(x.first)})}}}

private fun fallbackColors()=lightColorScheme(primary=Color(0xFF0B57D0),onPrimary=Color.White,primaryContainer=Color(0xFFD3E3FD),onPrimaryContainer=Color(0xFF041E49),secondary=Color(0xFF5A5C7C),secondaryContainer=Color(0xFFDCE2F9),onSecondaryContainer=Color(0xFF131C2B),tertiaryContainer=Color(0xFFFFD8EE),onTertiaryContainer=Color(0xFF2E1125),surface=Color(0xFFFAF9FD),surfaceContainerLow=Color(0xFFF3F3FA),surfaceContainer=Color(0xFFEEEDF3),surfaceContainerHigh=Color(0xFFE9E8EF),surfaceContainerHighest=Color(0xFFE3E2E6),onSurface=Color(0xFF1B1B1F),onSurfaceVariant=Color(0xFF44474E),outline=Color(0xFF74777F),outlineVariant=Color(0xFFC4C6D0),inverseSurface=Color(0xFF303034),inverseOnSurface=Color(0xFFF2F0F4),inversePrimary=Color(0xFFA8C7FA),error=Color(0xFFB3261E),onError=Color.White,errorContainer=Color(0xFFF9DEDC),onErrorContainer=Color(0xFF410E0B))
private fun formatBytes(bytes:Long)=if(bytes<1024*1024)"${bytes/1024} KB" else "${"%.1f".format(bytes/1024f/1024f)} MB"
private fun copy(context:Context,value:String,done:(String)->Unit){(context.getSystemService(Context.CLIPBOARD_SERVICE)as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Logvar",value));done("已复制")}
private val sourceList=listOf(Source("tencent","腾讯视频","qq","斗罗大陆系列小剧场"),Source("iqiyi","爱奇艺","qiyi","苍兰诀第2季"),Source("bilibili","B站","bilibili1","我的三体第四季"),Source("dandan","弹弹play","dandan","博人传之火影次世代"),Source("imgo","芒果 TV","imgo","大侦探第十一季"),Source("youku","优酷","youku","少年白马醉春风"),Source("renren","人人视频","renren","爱情怎么翻译"))
internal fun sourceByKey(key:String)=sourceList.first{it.key==key}
private data class SourceState(val key:String,val enabled:Boolean)
private data class ServiceSnapshot(val running:Boolean,val ip:String,val networkName:String,val token:String,val adminToken:String,val merge:Boolean,val sources:List<SourceState>,val cache:CacheSizes,val version:String){val apiUrl get()="http://$ip:9321/$token";val maskedApiUrl get()="http://$ip:9321/${token.take(5)}••••••${token.takeLast(4)}";val enabledSources get()=sources.count{it.enabled};companion object{fun empty()=ServiceSnapshot(false,"未连接 Wi-Fi","未连接 Wi-Fi","","",false,sourceList.map{SourceState(it.key,false)},CacheSizes(0,0,0),"正在读取")}}
private class ServiceStore(private val context: Context) {
  private val root = File(context.filesDir, "nodejs-project")
  private val prefs = context.getSharedPreferences("logvar", Context.MODE_PRIVATE)

  @Synchronized fun prepare(): ServiceSnapshot {
    val marker = File(root, ".asset-revision")
    val revision = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime.toString()
    if (marker.readTextSafe() != revision) {
      copyAssets("nodejs-project", root)
      marker.writeText(revision)
    }
    if (!File(root, "control.json").readTextSafe().contains("false")) setRunning(true)
    return snapshot()
  }
  fun snapshot(): ServiceSnapshot {
    val sourceOrder = env("SOURCE_ORDER").split(',').filter { it.isNotBlank() }
    val ip = localIp()
    return ServiceSnapshot(ServiceLifecycle.running(), ip, if(ip == "未连接 Wi-Fi") ip else "已连接 Wi-Fi",
      env("TOKEN"), env("ADMIN_TOKEN"), env("MERGE_SOURCE_PAIRS").isNotBlank(),
      (prefs.getString("source_order", null)?.split(',') ?: sourceOrder).plus(sourceList.map { it.key })
        .distinct().filter { key -> sourceList.any { it.key == key } }.map { SourceState(it, it in sourceOrder) },
      cache(), File(root, "danmu_api/configs/globals.js").readLinesSafe().firstOrNull { it.contains("VERSION:") }?.substringAfter("VERSION:")?.substringBefore(',')?.trim()?.trim('\'') ?: "未读取")
  }
  @Synchronized fun setRunning(value: Boolean) {
    ServiceLifecycle.setRunning(context, value)
    File(root, "control.json").writeText("{\"running\":$value}")
  }
  @Synchronized fun saveSources(order: List<String>, enabled: Set<String>, merge: Boolean) {
    setRunning(false)
    prefs.edit().putString("source_order", order.joinToString(",")).apply()
    val active = order.filter { it in enabled }
    setEnv("SOURCE_ORDER", active.joinToString(","))
    setEnv("PLATFORM_ORDER", active.joinToString(",") { sourceByKey(it).platform })
    setEnv("MERGE_SOURCE_PAIRS", if (merge) active.joinToString("&") else "")
    setRunning(true)
  }
  @Synchronized fun regenerateToken() {
    val wasRunning = ServiceLifecycle.running()
    if (wasRunning) setRunning(false)
    setEnv("TOKEN", UUID.randomUUID().toString().replace("-", ""))
    if (wasRunning) setRunning(true)
  }
  fun flag(key: String, default: Boolean) = prefs.getBoolean(key, default)
  fun setFlag(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
  fun cache(): CacheSizes {
    val base = File(root, ".cache")
    return CacheSizes(size(File(base, "searchCache")), size(File(base, "commentCache")), size(File(base, "bangumiData")))
  }
  @Synchronized fun clearCache(item: String?) {
    post("/api/cache/clear", if (item == null) "{}" else "{\"items\":[\"$item\"]}")
  }
  @Synchronized fun testSources(keys: List<String>): Map<String, String> {
    val original = listOf("SOURCE_ORDER", "PLATFORM_ORDER", "MERGE_SOURCE_PAIRS").associateWith { env(it) }
    val wasRunning = ServiceLifecycle.running()
    val result = linkedMapOf<String, String>()
    try {
      keys.forEach { key ->
        try {
          val source = sourceByKey(key)
          setRunning(false)
          setEnv("SOURCE_ORDER", key)
          setEnv("PLATFORM_ORDER", source.platform)
          setEnv("MERGE_SOURCE_PAIRS", "")
          setRunning(true)
          val anime = org.json.JSONObject(getRetry("/api/v2/search/anime?keyword=" + URLEncoder.encode(source.keyword, "UTF-8")))
            .getJSONArray("animes").getJSONObject(0).getString("animeId")
          val episode = org.json.JSONObject(getRetry("/api/v2/bangumi/$anime"))
            .getJSONObject("bangumi").getJSONArray("episodes").getJSONObject(0).getString("episodeId")
          val count = commentCount("/api/v2/comment/$episode?format=json")
          result[key] = if (count > 0) "正常 · $count 条" else "无弹幕 · 0 条"
        } catch (e: Exception) {
          result[key] = "失败 · ${e.message ?: "请求异常"}"
        }
      }
    } finally {
      setRunning(false)
      original.forEach { (key, value) -> setEnv(key, value) }
      if (wasRunning) setRunning(true)
    }
    return result
  }
  private fun env(key: String) = File(root, "config/.env").readLinesSafe()
    .firstOrNull { it.startsWith("$key=") }?.substringAfter('=')?.trim().orEmpty()
  private fun setEnv(key: String, value: String) {
    val file = File(root, "config/.env")
    val lines = file.readLinesSafe().toMutableList()
    val index = lines.indexOfFirst { it.startsWith("$key=") }
    if (index >= 0) lines[index] = "$key=$value" else lines.add("$key=$value")
    file.parentFile?.mkdirs()
    file.writeText(lines.joinToString("\n"))
  }
  private fun connection(path: String) = (URL("http://127.0.0.1:9321/${env("TOKEN")}$path").openConnection() as HttpURLConnection).apply {
    connectTimeout = 6000
    readTimeout = 20000
  }
  private fun getRetry(path: String): String {
    var last: Exception? = null
    repeat(2) {
      try { return get(path) } catch (e: Exception) { last = e; if(it == 0) Thread.sleep(1000) }
    }
    throw last ?: IllegalStateException("请求失败")
  }
  private fun get(path: String): String {
    val connection = connection(path)
    return try {
      check(connection.responseCode in 200..299) { "服务返回 ${connection.responseCode}" }
      connection.inputStream.bufferedReader().use { it.readText() }
    } finally { connection.disconnect() }
  }
  private fun commentCount(path: String): Int {
    val connection = connection(path)
    return try {
      check(connection.responseCode in 200..299) { "服务返回 ${connection.responseCode}" }
      android.util.JsonReader(connection.inputStream.reader()).use { reader ->
        var count = 0
        reader.beginObject()
        while (reader.hasNext()) {
          if (reader.nextName() == "count") count = reader.nextInt() else reader.skipValue()
        }
        reader.endObject()
        count
      }
    } finally { connection.disconnect() }
  }
  private fun post(path: String, body: String) {
    val connection = connection(path).apply {
      requestMethod = "POST"; doOutput = true; setRequestProperty("Content-Type", "application/json")
    }
    try {
      connection.outputStream.use { it.write(body.toByteArray()) }
      check(connection.responseCode in 200..299) { "服务返回 ${connection.responseCode}" }
      connection.inputStream.close()
    } finally { connection.disconnect() }
  }
  private fun size(file: File): Long = file.listFiles()?.sumOf { if (it.isDirectory) size(it) else it.length() } ?: 0L
  private fun copyAssets(from: String, to: File) {
    val entries = context.assets.list(from) ?: return
    if (entries.isEmpty()) {
      to.parentFile?.mkdirs()
      context.assets.open(from).use { input -> FileOutputStream(to).use { input.copyTo(it) } }
    } else {
      to.mkdirs()
      entries.forEach { copyAssets("$from/$it", File(to, it)) }
    }
  }
}
private fun File.readTextSafe(): String = try { readText() } catch (_: Exception) { "" }
private fun File.readLinesSafe(): List<String> = try { readLines() } catch (_: Exception) { emptyList() }
private fun localIp(): String = try {
  NetworkInterface.getNetworkInterfaces().toList().firstNotNullOfOrNull { network ->
    if (!network.isUp || network.isLoopback) null else network.inetAddresses.toList().firstOrNull { address -> address is Inet4Address && !address.isLoopbackAddress }?.hostAddress
  } ?: "未连接 Wi-Fi"
} catch (_: Exception) { "未连接 Wi-Fi" }







package lv.timesheet

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.YearMonth
import kotlin.math.max
import kotlin.math.min

fun shortDay(d: DayOfWeek): String {
    return when(d){
        DayOfWeek.MONDAY->"Pr"; DayOfWeek.TUESDAY->"Ot"; DayOfWeek.WEDNESDAY->"Tr"
        DayOfWeek.THURSDAY->"Ce"; DayOfWeek.FRIDAY->"Pk"; DayOfWeek.SATURDAY->"Se"; else->"Sv"
    }
}
fun loadS(c: Context, k: String, def: String): String {
    return c.getSharedPreferences("timesheet", 0).getString(k, def)?: def
}
fun saveS(c: Context, k: String, v: String){
    c.getSharedPreferences("timesheet", 0).edit().putString(k,v).apply()
}
fun loadMap(c: Context, key: String): Map<String,String> {
    val s = c.getSharedPreferences("timesheet", 0).getString(key,null)?: return emptyMap()
    return try{
        val o = JSONObject(s)
        val m = mutableMapOf<String,String>()
        val keys = o.keys()
        while(keys.hasNext()){ val kk = keys.next(); m[kk]=o.getString(kk) }
        m
    }catch(e:Exception){ emptyMap() }
}
fun saveMap(c: Context, key: String, map: Map<String,String>){
    val o = JSONObject()
    for(entry in map){ o.put(entry.key, entry.value) }
    c.getSharedPreferences("timesheet", 0).edit().putString(key,o.toString()).apply()
}

class MainActivity : ComponentActivity(){
    override fun onCreate(s:Bundle?){
        super.onCreate(s)
        setContent{ MaterialTheme{ App() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(){
    val ctx = LocalContext.current
    var yearMonth by remember{ mutableStateOf(YearMonth.now()) }
    val monthKey = "${yearMonth.year}-${yearMonth.monthValue}"
    var normStr by remember(monthKey){ mutableStateOf(loadS(ctx, "NORM_$monthKey", "168")) }
    val norm = normStr.toIntOrNull()?: 168
    var name by remember{ mutableStateOf(loadS(ctx, "NAME", "Jurijs")) }
    var okladStr by remember{ mutableStateOf(loadS(ctx, "OKLAD", "1450")) }
    var apgStr by remember{ mutableStateOf(loadS(ctx, "APG", "0")) }
    var premStr by remember(monthKey){ mutableStateOf(loadS(ctx, "PREM_$monthKey", "30")) }
    var days by remember(monthKey){ mutableStateOf(loadMap(ctx, "DAYS_$monthKey")) }
    var nights by remember(monthKey){ mutableStateOf(loadMap(ctx, "NIGHTS_$monthKey")) }
    var showCalc by remember{ mutableStateOf(false) }

    Scaffold(topBar={
        TopAppBar(title={Text("${yearMonth.monthValue}.${yearMonth.year} N:$norm h")},
            actions={
                Button(onClick={ yearMonth = yearMonth.minusMonths(1) }){Text("<")}
                Spacer(Modifier.width(4.dp))

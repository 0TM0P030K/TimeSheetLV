package lv.timesheet

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
fun loadNorm(c: Context, k: String): String { return c.getSharedPreferences("timesheet", Context.MODE_PRIVATE).getString("NORM_" + k,"168")?: "168" }
fun saveNorm(c: Context, k: String, v: String){ c.getSharedPreferences("timesheet", Context.MODE_PRIVATE).edit().putString("NORM_" + k,v).apply() }
fun loadName(c: Context): String { return c.getSharedPreferences("timesheet", Context.MODE_PRIVATE).getString("NAME","Jurijs")?: "Jurijs" }
fun saveName(c: Context, v: String){ c.getSharedPreferences("timesheet", Context.MODE_PRIVATE).edit().putString("NAME",v).apply() }
fun loadOklad(c: Context): String { return c.getSharedPreferences("timesheet", Context.MODE_PRIVATE).getString("OKLAD","")?: "" }
fun saveOklad(c: Context, v: String){ c.getSharedPreferences("timesheet", Context.MODE_PRIVATE).edit().putString("OKLAD",v).apply() }
fun loadApg(c: Context): String { return c.getSharedPreferences("timesheet", Context.MODE_PRIVATE).getString("APG","0")?: "0" }
fun saveApg(c: Context, v: String){ c.getSharedPreferences("timesheet", Context.MODE_PRIVATE).edit().putString("APG",v).apply() }
fun loadPrem(c: Context, k: String): String { return c.getSharedPreferences("timesheet", Context.MODE_PRIVATE).getString("PREM_" + k,"")?: "" }
fun savePrem(c: Context, k: String, v: String){ c.getSharedPreferences("timesheet", Context.MODE_PRIVATE).edit().putString("PREM_" + k,v).apply() }
fun loadMap(c: Context, key: String): Map<String,String> {
    val s = c.getSharedPreferences("timesheet", Context.MODE_PRIVATE).getString(key,null)?: return emptyMap()
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
    c.getSharedPreferences("timesheet", Context.MODE_PRIVATE).edit().putString(key,o.toString()).apply()
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
    val monthKey = yearMonth.year.toString() + "-" + yearMonth.monthValue.toString()
    var normStr by remember(monthKey){ mutableStateOf(loadNorm(ctx, monthKey)) }
    val norm = normStr.toIntOrNull()?: 168
    var name by remember{ mutableStateOf(loadName(ctx)) }
    var okladStr by remember{ mutableStateOf(loadOklad(ctx)) }
    var apgStr by remember{ mutableStateOf(loadApg(ctx)) }
    var premStr by remember(monthKey){ mutableStateOf(loadPrem(ctx, monthKey)) }
    var days by remember(monthKey){ mutableStateOf(loadMap(ctx, "DAYS_" + monthKey)) }
    var nights by remember(monthKey){ mutableStateOf(loadMap(ctx, "NIGHTS_" + monthKey)) }
    var showCalc by remember{ mutableStateOf(false) }

    Scaffold(topBar={
        TopAppBar(title={Text(yearMonth.monthValue.toString() + "." + yearMonth.year.toString() + " N:" + norm.toString() + " h")},
            actions={
                Button(onClick={ yearMonth = yearMonth.minusMonths(1) }){Text("<")}
                Spacer(Modifier.width(4.dp))
                Button(onClick={ yearMonth = yearMonth.plusMonths(1) }){Text(">")}
            }
        )
    }){ pad->
        Column(Modifier.fillMaxSize().padding(pad).padding(8.dp).verticalScroll(rememberScrollState())){
            OutlinedTextField(value=name, onValueChange={name=it; saveName(ctx,it)}, label={Text("Vards Uzvards")}, modifier=Modifier.fillMaxWidth())
            Row{
                OutlinedTextField(value=okladStr, onValueChange={okladStr=it; saveOklad(ctx,it)}, label={Text("Oklads EUR")}, modifier=Modifier.weight(1f), keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
                Spacer(Modifier.width(4.dp))
                OutlinedTextField(value=apgStr, onValueChange={apgStr=it; saveApg(ctx,it)}, label={Text("Apg.")}, modifier=Modifier.width(90.dp), keyboardOptions=KeyboardOptions(keyboardType

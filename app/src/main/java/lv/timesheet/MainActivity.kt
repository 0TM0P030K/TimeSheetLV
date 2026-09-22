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
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.YearMonth
import kotlin.math.max

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
    var normStr by remember(monthKey){ mutableStateOf(loadNorm(ctx, monthKey)) }
    val norm = normStr.toIntOrNull() ?: 168
    var name by remember{ mutableStateOf(loadName(ctx)) }
    var okladStr by remember{ mutableStateOf(loadOklad(ctx)) }
    var apgStr by remember{ mutableStateOf(loadApg(ctx)) }
    var premStr by remember(monthKey){ mutableStateOf(loadPrem(ctx, monthKey)) }
    var days by remember(monthKey){ mutableStateOf(loadMap(ctx, "DAYS_$monthKey")) }
    var nights by remember(monthKey){ mutableStateOf(loadMap(ctx, "NIGHTS_$monthKey")) }
    var showCalc by remember{ mutableStateOf(false) }

    Scaffold(topBar={
        TopAppBar(title={Text("${yearMonth.monthValue}.${yearMonth.year} N:$norm h")},
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
                OutlinedTextField(value=apgStr, onValueChange={apgStr=it; saveApg(ctx,it)}, label={Text("Apg.")}, modifier=Modifier.width(90.dp), keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
            }
            Row{
                OutlinedTextField(value=normStr, onValueChange={normStr=it; saveNorm(ctx,monthKey,it)}, label={Text("Norma h")}, modifier=Modifier.weight(1f), keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
                Spacer(Modifier.width(4.dp))
                OutlinedTextField(value=premStr, onValueChange={premStr=it; savePrem(ctx,monthKey,it)}, label={Text("Premija %")}, modifier=Modifier.weight(1f), keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().background(Color(0xFFEEEEEE)).padding(6.dp)){
                Text("Diena",Modifier.width(60.dp), fontWeight=FontWeight.Bold)
                Text("Diena",Modifier.width(80.dp), fontWeight=FontWeight.Bold)
                Text("Nakti",Modifier.width(80.dp), fontWeight=FontWeight.Bold)
            }
            for(d in 1..yearMonth.lengthOfMonth()){
                val dow = yearMonth.atDay(d).dayOfWeek
                val isWeekend = dow==DayOfWeek.SATURDAY || dow==DayOfWeek.SUNDAY
                val bg = if(isWeekend) Color(0xFFFFEBEE) else Color.White
                val dayVal = days[d.toString()] ?: ""
                val nightVal = nights[d.toString()] ?: ""
                Row(Modifier.fillMaxWidth().background(bg).padding(vertical=2.dp)){
                    Text("$d ${short(dow)}", Modifier.width(60.dp), fontWeight=FontWeight.Bold)
                    OutlinedTextField(
                        value=dayVal,
                        onValueChange={v-> val m=days.toMutableMap(); m[d.toString()]=v; days=m; saveMap(ctx,"DAYS_$monthKey",m)},
                        modifier=Modifier.width(75.dp).padding(end=4.dp),
                        singleLine=true,
                        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value=nightVal,
                        onValueChange={v-> val m=nights.toMutableMap(); m[d.toString()]=v; nights=m; saveMap(ctx,"NIGHTS_$monthKey",m)},
                        modifier=Modifier.width(75.dp),
                        singleLine=true,
                        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number)
                    )
                }
            }
            val totalD = days.values.mapNotNull{it.toDoubleOrNull()}.sum()
            val totalN = nights.values.mapNotNull{it.toDoubleOrNull()}.sum()
            val virs = max(0.0, totalD - norm)
            Card(Modifier.fillMaxWidth().padding(top=8.dp), colors=CardDefaults.cardColors(containerColor=Color(0xFFE8F5E9))){
                Column(Modifier.padding(8.dp)){
                    Text("Dienas: $totalD / Norma $norm", fontWeight=FontWeight.Bold)

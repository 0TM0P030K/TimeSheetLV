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
fun loadS(c: Context, k: String, def: String): String { return c.getSharedPreferences("timesheet", 0).getString(k, def)?: def }
fun saveS(c: Context, k: String, v: String){ c.getSharedPreferences("timesheet", 0).edit().putString(k,v).apply() }
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
    var okladStr by remember{ mutableStateOf(loadS(ctx, "OKLAD", "")) }
    var apgStr by remember{ mutableStateOf(loadS(ctx, "APG", "0")) }
    var premStr by remember(monthKey){ mutableStateOf(loadS(ctx, "PREM_$monthKey", "")) }
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
            OutlinedTextField(value=name, onValueChange={name=it; saveS(ctx,"NAME",it)}, label={Text("Vards Uzvards")}, modifier=Modifier.fillMaxWidth())
            Row{
                OutlinedTextField(value=okladStr, onValueChange={okladStr=it; saveS(ctx,"OKLAD",it)}, label={Text("Oklads EUR")}, modifier=Modifier.weight(1f))
                Spacer(Modifier.width(4.dp))
                OutlinedTextField(value=apgStr, onValueChange={apgStr=it; saveS(ctx,"APG",it)}, label={Text("Apg.")}, modifier=Modifier.width(90.dp))
            }
            Row{
                OutlinedTextField(value=normStr, onValueChange={normStr=it; saveS(ctx,"NORM_$monthKey",it)}, label={Text("Norma h")}, modifier=Modifier.weight(1f))
                Spacer(Modifier.width(4.dp))
                OutlinedTextField(value=premStr, onValueChange={premStr=it; saveS(ctx,"PREM_$monthKey",it)}, label={Text("Premija %")}, modifier=Modifier.weight(1f))
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
                val dayVal = days[d.toString()]?: ""
                val nightVal = nights[d.toString()]?: ""
                Row(Modifier.fillMaxWidth().background(bg).padding(vertical=2.dp)){
                    Text("$d ${shortDay(dow)}", Modifier.width(60.dp), fontWeight=FontWeight.Bold)
                    OutlinedTextField(value=dayVal, onValueChange={v-> val m=days.toMutableMap(); m[d.toString()]=v; days=m; saveMap(ctx,"DAYS_$monthKey",m)}, modifier=Modifier.width(75.dp).padding(end=4.dp), singleLine=true)
                    OutlinedTextField(value=nightVal, onValueChange={v-> val m=nights.toMutableMap(); m[d.toString()]=v; nights=m; saveMap(ctx,"NIGHTS_$monthKey",m)}, modifier=Modifier.width(75.dp), singleLine=true)
                }
            }
            val totalD = days.values.mapNotNull{ it.toDoubleOrNull() }.sum()
            val totalN = nights.values.mapNotNull{ it.toDoubleOrNull() }.sum()
            val virs = max(0.0, totalD - norm)
            val dienasLidzNormai = min(totalD, norm.toDouble())
            Card(Modifier.fillMaxWidth().padding(top=8.dp), colors=CardDefaults.cardColors(containerColor=Color(0xFFE8F5E9))){
                Column(Modifier.padding(8.dp)){
                    Text("Stundas: $totalD / Norma $norm", fontWeight=FontWeight.Bold)
                    if(virs>0) Text("VIRSSTUNDAS: $virs h", color=Color.Red, fontWeight=FontWeight.Bold)
                    Text("Nakts: $totalN h")
                }
            }
            Button(onClick={showCalc=true}, Modifier.fillMaxWidth().padding(top=8.dp)){Text("APREKINAT ALGU")}
            if(showCalc){
                val oklad = okladStr.toDoubleOrNull()?:0.0
                val likme = if(norm>0) oklad/norm else 0.0
                val apg = apgStr.toIntOrNull()?:0
                val baseP = dienasLidzNormai * likme
                val virsP = virs * likme * 2.0
                val naktsP = totalN * likme * 0.5
                val pPerc = premStr.toDoubleOrNull()?:0.0
                val premijaNorma = baseP * pPerc / 100.0
                val premijaVirs = virs * likme * pPerc / 100.0
                val kopaBruto = baseP + virsP + naktsP + premijaNorma + premijaVirs
                val vsaoi = kopaBruto * 0.105
                val atvApg = apg * 250.0
                val apliekamais = max(0.0, kopaBruto - vsaoi - atvApg)
                val iin = apliekamais * 0.255
                val neto = kopaBruto - vsaoi - iin
                Dialog(onDismissRequest={showCalc=false}){
                    Card(Modifier.fillMaxWidth().padding(16.dp)){
                        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())){
                            Text("Aprekins", fontWeight=FontWeight.Bold)
                            Text("Likme: %.2f EUR/h".format(likme))
                            Text("Dienas: %.2f = %.2f EUR".format(dienasLidzNormai, baseP))
                            Text("Virs x2: %.2f = %.2f EUR".format(virs, virsP))
                            Text("Nakts: %.2f = %.2f EUR".format(totalN, naktsP))
                            Text("Premija norma = %.2f".format(premijaNorma))
                            Text("Premija virs = %.2f".format(premijaVirs))
                            HorizontalDivider(Modifier.padding(vertical=8.dp))
                            Text("BRUTO: %.2f EUR".format(kopaBruto), fontWeight=FontWeight.Bold)
                            Text("VSAOI 10.5: -%.2f".format(vsaoi))
                            Text("IIN 25.5: -%.2f".format(iin))
                            HorizontalDivider(Modifier.padding(vertical=8.dp))
                            Text("NETO: %.2f EUR".format(neto), fontWeight=FontWeight.Bold, color=Color(0xFF2E7D32))
                            Button(onClick={showCalc=false}, Modifier.fillMaxWidth()){Text("Aizvert")}
                        }
                    }
                }
            }
        }
    }
}

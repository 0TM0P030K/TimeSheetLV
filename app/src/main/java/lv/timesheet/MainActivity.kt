package lv.timesheet

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
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
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.max
import kotlin.math.min

class DayHours(d: String = "", n: String = "") {
    var day by mutableStateOf(d)
    var night by mutableStateOf(n)
}
class MonthBonus(var p: String = "")
class Employee(val id: Int, initialName: String) {
    var name by mutableStateOf(initialName)
    var monthlySalary by mutableStateOf("")
    var dependents by mutableStateOf("0")
    val months = mutableStateMapOf<String, MutableMap<Int, DayHours>>()
    val bonuses = mutableStateMapOf<String, MonthBonus>()
    fun getMonth(key: String) = months.getOrPut(key){ mutableStateMapOf() }
    fun getBonus(key: String) = bonuses.getOrPut(key){ MonthBonus() }
}
fun lvShort(dow: DayOfWeek) = when(dow){
    DayOfWeek.MONDAY -> "Pr"; DayOfWeek.TUESDAY -> "Ot"; DayOfWeek.WEDNESDAY -> "Tr"
    DayOfWeek.THURSDAY -> "Ce"; DayOfWeek.FRIDAY -> "Pk"; DayOfWeek.SATURDAY -> "Se"; DayOfWeek.SUNDAY -> "Sv"
}
fun saveAll(ctx: Context, employees: List<Employee>, norms: Map<String,String>, holidays: Map<String,String>){
    val prefs = ctx.getSharedPreferences("timesheet", Context.MODE_PRIVATE)
    val root = JSONObject()
    employees.forEach { emp ->
        val e = JSONObject()
        e.put("name", emp.name); e.put("salary", emp.monthlySalary); e.put("dependents", emp.dependents)
        val monthsObj = JSONObject()
        emp.months.forEach { (mk, map) ->
            val daysObj = JSONObject()
            map.forEach { (num,h)-> val o=JSONObject(); o.put("d",h.day); o.put("n",h.night); daysObj.put(num.toString(),o) }
            monthsObj.put(mk, daysObj)
        }
        e.put("months", monthsObj)
        val bonObj = JSONObject()
        emp.bonuses.forEach { (mk,b)-> val o=JSONObject(); o.put("p",b.p); bonObj.put(mk,o) }
        e.put("bonuses", bonObj)
        root.put(emp.id.toString(), e)
    }
    prefs.edit().putString("DATA_V3", root.toString()).apply()
    val nObj = JSONObject(); norms.forEach{ (k,v)-> nObj.put(k,v) }; prefs.edit().putString("NORMS", nObj.toString()).apply()
    val hObj = JSONObject(); holidays.forEach{ (k,v)-> hObj.put(k,v) }; prefs.edit().putString("HOLIDAYS", hObj.toString()).apply()
}
fun loadAll(ctx: Context): Triple<MutableList<Employee>, MutableMap<String,String>, MutableMap<String,String>> {
    val prefs = ctx.getSharedPreferences("timesheet", Context.MODE_PRIVATE)
    val norms = mutableStateMapOf<String,String>()
    val holidays = mutableStateMapOf<String,String>()
    try{ val s=prefs.getString("NORMS",null); if(s!=null){ val o=JSONObject(s); val keys=o.keys(); while(keys.hasNext()){ val k=keys.next(); norms[k]=o.getString(k) } } }catch(_:Exception){}
    try{ val s=prefs.getString("HOLIDAYS",null); if(s!=null){ val o=JSONObject(s); val keys=o.keys(); while(keys.hasNext()){ val k=keys.next(); holidays[k]=o.getString(k) } }
        if(holidays.isEmpty()) holidays.putAll(mapOf("2026-01-01" to "Jaunais gads","2026-05-01" to "Darba svetki","2026-06-23" to "Ligo","2026-06-24" to "Jani","2026-11-18" to "Proklamesana","2026-12-24" to "Ziemassvetki","2026-12-25" to "Ziemassvetki","2026-12-26" to "Otrie"))
    }catch(_:Exception){}
    val emps = mutableStateListOf<Employee>()
    try{
        val s = prefs.getString("DATA_V3",null)?: prefs.getString("DATA_V2",null)?: return Triple(mutableStateListOf(Employee(1,"Jurijs")),norms,holidays)
        val root=JSONObject(s); val it=root.keys()
        while(it.hasNext()){ val id=it.next(); val e=root.getJSONObject(id); val emp=Employee(id.toInt(),e.optString("name","Jauns")); emp.monthlySalary=e.optString("salary",""); emp.dependents=e.optString("dependents","0")
            if(e.has("months")){ val mo=e.getJSONObject("months"); val mk=mo.keys(); while(mk.hasNext()){ val kk=mk.next(); val days=mo.getJSONObject(kk); val map=mutableStateMapOf<Int,DayHours>(); val dk=days.keys(); while(dk.hasNext()){ val d=dk.next(); val o=days.getJSONObject(d); map[d.toInt()]=DayHours(o.optString("d"),o.optString("n")) }; emp.months[kk]=map } }
            if(e.has("bonuses")){ val bo=e.getJSONObject("bonuses"); val bk=bo.keys(); while(bk.hasNext()){ val kk=bk.next(); val o=bo.getJSONObject(kk); val oldP=o.optString("p").ifBlank{o.optString("p1")}; emp.bonuses[kk]=MonthBonus(oldP) } }
            emps.add(emp)
        }
    }catch(_:Exception){ emps.add(Employee(1,"Jurijs")) }
    return Triple(if(emps.isEmpty()) mutableStateListOf(Employee(1,"Jurijs")) else emps, norms, holidays)
}
class MainActivity : ComponentActivity(){ override fun onCreate(s:Bundle?){ super.onCreate(s); setContent{ MaterialTheme{ App() } } } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(){
    val ctx=LocalContext.current
    val loaded=remember{ loadAll(ctx) }
    val employees=loaded.first
    val norms=loaded.second
    val holidaysMap=loaded.third
    var yearMonth by remember{ mutableStateOf(YearMonth.now()) }
    val monthKey="${yearMonth.year}-${yearMonth.monthValue}"
    var selected by remember{ mutableStateOf(0) }
    var showCalc by remember{ mutableStateOf(false) }
    var showMenu by remember{ mutableStateOf(false) }
    var screen by remember{ mutableStateOf(0) } //0 table 1 norms 2 profile
    val normHours = norms[monthKey]?.toIntOrNull()?: 168
    val holidayDates = holidaysMap.keys.mapNotNull{ try{ LocalDate.parse(it)}catch(_:Exception){null} }.toSet()

    Scaffold(topBar={
        TopAppBar(title={Text(if(screen==0) "${yearMonth.monthValue}.${yearMonth.year} N:$normHours h" else "Iestatijumi")},
            navigationIcon={ Button(onClick={showMenu=!showMenu}){Text("≡")} },
            actions={ if(screen==0){ Button(onClick={yearMonth=yearMonth.minusMonths(1)}){Text("<")}; Spacer(Modifier.width(4.dp)); Button(onClick={yearMonth=yearMonth.plusMonths(1)}){Text(">")} } }
        )
    }){ pad->
        Column(Modifier.fillMaxSize().padding(pad).padding(8.dp).verticalScroll(rememberScrollState())){
            if(showMenu){
                Card(Modifier.fillMaxWidth()){ Column(Modifier.padding(8.dp)){
                    Button(onClick={screen=0; showMenu=false}, Modifier.fillMaxWidth()){Text("1. Darba tabula")}
                    Button(onClick={screen=1; showMenu=false}, Modifier.fillMaxWidth()){Text("2. Menesa normas")}
                    Button(onClick={screen=2; showMenu=false}, Modifier.fillMaxWidth()){Text("3. Darbinieks - alga")}
                    Button(onClick={showMenu=false}, Modifier.fillMaxWidth(), colors=ButtonDefaults.buttonColors(containerColor=Color.Gray)){Text("Aizvert")}
                }}
            }
            if(screen==0){
                Row{ employees.forEachIndexed{ i,e-> FilterChip(selected=i==selected, onClick={selected=i}, label={Text(e.name)}, modifier=Modifier.padding(end=4.dp)) } }
                if(employees.isNotEmpty()){
                    val emp=employees[selected]
                    val monthData=emp.getMonth(monthKey)
                    val bonus=emp.getBonus(monthKey)
                    OutlinedTextField(value=emp.name, onValueChange={emp.name=it; saveAll(ctx,employees,norms,holidaysMap)}, label={Text("Vards")}, modifier=Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth().background(Color(0xFFEEEEEE)).padding(6.dp)){
                        Text("D",Modifier.width(55.dp), fontWeight=FontWeight.Bold); Text("Diena",Modifier.width(80.dp), fontWeight=FontWeight.Bold); Text("Nakti",Modifier.width(80.dp), fontWeight=FontWeight.Bold); Text("Sv",Modifier.width(30.dp))
                    }
                    for(d in 1..yearMonth.lengthOfMonth()){
                        val date=yearMonth.atDay(d); val dh=monthData.getOrPut(d){DayHours()}; val isHol=holidayDates.contains(date)
                        val bg=if(isHol) Color(0xFFFFF9C4) else if(date.dayOfWeek.value>=6) Color(0xFFFFEBEE) else Color.White
                        Row(Modifier.fillMaxWidth().background(bg).padding(vertical=2.dp)){
                            Text("$d ${lvShort(date.dayOfWeek)}",Modifier.width(55.dp), fontWeight=FontWeight.Bold)
                            Outlined

package lv.timesheet

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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

class DayHours(d: String = "", n: String = "") {
    var day by mutableStateOf(d)
    var night by mutableStateOf(n)
}
class Employee(val id: Int, initialName: String) {
    var name by mutableStateOf(initialName)
    // Ключ = "2026-9", значение = карта дней
    val months = mutableStateMapOf<String, MutableMap<Int, DayHours>>()
    fun getMonth(key: String): MutableMap<Int, DayHours> = months.getOrPut(key){ mutableStateMapOf() }
}

fun lvShort(dow: DayOfWeek) = when(dow){
    DayOfWeek.MONDAY -> "Pr"; DayOfWeek.TUESDAY -> "Ot"; DayOfWeek.WEDNESDAY -> "Tr"
    DayOfWeek.THURSDAY -> "Ce"; DayOfWeek.FRIDAY -> "Pk"; DayOfWeek.SATURDAY -> "Se"; DayOfWeek.SUNDAY -> "Sv"
}

// Латвийские праздники
fun getLatvianHolidays(year: Int): Map<LocalDate, String> {
    val m = mutableMapOf<LocalDate, String>()
    m[LocalDate.of(year,1,1)] = "Jaunais gads"
    // Lieldienas 2026 = 3.04 un 6.04, 2025 = 18.04 un 21.04, 2027 = 26.03 un 29.03
    if(year==2025){ m[LocalDate.of(year,4,18)]="Lielā Piektdiena"; m[LocalDate.of(year,4,21)]="Otrās Lieldienas" }
    if(year==2026){ m[LocalDate.of(year,4,3)]="Lielā Piektdiena"; m[LocalDate.of(year,4,6)]="Otrās Lieldienas" }
    if(year==2027){ m[LocalDate.of(year,3,26)]="Lielā Piektdiena"; m[LocalDate.of(year,3,29)]="Otrās Lieldienas" }
    m[LocalDate.of(year,5,1)]="Darba svētki"
    m[LocalDate.of(year,5,4)]="Neatkarības atjaunošana"
    m[LocalDate.of(year,6,23)]="Līgo diena"
    m[LocalDate.of(year,6,24)]="Jāņu diena"
    m[LocalDate.of(year,11,18)]="Proklamēšanas diena"
    m[LocalDate.of(year,12,24)]="Ziemassvētku vakars"
    m[LocalDate.of(year,12,25)]="Ziemassvētki"
    m[LocalDate.of(year,12,26)]="Otrie Ziemassvētki"
    return m
}

fun saveAll(context: Context, list: List<Employee>) {
    val prefs = context.getSharedPreferences("timesheet", Context.MODE_PRIVATE)
    val root = JSONObject()
    list.forEach { emp ->
        val e = JSONObject()
        e.put("name", emp.name)
        val monthsObj = JSONObject()
        emp.months.forEach { (monthKey, daysMap) ->
            val daysObj = JSONObject()
            daysMap.forEach { (num, h) ->
                val o = JSONObject()
                o.put("d", h.day); o.put("n", h.night)
                daysObj.put(num.toString(), o)
            }
            monthsObj.put(monthKey, daysObj)
        }
        e.put("months", monthsObj)
        root.put(emp.id.toString(), e)
    }
    prefs.edit().putString("DATA_V2", root.toString()).commit()
}

fun loadAll(context: Context): MutableList<Employee> {
    val prefs = context.getSharedPreferences("timesheet", Context.MODE_PRIVATE)
    // Миграция со старой версии
    val old = prefs.getString("DATA", null)
    val s = prefs.getString("DATA_V2", null)?: old?: return mutableStateListOf(Employee(1,"Jurijs"))
    try {
        val root = JSONObject(s)
        val res = mutableStateListOf<Employee>()
        val it = root.keys()
        while(it.hasNext()){
            val id = it.next()
            val e = root.getJSONObject(id)
            val emp = Employee(id.toInt(), e.getString("name"))
            if(e.has("months")){
                val monthsObj = e.getJSONObject("months")
                val mKeys = monthsObj.keys()
                while(mKeys.hasNext()){
                    val mk = mKeys.next()
                    val daysObj = monthsObj.getJSONObject(mk)
                    val map = mutableStateMapOf<Int, DayHours>()
                    val dKeys = daysObj.keys()
                    while(dKeys.hasNext()){
                        val d = dKeys.next()
                        val o = daysObj.getJSONObject(d)
                        map[d.toInt()] = DayHours(o.optString("d"), o.optString("n"))
                    }
                    emp.months[mk] = map
                }
            } else if(e.has("days")){ // старая версия - переносим в текущий месяц
                val nowKey = "${YearMonth.now().year}-${YearMonth.now().monthValue}"
                val daysObj = e.getJSONObject("days")
                val map = mutableStateMapOf<Int, DayHours>()
                val dKeys = daysObj.keys()
                while(dKeys.hasNext()){
                    val d = dKeys.next()
                    val o = daysObj.getJSONObject(d)
                    map[d.toInt()] = DayHours(o.optString("d"), o.optString("n"))
                }
                emp.months[nowKey] = map
            }
            res.add(emp)
        }
        return if(res.isEmpty()) mutableStateListOf(Employee(1,"Jurijs")) else res
    } catch (ex: Exception){
        return mutableStateListOf(Employee(1,"Jurijs"))
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { App() } }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun App(){
    val ctx = LocalContext.current
    var yearMonth by remember { mutableStateOf(YearMonth.now()) }
    val monthKey = "${yearMonth.year}-${yearMonth.monthValue}"
    val employees = remember { loadAll(ctx) }
    var selected by remember { mutableStateOf(0) }
    var nextId by remember { mutableStateOf((employees.maxOfOrNull { it.id }?:0)+1) }
    var showSalary by remember { mutableStateOf(false) }

    val holidays = getLatvianHolidays(yearMonth.year)
    val holidaysThisMonth = holidays.filter { it.key.year==yearMonth.year && it.key.monthValue==yearMonth.monthValue }
    val workingDays = (1..yearMonth.lengthOfMonth()).count{
        val d = yearMonth.atDay(it)
        d.dayOfWeek.value<=5 &&!holidays.containsKey(d)
    }
    val normHours = workingDays * 8

    Scaffold(
        topBar = {
            TopAppBar(title={Text("${yearMonth.month.name.lowercase().replaceFirstChar{it.uppercase()}} ${yearMonth.year}")},
                navigationIcon = { Button(onClick = { yearMonth = yearMonth.minusMonths(1) }){Text("<")} },
                actions = { Button(onClick = { yearMonth = yearMonth.plusMonths(1) }){Text(">")} }
            )
        }
    ){ pad->
        Column(Modifier.fillMaxSize().padding(pad).padding(12.dp)){

            // Инфо о норме и праздниках
            Card(Modifier.fillMaxWidth(), colors=CardDefaults.cardColors(containerColor=Color(0xFFE3F2FD))){
                Column(Modifier.padding(10.dp)){
                    Text("Mēneša norma: $normHours h ($workingDays darba dienas x 8h)", fontWeight=FontWeight.Bold)
                    if(holidaysThisMonth.isNotEmpty()){
                        Text("Svētku dienas:", fontWeight=FontWeight.Bold, modifier=Modifier.padding(top=4.dp))
                        holidaysThisMonth.forEach{ (date,name)->
                            Text("${date.dayOfMonth}. ${date.monthValue}.

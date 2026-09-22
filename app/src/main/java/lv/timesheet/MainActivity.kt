package lv.timesheet

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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

// --- MODELI ---
class DayHours(d: String = "", n: String = "") {
    var day by mutableStateOf(d)
    var night by mutableStateOf(n)
}
class Employee(val id: Int, initialName: String) {
    var name by mutableStateOf(initialName)
    var monthlySalary by mutableStateOf("") // EUR
    var dependents by mutableStateOf("") // skaits
    val months = mutableStateMapOf<String, MutableMap<Int, DayHours>>()
    fun getMonth(key: String): MutableMap<Int, DayHours> = months.getOrPut(key){ mutableStateMapOf() }
}

enum class Screen { TABLE, NORMS, PROFILE, HOLIDAYS }

fun lvShort(dow: DayOfWeek) = when(dow){
    DayOfWeek.MONDAY -> "Pr"; DayOfWeek.TUESDAY -> "Ot"; DayOfWeek.WEDNESDAY -> "Tr"
    DayOfWeek.THURSDAY -> "Ce"; DayOfWeek.FRIDAY -> "Pk"; DayOfWeek.SATURDAY -> "Se"; DayOfWeek.SUNDAY -> "Sv"
}

// --- SAGLABASANA ---
fun saveEmployees(context: Context, list: List<Employee>) {
    val prefs = context.getSharedPreferences("timesheet", Context.MODE_PRIVATE)
    val root = JSONObject()
    list.forEach { emp ->
        val e = JSONObject()
        e.put("name", emp.name)
        e.put("salary", emp.monthlySalary)
        e.put("dependents", emp.dependents)
        val monthsObj = JSONObject()
        emp.months.forEach { (mk, daysMap) ->
            val daysObj = JSONObject()
            daysMap.forEach { (num, h) -> val o = JSONObject(); o.put("d", h.day); o.put("n", h.night); daysObj.put(num.toString(), o) }
            monthsObj.put(mk, daysObj)
        }
        e.put("months", monthsObj)
        root.put(emp.id.toString(), e)
    }
    prefs.edit().putString("DATA_V2", root.toString()).apply()
}
fun loadEmployees(context: Context): MutableList<Employee> {
    val prefs = context.getSharedPreferences("timesheet", Context.MODE_PRIVATE)
    val s = prefs.getString("DATA_V2", null)?: return mutableStateListOf(Employee(1,"Jurijs"))
    try {
        val root = JSONObject(s)
        val res = mutableStateListOf<Employee>()
        val it = root.keys()
        while(it.hasNext()){
            val id = it.next()
            val e = root.getJSONObject(id)
            val emp = Employee(id.toInt(), e.optString("name","Jauns"))
            emp.monthlySalary = e.optString("salary","")
            emp.dependents = e.optString("dependents","0")
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
                    emp.months[mk]=map
                }
            }
            res.add(emp)
        }
        return if(res.isEmpty()) mutableStateListOf(Employee(1,"Jurijs")) else res
    } catch (ex: Exception){ return mutableStateListOf(Employee(1,"Jurijs")) }
}

fun saveNorms(context: Context, norms: Map<String, String>){
    val prefs = context.getSharedPreferences("timesheet", Context.MODE_PRIVATE)
    val obj = JSONObject(); norms.forEach{ (k,v)-> obj.put(k,v) }
    prefs.edit().putString("NORMS", obj.toString()).apply()
}
fun loadNorms(context: Context): MutableMap<String, String> {
    val prefs = context.getSharedPreferences("timesheet", Context.MODE_PRIVATE)
    val s = prefs.getString("NORMS", null)?: return mutableStateMapOf()
    try {
        val obj = JSONObject(s); val map = mutableStateMapOf<String, String>()
        val k = obj.keys(); while(k.hasNext()){ val key=k.next(); map[key]=obj.getString(key) }
        return map
    } catch (e:Exception){ return mutableStateMapOf() }
}

fun saveHolidays(context: Context, holidays: Map<String, String>){
    val prefs = context.getSharedPreferences("timesheet", Context.MODE_PRIVATE)
    val obj = JSONObject(); holidays.forEach{ (k,v)-> obj.put(k,v) }
    prefs.edit().putString("HOLIDAYS", obj.toString()).apply()
}
fun loadHolidays(context: Context): MutableMap<String, String> {
    // default LV
    val def = mutableMapOf<String,String>(
        "2026-01-01" to "Jaunais gads",
        "2026-04-03" to "Liela Piektdiena",
        "2026-04-06" to "Otras Lieldienas",
        "2026-05-01" to "Darba svetki",
        "2026-05-04" to "Neatkaribas diena",
        "2026-06-23" to "Ligo",
        "2026-06-24" to "Jani",
        "2026-11-18" to "Proklamesana",
        "2026-12-24" to "Ziemassvetku vakars",
        "2026-12-25" to "Ziemassvetki",
        "2026-12-26" to "Otrie Ziemassvetki",
        "2026-12-31" to "Vecgada diena - saisinata"
    )
    val prefs = context.getSharedPreferences("timesheet", Context.MODE_PRIVATE)
    val s = prefs.getString("HOLIDAYS", null)?: return mutableStateMapOf<String,String>().apply { putAll(def) }
    try {
        val obj = JSONObject(s); val map = mutableStateMapOf<String,String>()
        val k = obj.keys(); while(k.hasNext()){ val key=k.next(); map[key]=obj.getString(key) }
        if(map.isEmpty()) map.putAll(def)
        return map
    } catch (e:Exception){ return mutableStateMapOf<String,String>().apply { putAll(def) } }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { App() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(){
    val ctx = LocalContext.current
    var screen by remember { mutableStateOf(Screen.TABLE) }
    var yearMonth by remember { mutableStateOf(YearMonth.now()) }
    val monthKey = "${yearMonth.year}-${yearMonth.monthValue}"
    val employees = remember { loadEmployees(ctx) }
    val norms = remember { loadNorms(ctx) }
    val holidays = remember { loadHolidays(ctx) }
    var selected by remember { mutableStateOf(0) }
    var nextId by remember { mutableStateOf((employees.maxOfOrNull { it.id }?:0)+1) }

    // норма: ручная или авто
    val autoNorm = 168 // fallback
    val normHoursStr = norms[monthKey]?: ""
    val normHours = normHoursStr.toIntOrNull()?: autoNorm

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet{
                Text("IZVELE", modifier=Modifier.padding(16.dp), fontWeight=FontWeight.Bold)

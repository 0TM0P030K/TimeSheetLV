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
import java.time.YearMonth

class DayHours(d: String = "", n: String = "") {
    var day by mutableStateOf(d)
    var night by mutableStateOf(n)
}
class Employee(val id: Int, initialName: String) {
    var name by mutableStateOf(initialName)
    val days = mutableStateMapOf<Int, DayHours>()
}

fun lvWeekDay(dow: DayOfWeek): String {
    return when(dow){
        DayOfWeek.MONDAY -> "Pirmd."
        DayOfWeek.TUESDAY -> "Otrd."
        DayOfWeek.WEDNESDAY -> "Trešd."
        DayOfWeek.THURSDAY -> "Ceturtd."
        DayOfWeek.FRIDAY -> "Piektd."
        DayOfWeek.SATURDAY -> "Sestd."
        DayOfWeek.SUNDAY -> "Svētd."
    }
}
fun lvWeekDayShort(dow: DayOfWeek): String {
    return when(dow){
        DayOfWeek.MONDAY -> "Pr"
        DayOfWeek.TUESDAY -> "Ot"
        DayOfWeek.WEDNESDAY -> "Tr"
        DayOfWeek.THURSDAY -> "Ce"
        DayOfWeek.FRIDAY -> "Pk"
        DayOfWeek.SATURDAY -> "Se"
        DayOfWeek.SUNDAY -> "Sv"
    }
}

fun saveAll(context: Context, list: List<Employee>) {
    val prefs = context.getSharedPreferences("timesheet", Context.MODE_PRIVATE)
    val root = JSONObject()
    list.forEach { emp ->
        val e = JSONObject()
        e.put("name", emp.name)
        val days = JSONObject()
        emp.days.forEach { (num, h) ->
            val o = JSONObject()
            o.put("d", h.day)
            o.put("n", h.night)
            days.put(num.toString(), o)
        }
        e.put("days", days)
        root.put(emp.id.toString(), e)
    }
    prefs.edit().putString("DATA", root.toString()).commit()
}

fun loadAll(context: Context): MutableList<Employee> {
    val prefs = context.getSharedPreferences("timesheet", Context.MODE_PRIVATE)
    val s = prefs.getString("DATA", null)?: return mutableStateListOf(Employee(1,"Janis Berzins"))
    try {
        val root = JSONObject(s)
        val res = mutableStateListOf<Employee>()
        val it = root.keys()
        while(it.hasNext()){
            val id = it.next()
            val e = root.getJSONObject(id)
            val emp = Employee(id.toInt(), e.getString("name"))
            val days = e.optJSONObject("days")
            if(days!=null){
                val kit = days.keys()
                while(kit.hasNext()){
                    val d = kit.next()
                    val o = days.getJSONObject(d)
                    emp.days[d.toInt()] = DayHours(o.optString("d"), o.optString("n"))
                }
            }
            res.add(emp)
        }
        return if(res.isEmpty()) mutableStateListOf(Employee(1,"Janis Berzins")) else res
    } catch (ex: Exception){
        return mutableStateListOf(Employee(1,"Janis Berzins"))
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
    val employees = remember { loadAll(ctx) }
    var selected by remember { mutableStateOf(0) }
    var nextId by remember { mutableStateOf((employees.maxOfOrNull { it.id }?:0)+1) }

    Scaffold(
        topBar = {
            TopAppBar(title={Text("${yearMonth.month.name} ${yearMonth.year}".lowercase().replaceFirstChar { it.uppercase() })},
                navigationIcon = { Button(onClick = { yearMonth = yearMonth.minusMonths(1) }){Text("<")} },
                actions = { Button(onClick = { yearMonth = yearMonth.plusMonths(1) }){Text(">")} }
            )
        }
    ){ pad->
        Column(Modifier.fillMaxSize().padding(pad).padding(12.dp)){
            Row{
                employees.forEachIndexed{ idx, emp->
                    FilterChip(selected=idx==selected, onClick={selected=idx}, label={Text(emp.name)}, modifier=Modifier.padding(end=4.dp))
                }
                SmallFloatingActionButton(onClick={
                    val ne = Employee(nextId++, "Jauns")
                    employees.add(ne)
                    selected = employees.lastIndex
                    saveAll(ctx, employees)
                }){Text("+")}
            }

            if(employees.isNotEmpty() && selected in employees.indices){
                val emp = employees[selected]
                Row(verticalAlignment=Alignment.CenterVertically){
                    OutlinedTextField(value=emp.name, onValueChange={
                        emp.name = it
                        saveAll(ctx, employees)
                    }, label={Text("Vārds Uzvārds")}, modifier=Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Button(onClick={
                        employees.removeAt(selected)
                        selected = 0.coerceAtLeast(employees.size-1)
                        saveAll(ctx, employees)
                    }, colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFFFCDD2))){
                        Text("DZĒST", color=Color.Red)
                    }
                }

                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f)){
                    stickyHeader{
                        Row(Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(8.dp)){
                            Text("Diena",Modifier.width(70.dp), fontWeight=FontWeight.Bold)
                            Text("Dienā",Modifier.width(85.dp), fontWeight=FontWeight.Bold)
                            Text("Naktī *",Modifier.width(85.dp), fontWeight=FontWeight.Bold)
                        }
                    }
                    val daysInMonth = yearMonth.lengthOfMonth()
                    items(daysInMonth){ idx->
                        val d = idx+1
                        val date = yearMonth.atDay(d)
                        val isWeekend = date.dayOfWeek.value>=6
                        val dh = emp.days.getOrPut(d){ DayHours() }
                        Row(Modifier.fillMaxWidth().background(if(isWeekend) Color(0xFFFFEBEE) else Color.White).padding(vertical=4.dp), verticalAlignment=Alignment.CenterVertically){
                            Text("${d}. ${lvWeekDayShort(date.dayOfWeek)}", Modifier.width(70.dp), color=if(isWeekend) Color.Red else Color.Black, fontWeight=FontWeight.Bold)
                            OutlinedTextField(value=dh.day, onValueChange={
                                dh.day = it
                                saveAll(ctx, employees)
                            }, Modifier.width(80.dp).padding(end=4.dp), keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), singleLine=true)
                            OutlinedTextField(value=dh.night, onValueChange={
                                dh.night = it
                                saveAll(ctx, employees)
                            }, Modifier.width(80.dp), keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), singleLine=true)
                        }
                    }
                }
                val totalD = emp.days.values.mapNotNull{ it.day.toDoubleOrNull() }.sum()
                val totalN = emp.days.values.mapNotNull{ it.night.toDoubleOrNull() }.sum()
                Card(Modifier.fillMaxWidth().padding(top=8.dp), colors=CardDefaults.cardColors(containerColor=Color(0xFFE8F5E9))){
                    Column(Modifier.padding(12.dp)){
                        Text("Dienas stundas: $totalD h", fontWeight=FontWeight.Bold)
                        Text("Nakts stundas: $totalN h (piemaksa atsevišķi)", fontWeight=FontWeight.Bold, color=Color(0xFFBF360C))
                    }
                }
            }
        }
    }
}

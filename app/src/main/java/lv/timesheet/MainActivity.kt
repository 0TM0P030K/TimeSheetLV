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
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.time.YearMonth

data class DayHours(var day: String = "", var night: String = "")
data class Employee(val id: Int, var name: String, val days: SnapshotStateMap<Int, DayHours> = mutableStateMapOf())

fun saveEmployees(context: Context, employees: List<Employee>) {
    val prefs = context.getSharedPreferences("timesheet", Context.MODE_PRIVATE)
    val json = JSONObject()
    employees.forEach { emp ->
        val empObj = JSONObject()
        empObj.put("name", emp.name)
        val daysObj = JSONObject()
        emp.days.forEach { (d, h) ->
            val dh = JSONObject()
            dh.put("d", h.day)
            dh.put("n", h.night)
            daysObj.put(d.toString(), dh)
        }
        empObj.put("days", daysObj)
        json.put(emp.id.toString(), empObj)
    }
    prefs.edit().putString("data", json.toString()).apply()
}

fun loadEmployees(context: Context): MutableList<Employee> {
    val prefs = context.getSharedPreferences("timesheet", Context.MODE_PRIVATE)
    val str = prefs.getString("data", null)?: return mutableStateListOf(Employee(1, "Janis Berzins"), Employee(2, "Anna Ozola"))
    return try {
        val json = JSONObject(str)
        val list = mutableStateListOf<Employee>()
        val keys = json.keys()
        while(keys.hasNext()){
            val idStr = keys.next()
            val empObj = json.getJSONObject(idStr)
            val emp = Employee(idStr.toInt(), empObj.getString("name"))
            val daysObj = empObj.optJSONObject("days")
            if(daysObj!=null){
                val dKeys = daysObj.keys()
                while(dKeys.hasNext()){
                    val dStr = dKeys.next()
                    val dhObj = daysObj.getJSONObject(dStr)
                    emp.days[dStr.toInt()] = DayHours(dhObj.optString("d",""), dhObj.optString("n",""))
                }
            }
            list.add(emp)
        }
        if(list.isEmpty()) mutableStateListOf(Employee(1, "Janis Berzins")) else list
    } catch (e: Exception){
        mutableStateListOf(Employee(1, "Janis Berzins"), Employee(2, "Anna Ozola"))
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { TimeSheetScreen() } }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TimeSheetScreen() {
    val context = LocalContext.current
    var yearMonth by remember { mutableStateOf(YearMonth.now()) }
    val employees = remember { loadEmployees(context) }
    var selected by remember { mutableStateOf(0) }
    var nextId by remember { mutableStateOf((employees.maxOfOrNull { it.id }?: 0) + 1) }

    // Автосохранение при любом изменении
    LaunchedEffect(employees.toList(), employees.map { it.name }, employees.map { it.days.toMap() }) {
        saveEmployees(context, employees)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("${yearMonth.month.name} ${yearMonth.year}") },
                navigationIcon = { Button(onClick = { yearMonth = yearMonth.minusMonths(1) }) { Text("<") } },
                actions = { Button(onClick = { yearMonth = yearMonth.plusMonths(1) }) { Text(">") } }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(12.dp)) {
            Row {
                employees.forEachIndexed { idx, emp ->
                    FilterChip(selected = idx == selected, onClick = { selected = idx }, label = { Text(emp.name) }, modifier = Modifier.padding(end=4.dp))
                }
                SmallFloatingActionButton(onClick = { employees.add(Employee(nextId++, "Jauns")); selected = employees.lastIndex }) { Text("+") }
            }

            if (employees.isNotEmpty() && selected in employees.indices) {
                val emp = employees[selected]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = emp.name, onValueChange = { emp.name = it; saveEmployees(context, employees) }, label = { Text("Vārds Uzvārds") }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { employees.removeAt(selected); selected = 0.coerceAtLeast(employees.size-1); saveEmployees(context, employees) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCDD2))) {
                        Text("DZĒST", color = Color.Red)
                    }
                }

                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f)) {
                    stickyHeader {
                        Row(Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(8.dp)) {
                            Text("Diena", Modifier.width(70.dp), fontWeight = FontWeight.Bold)
                            Text("Dienā", Modifier.width(85.dp), fontWeight = FontWeight.Bold)
                            Text("Naktī *", Modifier.width(85.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                    val daysInMonth = yearMonth.lengthOfMonth()
                    items(daysInMonth) { index ->
                        val d = index + 1
                        val date = yearMonth.atDay(d)
                        val isWeekend = date.dayOfWeek.value >= 6
                        val dh = emp.days.getOrPut(d) { DayHours() }
                        Row(
                            Modifier.fillMaxWidth().background(if(isWeekend) Color(0xFFFFEBEE) else Color.White).padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${d}. ${date.dayOfWeek.name.take(3)}", Modifier.width(70.dp), color = if(isWeekend) Color.Red else Color.Black, fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = dh.day, onValueChange = { emp.days[d] = dh.copy(day = it); saveEmployees(context, employees) }, Modifier.width(80.dp).padding(end=4.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                            OutlinedTextField(value = dh.night, onValueChange = { emp.days[d] = dh.copy(night = it); saveEmployees(context, employees) }, Modifier.width(80.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                        }
                    }
                }
                val totalD = emp.days.values.mapNotNull { it.day.toDoubleOrNull() }.sum()
                val totalN = emp.days.values.mapNotNull { it.night.toDoubleOrNull() }.sum()
                // ИСПРАВЛЕНО: считается отдельно
                Card(Modifier.fillMaxWidth().padding(top=8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Dienas stundas: $totalD h", fontWeight = FontWeight.Bold)
                        Text("Nakts stundas: $totalN h (piemaksa)", fontWeight = FontWeight.Bold, color = Color(0xFFBF360C))
                        Text("Kopā nostrādāts: ${totalD + totalN} h", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

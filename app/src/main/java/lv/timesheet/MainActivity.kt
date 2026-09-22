package lv.timesheet

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.YearMonth

data class DayHours(var day: String = "", var night: String = "")
data class Employee(val id: Int, var name: String, val days: SnapshotStateMap<Int, DayHours> = mutableStateMapOf())

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { TimeSheetScreen() } }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TimeSheetScreen() {
    var yearMonth by remember { mutableStateOf(YearMonth.now()) }
    val employees = remember { mutableStateListOf(Employee(1, "Janis Berzins"), Employee(2, "Anna Ozola")) }
    var selected by remember { mutableStateOf(0) }
    var nextId by remember { mutableStateOf(3) }

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
                    OutlinedTextField(value = emp.name, onValueChange = { emp.name = it }, label = { Text("Vārds Uzvārds") }, modifier = Modifier.weight(1f))
                    IconButton(onClick = { employees.removeAt(selected); selected = 0.coerceAtLeast(employees.size-1) }) { Text("DZĒST", color = Color.Red) }
                }

                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f)) {
                    stickyHeader {
                        Row(Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(8.dp)) {
                            Text("Diena", Modifier.width(70.dp), fontWeight = FontWeight.Bold)
                            Text("Diena h", Modifier.width(85.dp), fontWeight = FontWeight.Bold)
                            Text("Nakts h", Modifier.width(85.dp), fontWeight = FontWeight.Bold)
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
                            OutlinedTextField(value = dh.day, onValueChange = { emp.days[d] = dh.copy(day = it) }, Modifier.width(80.dp).padding(end=4.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                            OutlinedTextField(value = dh.night, onValueChange = { emp.days[d] = dh.copy(night = it) }, Modifier.width(80.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                        }
                    }
                }
                val totalD = emp.days.values.mapNotNull { it.day.toDoubleOrNull() }.sum()
                val totalN = emp.days.values.mapNotNull { it.night.toDoubleOrNull() }.sum()
                Text("Kopā: $totalD + $totalN = ${totalD+totalN} h", fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
            }
        }
    }
} 

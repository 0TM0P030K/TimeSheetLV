package lv.timesheet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

data class Employee(var name: String, val hours: MutableMap<Int, String> = mutableMapOf())

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TimeSheetScreen()
            }
        }
    }
}

@Composable
fun TimeSheetScreen() {
    var yearMonth by remember { mutableStateOf(YearMonth.now()) }
    val employees = remember { mutableStateListOf(Employee("Janis Berzins"), Employee("Anna Ozola")) }
    var selectedEmp by remember { mutableStateOf(0) }

    val daysInMonth = yearMonth.lengthOfMonth()
    val today = LocalDate.now()

    Column(Modifier.fillMaxSize().padding(8.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { yearMonth = yearMonth.minusMonths(1) }) { Text("<") }
            Text("${yearMonth.month.name} ${yearMonth.year}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { yearMonth = yearMonth.plusMonths(1) }) { Text(">") }
        }

        Spacer(Modifier.height(8.dp))

        Row {
            employees.forEachIndexed { i, emp ->
                FilterChip(selected = i == selectedEmp, onClick = { selectedEmp = i }, label = { Text(emp.name) }, modifier = Modifier.padding(end=4.dp))
            }
            Button(onClick = { employees.add(Employee("Darbinieks ${employees.size+1}")) }) { Text("+") }
        }

        Spacer(Modifier.height(12.dp))

        // Editable name
        if (employees.isNotEmpty()) {
            OutlinedTextField(value = employees[selectedEmp].name, onValueChange = { employees[selectedEmp] = employees[selectedEmp].copy(name = it) }, label = { Text("Vārds Uzvārds") }, modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(12.dp))

        // Header
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            Box(Modifier.width(50.dp)) { Text("Diena", fontWeight = FontWeight.Bold) }
            for (d in 1..daysInMonth) {
                val date = yearMonth.atDay(d)
                val isWeekend = date.dayOfWeek.value >= 6
                Box(Modifier.width(40.dp).background(if(isWeekend) Color(0xFFFFCDD2) else Color.Transparent).padding(2.dp)) {
                    Text("$d", fontWeight = FontWeight.Bold, color = if(isWeekend) Color.Red else Color.Black)
                }
            }
            Box(Modifier.width(60.dp)) { Text("Sum", fontWeight = FontWeight.Bold) }
        }
        Divider()

        // Current employee row
        if (employees.isNotEmpty()) {
            val emp = employees[selectedEmp]
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp)) {
                Box(Modifier.width(50.dp)) { Text("Stundas") }
                var sum = 0.0
                for (d in 1..daysInMonth) {
                    val v = emp.hours[d]?: ""
                    if (v.toDoubleOrNull()!= null) sum += v.toDouble()
                    OutlinedTextField(
                        value = v,
                        onValueChange = { emp.hours[d] = it },
                        modifier = Modifier.width(40.dp).height(52.dp).padding(1.dp),
                        singleLine = true
                    )
                }
                Box(Modifier.width(60.dp).padding(start=4.dp)) { Text("%.1f".format(sum), fontWeight = FontWeight.Bold) }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Instrukcija: Raksti 8, 8.5, B - slimiba, A - atvalinajums. Weekends sarkani.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Button(onClick = { /* te bus PDF exports */ }, modifier = Modifier.fillMaxWidth()) { Text("Eksportēt uz PDF (nakamais solis)") }
    }
}

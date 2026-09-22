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
    val months = mutableStateMapOf<String, MutableMap<Int, DayHours>>()
    fun getMonth(key: String): MutableMap<Int, DayHours> = months.getOrPut(key){ mutableStateMapOf() }
}

fun lvShort(dow: DayOfWeek): String = when(dow){
    DayOfWeek.MONDAY -> "Pr"; DayOfWeek.TUESDAY -> "Ot"; DayOfWeek.WEDNESDAY -> "Tr"
    DayOfWeek.THURSDAY -> "Ce"; DayOfWeek.FRIDAY -> "Pk"; DayOfWeek.SATURDAY -> "Se"; DayOfWeek.SUNDAY -> "Sv"
}

fun getLatvianHolidays(year: Int): Map<LocalDate, String> {
    val m = mutableMapOf<LocalDate, String>()
    m[LocalDate.of(year,1,1)] = "Jaunais gads"
    if(year==2025){ m[LocalDate.of(year,4,18)]="Liela Piektdiena"; m[LocalDate.of(year,4,21)]="Otras Lieldienas" }
    if(year==2026){ m[LocalDate.of(year,4,3)]="Liela Piektdiena"; m[LocalDate.of(year,4,6)]="Otras Lieldienas" }
    if(year==2027){ m[LocalDate.of(year,3,26)]="Liela Piektdiena"; m[LocalDate.of(year,3,29)]="Otras Lieldienas" }
    m[LocalDate.of(year,5,1)]="Darba svetki"
    m[LocalDate.of(year,5,4)]="Neatkaribas atjaunosana"
    m[LocalDate.of(year,6,23)]="Ligo diena"
    m[LocalDate.of(year,6,24)]="Janu diena"
    m[LocalDate.of(year,11,18)]="Proklamesanas diena"
    m[LocalDate.of(year,12,24)]="Ziemassvetku vakars"
    m[LocalDate.of(year,12,25)]="Ziemassvetki"
    m[LocalDate.of(year,12,26)]="Otrie Ziemassvetki"
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
                val o = JSONObject(); o.put("d", h.day); o.put("n", h.night)
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
            } else if(e.has("days")){
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
    // Праздники в этом месяце
    val holidaysThisMonth = holidays.filter { it.key.year==yearMonth.year && it.key.monthValue==yearMonth.monthValue }
    // Сокращенные дни: день перед праздником, если это рабочий день
    val shortenedDays = holidays.map { it.key.minusDays(1) }
       .filter {
            it.year==yearMonth.year && it.monthValue==yearMonth.monthValue
            && it.dayOfWeek.value<=5
            &&!holidays.containsKey(it)
        }.sorted()

    val workingDays = (1..yearMonth.lengthOfMonth()).count{
        val d = yearMonth.atDay(it)
        d.dayOfWeek.value<=5 &&!holidays.containsKey(d)
    }
    // Норма = рабочие дни * 8 - сокращенные * 1
    val normHours = workingDays * 8 - shortenedDays.size

    Scaffold(
        topBar = {
            TopAppBar(title={Text("${yearMonth.month} ${yearMonth.year}")},
                navigationIcon = { Button(onClick = { yearMonth = yearMonth.minusMonths(1) }){Text("<")} },
                actions = { Button(onClick = { yearMonth = yearMonth.plusMonths(1) }){Text(">")} }
            )
        }
    ){ pad->
        Column(Modifier.fillMaxSize().padding(pad).padding(12.dp)){

            Card(Modifier.fillMaxWidth(), colors=CardDefaults.cardColors(containerColor=Color(0xFFE3F2FD))){
                Column(Modifier.padding(10.dp)){
                    Text("Menesa norma: $normHours h", fontWeight=FontWeight.Bold)
                    Text("($workingDays darba dienas x 8h - ${shortenedDays.size} saisinatas x 1h)", style=MaterialTheme.typography.labelMedium)
                    if(holidaysThisMonth.isNotEmpty()){
                        Spacer(Modifier.height(4.dp))
                        Text("Svetku dienas:", fontWeight=FontWeight.Bold)
                        holidaysThisMonth.forEach{ (date,name)-> Text("${date.dayOfMonth}.${date.monthValue}. - $name", color=Color(0xFF0D47A1)) }
                    }
                    if(shortenedDays.isNotEmpty()){
                        Spacer(Modifier.height(4.dp))
                        Text("Saisinatas dienas (-1h):", fontWeight=FontWeight.Bold, color=Color(0xFFE65100))
                        shortenedDays.forEach{ d->
                            val h = holidays[d.plusDays(1)]
                            Text("${d.dayOfMonth}.${d.monthValue}. ${lvShort(d.dayOfWeek)} - pirms $h", color=Color(0xFFE65100))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            Row{
                employees.forEachIndexed{ idx, emp-> FilterChip(selected=idx==selected, onClick={selected=idx}, label={Text(emp.name)}, modifier=Modifier.padding(end=4.dp)) }
                SmallFloatingActionButton(onClick={
                    val ne = Employee(nextId++, "Jauns"); employees.add(ne); selected = employees.lastIndex; saveAll(ctx, employees)
                }){Text("+")}
            }

            if(employees.isNotEmpty() && selected in employees.indices){
                val emp = employees[selected]
                val monthData = emp.getMonth(monthKey)

                Row(verticalAlignment=Alignment.CenterVertically){
                    OutlinedTextField(value=emp.name, onValueChange={ emp.name=it; saveAll(ctx,employees) }, label={Text("Vards Uzvards")}, modifier=Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Button(onClick={ employees.removeAt(selected); selected=0.coerceAtLeast(employees.size-1); saveAll(ctx,employees) }, colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFFFCDD2))){ Text("DZEST", color=Color.Red) }
                }

                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f)){
                    stickyHeader{
                        Row(Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(8.dp)){
                            Text("Diena",Modifier.width(70.dp), fontWeight=FontWeight.Bold)
                            Text("Diena",Modifier.width(85.dp), fontWeight=FontWeight.Bold)
                            Text("Nakti",Modifier.width(85.dp), fontWeight=FontWeight.Bold)
                        }
                    }
                    items(yearMonth.lengthOfMonth()){ idx->
                        val d = idx+1
                        val date = yearMonth.atDay(d)
                        val isHoliday = holidays.containsKey(date)
                        val isShort = shortenedDays.contains(date)
                        val isWeekend = date.dayOfWeek.value>=6
                        val dh = monthData.getOrPut(d){ DayHours() }
                        val bg = when{
                            isHoliday -> Color(0xFFFFF9C4)
                            isShort -> Color(0xFFFFE0B2)
                            isWeekend -> Color(0xFFFFEBEE)
                            else -> Color.White
                        }
                        Row(Modifier.fillMaxWidth().background(bg).padding(vertical=4.dp), verticalAlignment=Alignment.CenterVertically){
                            Column(Modifier.width(70.dp)){
                                Text("$d. ${lvShort(date.dayOfWeek)}", color=if(isWeekend||isHoliday) Color.Red else Color.Black, fontWeight=FontWeight.Bold)
                                if(isShort) Text(" -1h", color=Color(0xFFE65100), style=MaterialTheme.typography.labelSmall, fontWeight=FontWeight.Bold)
                                if(isHoliday) Text(holidays[date]!!.take(10), color=Color(0xFFF57F17), style=MaterialTheme.typography.labelSmall)
                            }
                            OutlinedTextField(value=dh.day, onValueChange={ dh.day=it; saveAll(ctx,employees) }, Modifier.width(80.dp).padding(end=4.dp), keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), singleLine=true)
                            OutlinedTextField(value=dh.night, onValueChange={ dh.night=it; saveAll(ctx,employees) }, Modifier.width(80.dp), keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), singleLine=true)
                        }
                    }
                }
                val totalD = monthData.values.mapNotNull{ it.day.toDoubleOrNull() }.sum()
                val totalN = monthData.values.mapNotNull{ it.night.toDoubleOrNull() }.sum()
                val virsstundas = (totalD - normHours).coerceAtLeast(0.0)
                val trukst = (normHours - totalD).coerceAtLeast(0.0)

                Card(Modifier.fillMaxWidth().padding(top=8.dp), colors=CardDefaults.cardColors(containerColor=Color(0xFFE8F5E9))){
                    Column(Modifier.padding(12.dp)){
                        Text("Dienas: $totalD h / Norma $normHours h", fontWeight=FontWeight.Bold)
                        if(virsstundas>0) Text("VIRSSTUNDAS: $virsstundas h", fontWeight=FontWeight.Bold, color=Color.Red)
                        else Text("Lidz normai: $trukst h", color=Color(0xFF2E7D32))
                        Divider(Modifier.padding(vertical=4.dp))
                        Text("Nakts: $totalN h (atseviski)", fontWeight=FontWeight.Bold, color=Color(0xFFBF360C))
                    }
                }
                Button(onClick={ showSalary=true }, modifier=Modifier.fillMaxWidth().padding(top=8.dp)){ Text("APREKINAT ALGU") }

                if(showSalary){
                    AlertDialog(onDismissRequest={showSalary=false}, title={Text("Alga")},
                        text={
                            Column{
                                Text("Darbinieks: ${emp.name}")
                                Text("Menesis: $monthKey")
                                Text("Norma: $normHours h (ar saisinatajam)")
                                Text("Dienas: $totalD h")
                                Text("Virsstundas: $virsstundas h")
                                Text("Nakts: $totalN h")
                                Text("Saisinatas: ${shortenedDays.size} dienas")
                            }
                        },
                        confirmButton={ Button(onClick={showSalary=false}){Text("Aizvert")} }
                    )
                }
            }
        }
    }
}

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
                NavigationDrawerItem(label={Text("Darba tabula")}, selected=screen==Screen.TABLE, onClick={screen=Screen.TABLE})
                NavigationDrawerItem(label={Text("Menesa normas (manual)")}, selected=screen==Screen.NORMS, onClick={screen=Screen.NORMS})
                NavigationDrawerItem(label={Text("Darbinieks - alga, apgadajamie")}, selected=screen==Screen.PROFILE, onClick={screen=Screen.PROFILE})
                NavigationDrawerItem(label={Text("Svetku dienas - rediget")}, selected=screen==Screen.HOLIDAYS, onClick={screen=Screen.HOLIDAYS})
            }
        }
    ){
        Scaffold(
            topBar = {
                TopAppBar(title={Text(
                    when(screen){
                        Screen.TABLE -> "${yearMonth.month} ${yearMonth.year} | Norma $normHours h"
                        Screen.NORMS -> "Menesa normas"
                        Screen.PROFILE -> "Darbinieka dati"
                        Screen.HOLIDAYS -> "Svetku dienas"
                    }
                )},
                    navigationIcon = {
                        // menu burger
                        Icon(Icons.Default.Menu, contentDescription=null)
                    },
                    actions = {
                        if(screen==Screen.TABLE){
                            Button(onClick={ yearMonth=yearMonth.minusMonths(1) }){Text("<")}
                            Spacer(Modifier.width(4.dp))
                            Button(onClick={ yearMonth=yearMonth.plusMonths(1) }){Text(">")}
                        }
                    }
                )
            }
        ){ pad->
            Column(Modifier.fillMaxSize().padding(pad).padding(12.dp)){
                when(screen){
                    Screen.TABLE -> {
                        // выбор сотрудника
                        Row{
                            employees.forEachIndexed{ idx, emp-> FilterChip(selected=idx==selected, onClick={selected=idx}, label={Text(emp.name)}, modifier=Modifier.padding(end=4.dp)) }
                            SmallFloatingActionButton(onClick={ val ne=Employee(nextId++,"Jauns"); employees.add(ne); selected=employees.lastIndex; saveEmployees(ctx,employees) }){Text("+")}
                        }
                        if(employees.isNotEmpty()){
                            val emp = employees[selected]
                            val monthData = emp.getMonth(monthKey)
                            Row(verticalAlignment=Alignment.CenterVertically){
                                OutlinedTextField(value=emp.name, onValueChange={ emp.name=it; saveEmployees(ctx,employees) }, label={Text("Vards Uzvards")}, modifier=Modifier.weight(1f))
                                Spacer(Modifier.width(8.dp))
                                Button(onClick={ employees.removeAt(selected); selected=0; saveEmployees(ctx,employees) }, colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFFFCDD2))){Text("DZEST")}
                            }
                            Spacer(Modifier.height(8.dp))
                            LazyColumn(Modifier.weight(1f)){
                                item{
                                    Row(Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(8.dp)){
                                        Text("Diena",Modifier.width(70.dp), fontWeight=FontWeight.Bold)
                                        Text("Diena",Modifier.width(85.dp), fontWeight=FontWeight.Bold)
                                        Text("Nakti",Modifier.width(85.dp), fontWeight=FontWeight.Bold)
                                    }
                                }
                                items(yearMonth.lengthOfMonth()){ idx->
                                    val d=idx+1
                                    val date = yearMonth.atDay(d)
                                    val dh = monthData.getOrPut(d){ DayHours() }
                                    val isWeekend = date.dayOfWeek.value>=6
                                    val bg = if(isWeekend) Color(0xFFFFEBEE) else Color.White
                                    Row(Modifier.fillMaxWidth().background(bg).padding(vertical=4.dp), verticalAlignment=Alignment.CenterVertically){
                                        Text("$d. ${lvShort(date.dayOfWeek)}", Modifier.width(70.dp), fontWeight=FontWeight.Bold, color=if(isWeekend) Color.Red else Color.Black)
                                        OutlinedTextField(value=dh.day, onValueChange={ dh.day=it; saveEmployees(ctx,employees) }, Modifier.width(80.dp).padding(end=4.dp), keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), singleLine=true)
                                        OutlinedTextField(value=dh.night, onValueChange={ dh.night=it; saveEmployees(ctx,employees) }, Modifier.width(80.dp), keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), singleLine=true)
                                    }
                                }
                            }
                            val totalD = monthData.values.mapNotNull{ it.day.toDoubleOrNull() }.sum()
                            val totalN = monthData.values.mapNotNull{ it.night.toDoubleOrNull() }.sum()
                            val virs = (totalD - normHours).coerceAtLeast(0.0)
                            Card(Modifier.fillMaxWidth().padding(top=8.dp), colors=CardDefaults.cardColors(containerColor=Color(0xFFE8F5E9))){
                                Column(Modifier.padding(12.dp)){
                                    Text("Dienas: $totalD h / Norma $normHours h", fontWeight=FontWeight.Bold)
                                    if(virs>0) Text("VIRSSTUNDAS (tikai dienas): $virs h", color=Color.Red, fontWeight=FontWeight.Bold)
                                    else Text("Lidz normai: ${(normHours-totalD).coerceAtLeast(0.0)} h")
                                    Text("Nakts: $totalN h (atseviski, neietilpst norma)", color=Color(0xFFBF360C))
                                }
                            }
                            Button(onClick={}, Modifier.fillMaxWidth().padding(top=8.dp)){ Text("APREKINAT ALGU - nakamais solis") }
                        }
                    }
                    Screen.NORMS -> {
                        Text("Sheit vari ievadit normu katram menesim manuali. Ja neievadisi, izmantos noklusejuma.", style=MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        var editMonth by remember { mutableStateOf(YearMonth.now()) }
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Button(onClick={ editMonth=editMonth.minusMonths(1) }){Text("<")}
                            Text("${editMonth.year}-${editMonth.monthValue}", modifier=Modifier.padding(horizontal=12.dp), fontWeight=FontWeight.Bold)
                            Button(onClick={ editMonth=editMonth.plusMonths(1) }){Text(">")}
                        }
                        Spacer(Modifier.height(12.dp))
                        val key = "${editMonth.year}-${editMonth.monthValue}"
                        var value by remember(key) { mutableStateOf(norms[key]?: "") }
                        OutlinedTextField(value=value, onValueChange={ value=it }, label={Text("Norma stundas $key")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), modifier=Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Button(onClick={
                            norms[key]=value
                            saveNorms(ctx, norms)
                        }, modifier=Modifier.fillMaxWidth()){ Text("SAGLABAT NORMU") }
                        Spacer(Modifier.height(16.dp))
                        Text("Saglabatas normas:", fontWeight=FontWeight.Bold)
                        LazyColumn{
                            items(norms.keys.toList().sorted().size){ i->
                                val k = norms.keys.toList().sorted()[i]
                                Row(Modifier.fillMaxWidth().padding(vertical=4.dp), horizontalArrangement=Arrangement.SpaceBetween){
                                    Text("$k : ${norms[k]} h")
                                    TextButton(onClick={ norms.remove(k); saveNorms(ctx,norms) }){Text("Dzest", color=Color.Red)}
                                }
                            }
                        }
                    }
                    Screen.PROFILE -> {
                        if(employees.isEmpty()){ Text("Nav darbinieku") }
                        else {
                            val emp = employees[selected]
                            Text("Izveletais: ${emp.name}", fontWeight=FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value=emp.monthlySalary, onValueChange={ emp.monthlySalary=it; saveEmployees(ctx,employees) }, label={Text("Menesa oklads EUR (bruto)")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), modifier=Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value=emp.dependents, onValueChange={ emp.dependents=it; saveEmployees(ctx,employees) }, label={Text("Apgadajamo skaits")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), modifier=Modifier.fillMaxWidth())
                            Spacer(Modifier.height(12.dp))
                            Card(Modifier.fillMaxWidth(), colors=CardDefaults.cardColors(containerColor=Color(0xFFFFF3E0))){
                                Column(Modifier.padding(12.dp)){
                                    Text("Ka rekinas alga:", fontWeight=FontWeight.Bold)
                                    Text("1. Stundas likme = oklads / norma")
                                    Text("2. Dienas virsstundas = (dienas - norma) * likme * 2.0")
                                    Text("3. Nakts piemaksa atseviski")
                                    Text("4. Apgadajamie ietekme NIN atlaidi")
                                }
                            }
                        }
                    }
                    Screen.HOLIDAYS -> {
                        Text("Sheit vari labot svetku un saisinatas dienas. Formats: YYYY-MM-DD", style=MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        var newDate by remember { mutableStateOf("") }
                        var newName by remember { mutableStateOf("") }
                        Row{
                            OutlinedTextField(value=newDate, onValueChange={newDate=it}, label={Text("2026-12-24")}, modifier=Modifier.weight(1f))
                            Spacer(Modifier.width(4.dp))
                            OutlinedTextField(value=newName, onValueChange={newName=it}, label={Text("Nosaukums")}, modifier=Modifier.weight(1f))
                        }
                        Button(onClick={
                            if(newDate.isNotBlank()){
                                holidays[newDate]=newName.ifBlank { "Svetki" }
                                saveHolidays(ctx, holidays)
                                newDate=""; newName=""
                            }
                        }, modifier=Modifier.fillMaxWidth().padding(top=4.dp)){ Text("PIEVIENOT / LABOT") }
                        Spacer(Modifier.height(12.dp))
                        LazyColumn(Modifier.weight(1f)){
                            items(holidays.keys.toList().sorted().size){ i->
                                val k = holidays.keys.toList().sorted()[i]
                                Row(Modifier.fillMaxWidth().padding(vertical=4.dp), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically){
                                    Column{ Text(k, fontWeight=FontWeight.Bold); Text(holidays[k]!!, style=MaterialTheme.typography.labelMedium) }
                                    TextButton(onClick={ holidays.remove(k); saveHolidays(ctx,holidays) }){Text("Dzest", color=Color.Red)}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

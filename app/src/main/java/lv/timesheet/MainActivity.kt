package lv.timesheet

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
enum class Screen { TABLE, NORMS, PROFILE, HOLIDAYS }

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
    prefs.edit().

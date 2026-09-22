package lv.timesheet

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import java.time.YearMonth
import kotlin.math.max
import kotlin.math.min

fun shortDay(d: DayOfWeek): String {
    return when(d){
        DayOfWeek.MONDAY->"Pr"; DayOfWeek.TUESDAY->"Ot"; DayOfWeek.WEDNESDAY->"Tr"
        DayOfWeek.THURSDAY->"Ce"; DayOfWeek.FRIDAY->"Pk"; DayOfWeek.SATURDAY->"Se"; else->"Sv"
    }
}
fun loadNorm(c: Context, k: String): String { return c.getSharedPreferences("timesheet", Context.MODE_PRIVATE).getString("NORM_$k","168")?: "168" }
fun saveNorm(c: Context, k: String, v: String){ c.getSharedPreferences("timesheet", Context.MODE_PRIVATE).edit().putString("NORM_$k",v).apply() }
fun loadName(c: Context): String { return c.getSharedPreferences("timesheet", Context.MODE_PRIVATE).getString("NAME","Jurijs")?: "Jurijs" }
fun saveName(c: Context, v: String){ c.getSharedPreferences("timesheet", Context.MODE_PRIVATE).edit().putString("NAME",v).apply() }
fun loadOklad(c: Context): String { return c.getSharedPreferences("timesheet", Context.MODE_PRIVATE).getString("OKLAD","")?: "" }
fun saveOklad(c: Context, v: String){ c.getSharedPreferences("timesheet", Context.MODE_PRIVATE).edit().putString("OKLAD",v).apply() }
fun loadApg(c: Context): String { return c.getSharedPreferences("timesheet", Context.MODE_PRIVATE).getString("APG","0")?: "0" }
fun saveApg(c: Context, v: String){ c.getSharedPreferences("timesheet", Context.MODE_PRIVATE).edit().putString("APG",v).apply() }
fun loadPrem(c: Context, k: String): String { return c.getSharedPreferences("timesheet", Context.MODE_PRIVATE).getString("PREM_$k","")?: "" }
fun savePrem(c: Context, k: String, v: String){ c.getSharedPreferences("timesheet", Context.MODE_PRIVATE).edit().putString("PREM_$k",v).apply() }
fun loadMap(c: Context, key: String): Map<String,String> {
    val s = c.getSharedPreferences("timesheet", Context.MODE_PRIVATE).getString(key,null)?: return emptyMap()
    return try{
        val o = JSONObject(s)
        val m = mutableMapOf<String,String>()
        val keys = o.keys()
        while(keys.hasNext()){ val kk = keys.next();

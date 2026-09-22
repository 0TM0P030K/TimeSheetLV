package lv.timesheet
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.YearMonth
import kotlin.math.max
import kotlin.math.min

fun sDay(d: DayOfWeek):String{
 return when(d){DayOfWeek.MONDAY->"Pr" DayOfWeek.TUESDAY->"Ot" DayOfWeek.WEDNESDAY->"Tr" DayOfWeek.THURSDAY->"Ce" DayOfWeek.FRIDAY->"Pk" DayOfWeek.SATURDAY->"Se" else->"Sv"}
}
fun loadS(c:Context,k:String,def:String):String{ return c.getSharedPreferences("a",0).getString(k,def)?:def }
fun saveS(c:Context,k:String,v:String){ c.getSharedPreferences("a",0).edit().putString(k,v).apply() }
fun loadMap(c:Context,k:String):Map<String,String>{
 val s=c.getSharedPreferences("a",0).getString(k,null)?:return emptyMap()
 try{ val o=JSONObject(s); val m=mutableMapOf<String,String>(); val it=o.keys(); while(it.hasNext()){val kk=it.next(); m[kk]=o.getString(kk)}; return m }catch(e:Exception){return emptyMap()}
}
fun saveMap(c:Context,k:String,m:Map<String,String>){ val o=JSONObject(); for(e in m){o.put(e.key,e.value)}; c.getSharedPreferences("a",0).edit().putString(k,o.toString()).apply() }

class MainActivity: ComponentActivity(){
 override fun onCreate(b:Bundle?){ super.onCreate(b); setContent{ MaterialTheme{ App() } } }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(){
 val ctx=LocalContext.current
 var ym by remember{mutableStateOf(YearMonth.now())}
 val mk=ym.year.toString()+"-"+ym.monthValue
 var normS by remember(mk){mutableStateOf(loadS(ctx,"N"+mk,"168"))}
 var okladS by remember{mutableStateOf(loadS(ctx,"O","1450"))}
 var apgS by remember{mutableStateOf(loadS(ctx,"A","0"))}
 var neaplS by remember{mutableStateOf(loadS(ctx,"NE","0"))}
 var premS by remember(mk){mutableStateOf(loadS(ctx,"P"+mk,"30"))}
 var days by remember(mk){mutableStateOf(loadMap(ctx,"D"+mk))}
 var nights by remember(mk){mutableStateOf(loadMap(ctx,"N2"+mk))}
 var show by remember{mutableStateOf(false)}
 val norm=normS.toIntOrNull()?:168

 Scaffold(topBar={TopAppBar(title={Text(ym.monthValue.toString()+"."+ym.year.toString()+" N"+norm.toString())}, actions={Button(onClick={ym=ym.minusMonths(1)}){Text("<")}; Spacer(Modifier.width(4.dp)); Button(onClick={ym=ym.plusMonths(1)}){Text(">")}})}){pad->
  Column(Modifier.fillMaxSize().padding(pad).padding(8.dp).verticalScroll(rememberScrollState())){
   Row{ OutlinedTextField(value=okladS,onValueChange={okladS=it;saveS(ctx,"O",it)},label={Text("Oklads")},modifier=Modifier.weight(1f)); Spacer(Modifier.width(4.dp)); OutlinedTextField(value=neaplS,onValueChange={neaplS=it;saveS(ctx,"NE",it)},label={Text("Neapl")},modifier=Modifier.width(80.dp)); Spacer(Modifier.width(4.dp)); OutlinedTextField(value=apgS,onValueChange={apgS=it;saveS(ctx,"A",it)},label={Text("Apg")},modifier=Modifier.width(70.dp)) }
   Row{ OutlinedTextField(value=normS,onValueChange={normS=it;saveS(ctx,"N"+mk,it)},label={Text("Norma")},modifier=Modifier.weight(1f)); Spacer(Modifier.width(4.dp)); OutlinedTextField(value=premS,onValueChange={premS=it;saveS(ctx,"P"+mk,it)},label={Text("Prem")},modifier=Modifier.weight(1f)) }
   Spacer(Modifier.height(8.dp))
   val totD=days.values.mapNotNull{it.toDoubleOrNull()}.sum()
   val totN=nights.values.mapNotNull{it.toDoubleOrNull()}.sum()
   val virs=max(0.0,totD-norm.toDouble())
   val lidz=min(totD,norm.toDouble())
   for(d in 1..ym.lengthOfMonth()){
    val dow=ym.atDay(d).dayOfWeek
    val bg=if(dow==DayOfWeek.SATURDAY||dow==DayOfWeek.SUNDAY) Color(0xFFFFEBEE) else Color.White
    val dv=days[d.toString()]?:""
    val nv=nights[d.toString()]?:""
    Row(Modifier.fillMaxWidth().background(bg).padding(vertical=2.dp)){ Text(d.toString()+" "+sDay(dow),Modifier.width(60.dp),fontWeight=FontWeight.Bold); OutlinedTextField(value=dv,onValueChange={v->val m=days.toMutableMap();m[d.toString()]=v;days=m;saveMap(ctx,"D"+mk,m)},modifier=Modifier.width(75.dp).padding(end=4.dp),singleLine=true); OutlinedTextField(value=nv,onValueChange={v->val m=nights.toMutableMap();m[d.toString()]=v;nights=m;saveMap(ctx,"N2"+mk,m)},modifier=Modifier.width(75.dp),singleLine=true) }
   }
   Card(Modifier.fillMaxWidth().padding(top=8.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFFE8F5E9))){ Column(Modifier.padding(8.dp)){ Text("D "+totD.toString()+" / "+norm.toString(),fontWeight=FontWeight.Bold); if(virs>0) Text("VIRS "+virs.toString(),color=Color.Red); Text("Nakts "+totN.toString()) } }
   Button(onClick={show=true},Modifier.fillMaxWidth().padding(top=8.dp)){Text("ALGA")}
   if(show){
    val oklad=okladS.toDoubleOrNull()?:0.0
    val likme=if(norm>0) oklad/norm.toDouble() else 0.0
    val apgC=apgS.toIntOrNull()?:0
    val neapl=neaplS.toDoubleOrNull()?:0.0
    val base=lidz*likme
    val vP=virs*likme*2.0
    val nP=totN*likme*0.5
    val pP=premS.toDoubleOrNull()?:0.0
    val pN=base*pP/100.0
    val pV=vP*pP/100.0
    val bruto=base+vP+nP+pN+pV
    val vsaoi=bruto*0.105
    val atv=apgC*250.0
    val tmp=bruto-vsaoi-neapl-atv
    val apliek=max(0.0,tmp)
    val iin=apliek*0.255
    val neto=bruto-vsaoi-iin
    Dialog(onDismissRequest={show=false}){ Card(Modifier.fillMaxWidth().padding(16.dp)){ Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())){ Text("Aprekins",fontWeight=FontWeight.Bold); Text("Likme "+String.format("%.2f",likme)); Text("Base "+String.format("%.2f",base)); Text("Virs "+String.format("%.2f",vP)); Text("Nakts "+String.format("%.2f",nP)); Text("BRUTO "+String.format("%.2f",bruto),fontWeight=FontWeight.Bold); Text("VSAOI -"+String.format("%.2f",vsaoi)); Text("IIN -"+String.format("%.2f",iin)); Divider(Modifier.padding(vertical=8.dp)); Text("NETO "+String.format("%.2f",neto),fontWeight=FontWeight.Bold,color=Color(0xFF2E7D32)); Button(onClick={show=false},Modifier.fillMaxWidth()){Text("OK")} } } }
   }
  }
 }
}

package systems.carson

import com.google.gson.annotations.SerializedName
import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.all
import org.bson.Document
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import spark.Spark.*


import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.api.events.EventDispatcher
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.util.EmbedBuilder
import sx.blah.discord.util.RequestBuffer


class Main{
    companion object {
        @JvmStatic
        fun main(args :Array<String>){
            mainf(args)
        }
    }
}

lateinit var client : IDiscordClient
fun mainf(args: Array<String>) {
    startRest()
    client = Utils.buildClient()
    client.login()
    client.dispatcher.registerKotlinListener { event :MessageReceivedEvent ->
        // event:MessageReceivedEvent ->
        println("${event.author.name} : ${event.message.content}")

        val id = event.channel.longID
        if (event.message.content == "fcps-sub") {
            println("called fcps-sub")
            val exists = (mongo.getCollection().find(all("_id", id)).first() ?: Document("_id",-1L))["_id"] as Long
            if (exists != -1L) {
                RequestBuffer.request { event.channel.sendMessage("You seem to be already registered. Try using fpcs-unsub to unsubscribe") }
            } else {
                RequestBuffer.request { event.channel.sendMessage("Adding this channel to the database") }
                mongo.getCollection().insertOne(Document("_id", id))
            }
        } else if (event.message.content == "fcps-unsub") {
            println("called fcps-unsub")
            val contains = mongo.getCollection().find(all("_id",id)).first() != null
            if (contains) {
                mongo.getCollection().deleteOne(all("_id", id))
                RequestBuffer.request { event.channel.sendMessage("Removed from database") }
            } else {
                RequestBuffer.request { event.channel.sendMessage("Already removed from database") }

            }


        }
        Unit
    }

}
fun EventDispatcher.registerKotlinListener(x :(MessageReceivedEvent) -> Unit){
    this.registerListener(object {
        @EventSubscriber
        fun messageEvent(e :MessageReceivedEvent){
            x(e)
        }
    })
}


fun startRest() {
    put("/fcps"){req,_ ->
        val body = req.body()
        val obj = Utils.gson.fromJson(body,JsonObject::class.java)
        if(obj.status == Status.ERROR){
            halt(500,"Unable to parse json, or got status = ERROR")
        }
        val embed = EmbedBuilder()
            .appendField("Students",obj.students.toStatusString(),false)
            .appendField("School Offices",obj.schoolOffices.toStatusString(),false)
            .appendField("Central Offices",obj.centralOffices.toStatusString(),false)
            .withTitle("FCPS School cancellation broadcast")
            .withDesc(if(obj.humanVerified) "This data has been human verified" else "This data has not been human verified")
            .withFooterText("To unsubscribe from these notifications, use fcps-unsub")
            .build()
        var i = 0
        mongo.getCollection()
            .find()
            .map { i++;it }
            .map { it["_id"] }
            .map { it as Long }
            .map { client.getChannelByID(it) }
            .forEach { RequestBuffer.request { it.sendMessage(embed) } }

        """
{
    messagesSent = $i
}
        """.trimIndent()
    }
}
fun Int.toStatusString() :String{
    return when(this){
        0 -> "As normal"
        1 -> "2 hour delay"
        2 -> "Cancelled"
        else -> "Unknown status code :$this"
    }
}
/*
{"status":"(enum success || error)",
"human":(bool has a human verified this?),
"students":(int student status),
"schoolOffices":(int school offices status),
"centralOffices":(int central offices status)}
 */
data class JsonObject(
    val status :Status = Status.ERROR,
    @SerializedName("human")
    val humanVerified :Boolean = false,
    val students :Int = -1,
    val schoolOffices :Int = -1,
    val centralOffices :Int = -1)

enum class Status{SUCCESS, ERROR}

val mongo = Mongo()
class Mongo {
    private val client: MongoClient = MongoClient("192.168.1.203:27017")
    private val db: MongoDatabase = client.getDatabase("fcps-bot")
    fun getCollection(): MongoCollection<Document> = db.getCollection("sub")
}
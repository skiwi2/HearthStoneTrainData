import groovy.json.JsonSlurper
import sun.nio.cs.UTF_8

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * @author Frank van Heeswijk
 */
class Main {
    static String cardDataUrl = "https://api.hearthstonejson.com/v1/latest/enUS/cards.collectible.json"
    static Path targetPath = Paths.get(System.getProperty("user.home"), "HearthStoneTrainData", "data.txt")
    static long targetFileSize = 1024L * 1024L

    static void main(String[] args) {
        saveToTargetFile(convertToOneLiners(retrieveCardData()))
    }

    static Object retrieveCardData() {
        def jsonSlurper = new JsonSlurper()
        def connection = (HttpURLConnection)new URL(cardDataUrl).openConnection()
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:50.0) Gecko/20100101 Firefox/50.0")    // I'm not a bot
        jsonSlurper.parse(connection.inputStream, StandardCharsets.UTF_8.name())
    }

    static List<String> convertToOneLiners(List<Object> cards) {
        def list = []
        cards.each { card ->
            if (card.collectible) {
                list.add(convertToOneLiner(card))
            }
        }
        list
    }

    static String convertToOneLiner(Object cardJson) {
        def type = cardJson.type
        def rarity = cardJson.rarity ?: "Token"
        def race = cardJson.race ?: "None"
        def originalPlayerClass = cardJson.playerClass ?: "Neutral"
        def classes = cardJson.classes ?: []
        def playerClass = (classes ?: [originalPlayerClass]).join(" ")
        def name = cardJson.name
        def cost = cardJson.cost
        def attack = cardJson.attack
        def health = cardJson.health
        def durability = cardJson.durability
        def originalText = cardJson.text ?: ""
        def collectionText = cardJson.collectionText ?: ""
        def text = collectionText ?: originalText
        def set = cardJson.set
        switch (type) {
            case "MINION":
                return "${text} | ${cost} | ${attack}/${health} | ${name} | ${race} | ${playerClass} | ${rarity} | ${type} | ${set}"
            case "SPELL":
                return "${text} | ${cost} | ${name} | ${playerClass} | ${rarity} | ${type} | ${set}"
            case "ENCHANTMENT":
                return "${text} | ${playerClass} | ${type} | ${set}"
            case "WEAPON":
                return "${text} | ${attack}/${durability} | ${cost} | ${name} | ${playerClass} | ${rarity} | ${type} | ${set}"
            case "HERO":
                return "${health} | ${name} | ${playerClass} | ${rarity} | ${type} | ${set}"
            case "HERO POWER":
                return "${text} | ${cost} | ${name} | ${playerClass} | ${rarity} | ${type} | ${set}"
            default:
                throw new IllegalArgumentException("Unknown type: ${type} in ${cardJson}")
        }
    }

    static void saveToTargetFile(List<String> cardOneLiners) {
        def copyCardOneLiners = new ArrayList(cardOneLiners)
        Files.deleteIfExists(targetPath)
        Files.createFile(targetPath)
        def writer = Files.newBufferedWriter(targetPath, StandardCharsets.UTF_8, StandardOpenOption.APPEND)
        writer.withCloseable {
            while (targetPath.size() < targetFileSize) {
                println("Progress: ${targetPath.size()}/${targetFileSize}")
                Collections.shuffle(copyCardOneLiners)
                copyCardOneLiners.each { line ->
                    writer.append(line)
                    writer.newLine()
                    writer.newLine()
                }
            }
        }
    }
}

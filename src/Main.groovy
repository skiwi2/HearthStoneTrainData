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

        cost = convertResource(cost ?: 0)
        attack = convertResource(attack ?: 0)
        health = convertResource(health ?: 0)
        durability = convertResource(durability ?: 0)
        text = convertText(text)

        switch (type) {
            case "MINION":
                return formatOneLiner(cost, type, attack, health, rarity, playerClass, race, text, name, set)
            case "SPELL":
                return formatOneLiner(cost, type, rarity, playerClass, text, name, set)
            case "ENCHANTMENT":
                return formatOneLiner(type, playerClass, text, set)
            case "WEAPON":
                return formatOneLiner(cost, type, attack, durability, rarity, playerClass, text, name, set)
            case "HERO":
                return formatOneLiner(type, health, rarity, playerClass, name, set)
            case "HERO POWER":
                return formatOneLiner(cost, type, rarity, playerClass, text, name, set)
            default:
                throw new IllegalArgumentException("Unknown type: ${type} in ${cardJson}")
        }
    }

    static String convertResource(Integer resource) {
        "{" + "X".multiply(resource) + "}"
    }

    static String convertText(String text) {
        def sb = new StringBuilder()
        def digits = ""
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i)
            if (Character.isDigit(ch)) {
                digits += ch
            }
            else {
                if (digits) {
                    sb.append(convertTextResource(digits.toInteger()))
                    digits = ""
                }
                sb.append(ch)
            }
        }
        if (digits) {
            sb.append(convertTextResource(digits.toInteger()))
        }
        sb.toString()
    }

    static String convertTextResource(Integer resource) {
        "{^" + "&".multiply(resource.toInteger()) + "}"
    }

    static String formatOneLiner(Object... arguments) {
        String.join(" | ", arguments.collect { it.toString() })
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

import groovy.json.JsonSlurper

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * @author Frank van Heeswijk
 */
class Main {
    static String cardDataUrl = "http://hearthstonejson.com/json/AllSets.json"
    static Path targetPath = Paths.get(System.getProperty("user.home"), "HearthStoneTrainData", "data.txt")
    static long targetFileSize = 1024L * 1024L

    static void main(String[] args) {
        saveToTargetFile(convertToOneLiners(splitIntoSets(retrieveCardData())))
    }

    static Object retrieveCardData() {
        JsonSlurper jsonSlurper = new JsonSlurper()
        jsonSlurper.parse(new URL(cardDataUrl), StandardCharsets.UTF_8.name())
    }

    static Map<String, List> splitIntoSets(Object cardsJson) {
        cardsJson.collectEntries { it }
    }

    static List<String> convertToOneLiners(Map<String, List> cardsBySet) {
        def list = []
        cardsBySet.each { set, cards ->
            cards.each { card ->
                if (card.collectible) {
                    list.add(convertToOneLiner(set, card))
                }
            }
        }
        list
    }

    static String convertToOneLiner(String set, Object cardJson) {
        def type = cardJson.type
        def rarity = cardJson.rarity ?: "Token"
        def race = cardJson.race ?: "None"
        def playerClass = cardJson.playerClass ?: "Neutral"
        def name = cardJson.name
        def cost = cardJson.cost
        def attack = cardJson.attack
        def health = cardJson.health
        def durability = cardJson.durability
        def text = cardJson.text ?: ""
        switch (type) {
            case "Minion":
                return "${text} | ${cost} | ${attack}/${health} | ${name} | ${race} | ${playerClass} | ${rarity} | ${type} | ${set}"
            case "Spell":
                return "${text} | ${cost} | ${name} | ${playerClass} | ${rarity} | ${type} | ${set}"
            case "Enchantment":
                return "${text} | ${playerClass} | ${type} | ${set}"
            case "Weapon":
                return "${text} | ${attack}/${durability} | ${cost} | ${name} | ${playerClass} | ${rarity} | ${type} | ${set}"
            case "Hero":
                return "${health} | ${name} | ${playerClass} | ${rarity} | ${type} | ${set}"
            case "Hero Power":
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
                }
            }
        }
    }
}

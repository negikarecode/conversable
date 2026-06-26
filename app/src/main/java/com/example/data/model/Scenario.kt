package com.example.data.model

data class Scenario(
    val id: String,
    val title: String,
    val category: String, // "Dating", "Small Talk", "Networking", "Conflict Resolution"
    val partnerName: String,
    val partnerAvatar: String, // Emoji or icon descriptor
    val partnerPersona: String,
    val scenarioDescription: String,
    val hiddenGoal: String,
    val difficulty: String, // "Easy", "Medium", "Hard"
    val initialMessage: String
)

object ScenarioCatalog {
    val categories = listOf("Dating", "Small Talk", "Networking", "Conflict Resolution")

    val scenarios = listOf(
        // --- DATING ---
        Scenario(
            id = "dating_first_date",
            title = "First Date Coffee Shop",
            category = "Dating",
            partnerName = "Clara",
            partnerAvatar = "M",
            partnerPersona = "Slightly reserved, thoughtful, loves dry humor, but hates intense interrogators. Appreciates when a conversation flows naturally about hobbies rather than resumes.",
            scenarioDescription = "Coffee shop first date. Keep it natural.",
            hiddenGoal = "Find a shared leisure hobby and transition naturally into asking for a second date, without being overly intense or creepy.",
            difficulty = "Easy",
            initialMessage = "Hey, thanks for meeting up. It's so nice to finally grab a drink in person. How's your week been?"
        ),
        Scenario(
            id = "dating_speed_dating",
            title = "High-Speed Dating Mixer",
            category = "Dating",
            partnerName = "Leo",
            partnerAvatar = "L",
            partnerPersona = "Incredibly high-energy, witty, easily distracted by their surroundings. Attracted to unique storytellers or clever visual descriptions. Bothered by boring 'cliché' small talk.",
            scenarioDescription = "Speed dating mixer. 3 minutes on the clock.",
            hiddenGoal = "Hook Leo's interest with a memorable, unique story or joke, and make them want to ask for your phone number.",
            difficulty = "Hard",
            initialMessage = "Whew, that speed buzzer is intense. Hi there! Let's skip the 'what do you do for work' question. Tell me something weird about yourself instead."
        ),

        // --- SMALL TALK ---
        Scenario(
            id = "small_talk_coffee_line",
            title = "The Endless Coffee Line",
            category = "Small Talk",
            partnerName = "Sam",
            partnerAvatar = "F",
            partnerPersona = "Stressed corporate analyst who is checking their watch every 10 seconds. Not originally looking to chat, but easily disarmed by relatable humor about waiting or coffee addiction.",
            scenarioDescription = "Stuck in line together. Break the silence.",
            hiddenGoal = "Disarm Sam's stress and get them to laugh, sharing a mutually funny complaint about modern morning rushes before you reach the register.",
            difficulty = "Easy",
            initialMessage = "(Sighs loudly, looking at the menu board) Man, I think they're harvesting the coffee beans in the back from scratch today."
        ),
        Scenario(
            id = "small_talk_neighbor",
            title = "The Proud Neighbor",
            category = "Small Talk",
            partnerName = "Arthur",
            partnerAvatar = "G",
            partnerPersona = "A retired neighbor in his late 60s who is fiercely proud of his award-winning rosebushes. Can be initially grumpy or suspicious of younger neighbors, but warms up instantly when asked about gardening.",
            scenarioDescription = "Retired neighbor gardening. Ask him for advice.",
            hiddenGoal = "Build neighborly rapport, avoid awkward silence, and naturally ask him for a landscaping or flower gardening tip.",
            difficulty = "Easy",
            initialMessage = "Afternoon. Hot one today, isn't it? Have to keep the lawn watered or it'll dry up in a heartbeat."
        ),

        // --- NETWORKING ---
        Scenario(
            id = "networking_tech_conference",
            title = "Tech Conference Mixer",
            category = "Networking",
            partnerName = "Elena",
            partnerAvatar = "W",
            partnerPersona = "Senior Vice President of Product at a fast-growing tech unicorn. She's polite, highly analytical, and values brevity. Flooded with pushy sales pitches all night. Looking for genuine peer-to-peer intellect.",
            scenarioDescription = "Tech conference mixer. Introduce yourself.",
            hiddenGoal = "Pitch a conversation or project topic, establishing yourself as someone with fresh insights, and secure her promise to connect on LinkedIn or email later.",
            difficulty = "Medium",
            initialMessage = "Hey there, quite a turnout tonight. The panel on generative tech was fascinating, though I felt they missed a key point. What did you think of it?"
        ),
        Scenario(
            id = "networking_office_kitchen",
            title = "Co-working Incubator Kitchen",
            category = "Networking",
            partnerName = "Marcus",
            partnerAvatar = "C",
            partnerPersona = "A busy venture capital scout who receives hundreds of slide decks a day. Extremely friendly, but possesses a laser-sharp radar for people with hidden agendas or desperate financial pitches.",
            scenarioDescription = "Shared office kitchen. Start a chat with Marcus.",
            hiddenGoal = "Establish credibility, exchange info about what you're working on, and get his feedback or a suggestion on who you should speak to next.",
            difficulty = "Medium",
            initialMessage = "Morning! Ah, is the coffee machine acting up again? It seems to have a personality of its own on Mondays."
        ),

        // --- CONFLICT RESOLUTION ---
        Scenario(
            id = "conflict_stolen_lunch",
            title = "The Missing Fridge Items",
            category = "Conflict Resolution",
            partnerName = "Jordan",
            partnerAvatar = "U",
            partnerPersona = "A defensive co-worker who has been caught eating other coworkers' food in the office kitchen before. He has a strong 'it's not a big deal' attitude and deflects blame using jokes or excuses.",
            scenarioDescription = "Office breakroom. Face Jordan about your lunch.",
            hiddenGoal = "Politely, firmly address the stolen food directly, and secure a clear apology and promise to respect labeled food, without starting an explosive corporate HR feud.",
            difficulty = "Hard",
            initialMessage = "Oh, hey! Oh, is this yours? I saw this labeled bowl in the back and thought it was the company-provided leftover catering from yesterday. Honest mistake!"
        ),
        Scenario(
            id = "conflict_loud_neighbor",
            title = "The Late-Night DJ Neighbor",
            category = "Conflict Resolution",
            partnerName = "Zoe",
            partnerAvatar = "N",
            partnerPersona = "An aspiring 23-year-old tech marketer and bedroom DJ who loves throwing weekend parties. She is generally nice but totally oblivious to how loud bass vibrations travel through apartment floorboards.",
            scenarioDescription = "Late-night hallway. Talk to Zoe about the noise.",
            hiddenGoal = "Express your noise complaint constructively, establish a noise boundary for late nights, and secure mutual respect without causing permanent neighborly tension.",
            difficulty = "Medium",
            initialMessage = "Hey! (Turns down bass controller slightly) Oh my gosh, are we being way too loud? We're just having a small housewarming session!"
        )
    )
}

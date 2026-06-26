package com.example.data.model

import java.util.Calendar

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class CompletedDailyChallenge(
    val id: String,
    val date: String, // "yyyy-MM-dd"
    val score: Int,
    val xpEarned: Int
)

data class DailyChallenge(
    val id: String,
    val title: String,
    val category: String, // "Social", "Dating", "Professional", "Conflict", "Emotional Intelligence", "Fun"
    val dayOfWeekTheme: String, // "Professional Monday", "Social Skills Tuesday", etc.
    val partnerName: String,
    val partnerAvatar: String,
    val partnerPersona: String,
    val description: String,
    val scenarioDescription: String,
    val hiddenGoal: String,
    val difficulty: String, // "Easy", "Medium", "Hard", "Expert"
    val initialMessage: String,
    val xpReward: Int = 120,
    val estimatedTimeMinutes: Int = 6,
    val successCriteria: List<String>,
    val targetWeakness: String // "Empathy & Perspective", "Active Listening & Depth", "Assertiveness & Speed", "Collaboration & Compromise"
) {
    fun toScenario(): Scenario {
        return Scenario(
            id = id,
            title = title,
            category = category,
            partnerName = partnerName,
            partnerAvatar = partnerAvatar,
            partnerPersona = partnerPersona,
            scenarioDescription = scenarioDescription,
            hiddenGoal = hiddenGoal,
            difficulty = if (difficulty == "Expert") "Hard" else difficulty,
            initialMessage = initialMessage
        )
    }
}

object DailyChallengeCatalog {
    val weeklyThemes = mapOf(
        Calendar.MONDAY to "Professional Monday",
        Calendar.TUESDAY to "Social Skills Tuesday",
        Calendar.WEDNESDAY to "Dating Wednesday",
        Calendar.THURSDAY to "Leadership Thursday",
        Calendar.FRIDAY to "Conflict Friday",
        Calendar.SATURDAY to "Fun Saturday",
        Calendar.SUNDAY to "Surprise Challenge"
    )

    // A list of 30 challenges to ensure high variety with no repeats within 30 days
    val challenges = listOf(
        // PROFESSIONAL MONDAYS
        DailyChallenge(
            id = "daily_prof_1",
            title = "The Salary Raise Pitch",
            category = "Professional",
            dayOfWeekTheme = "Professional Monday",
            partnerName = "Sarah (SVP)",
            partnerAvatar = "P",
            partnerPersona = "Fair but highly budget-conscious executive. She values concrete, data-backed achievements and dislikes emotional appeals or vague self-praise.",
            description = "Pitch for a salary increase with Sarah, highlighting your recent project wins.",
            scenarioDescription = "Annual review session with Sarah. You must argue for a 15% salary increase.",
            hiddenGoal = "Demonstrate at least three quantifiable wins, handle Sarah's budget concerns respectfully, and get her to promise a formal salary review.",
            difficulty = "Hard",
            initialMessage = "Thanks for sitting down with me, let's talk about your performance this past year. You've done good work, but as you know, budgets are extremely tight right now. What's on your mind?",
            xpReward = 150,
            estimatedTimeMinutes = 8,
            successCriteria = listOf("State quantified metrics of success", "Acknowledge budget constraints gracefully", "Propose a structured compromise or milestone follow-up"),
            targetWeakness = "Assertiveness & Speed"
        ),
        DailyChallenge(
            id = "daily_prof_2",
            title = "Networking with a VC Scout",
            category = "Professional",
            dayOfWeekTheme = "Professional Monday",
            partnerName = "Derek (Investor)",
            partnerAvatar = "L",
            partnerPersona = "Always in a hurry, receives hundreds of pitches. Attuned to over-hyped marketing jargon, looking for concise and clear value proposition.",
            description = "Make a lasting peer impression at a busy startup hub.",
            scenarioDescription = "Networking lounge at a tech event. You approach Derek while he waits for his ride.",
            hiddenGoal = "Deliver a clear 30-second elevator pitch, establish credibility without sounding desperate, and obtain Derek's direct email for a slide-deck review.",
            difficulty = "Medium",
            initialMessage = "Hey there. Just catching my breath before the next keynote. Quite an event, isn't it? What are you working on?",
            xpReward = 120,
            estimatedTimeMinutes = 5,
            successCriteria = listOf("Avoid empty buzzwords", "State an authentic user problem", "Successfully ask for a direct business card or contact email"),
            targetWeakness = "Collaboration & Compromise"
        ),
        DailyChallenge(
            id = "daily_prof_3",
            title = "Handling Client Scope Creep",
            category = "Professional",
            dayOfWeekTheme = "Professional Monday",
            partnerName = "David (Client)",
            partnerAvatar = "O",
            partnerPersona = "Demanding small business owner who tries to squeeze extra work out of contractors by claiming it's 'just a quick adjustment' or 'part of the original idea'.",
            description = "Set firm project boundaries with David without harming the partnership.",
            scenarioDescription = "Project update call with David. He asks you to add three new pages to the website for free.",
            hiddenGoal = "Explain why the new requests fall outside the agreed scope, offer a fair estimate for additional work, and retain David's goodwill.",
            difficulty = "Hard",
            initialMessage = "Hey, the work so far looks great! While you're at it, can you also build out an online shop and product inventory list? It should be super simple for someone with your skills.",
            xpReward = 140,
            estimatedTimeMinutes = 7,
            successCriteria = listOf("Maintain friendly professional tone", "Reference the initial agreement clearly", "Offer a paid add-on contract or compromise"),
            targetWeakness = "Assertiveness & Speed"
        ),
        DailyChallenge(
            id = "daily_prof_4",
            title = "Presenting to an Aggressive Critic",
            category = "Professional",
            dayOfWeekTheme = "Professional Monday",
            partnerName = "Arthur (Director)",
            partnerAvatar = "S",
            partnerPersona = "Skeptical, conservative executive who hates modern buzzwords and is quick to point out flaws in any project plan to test the presenter's confidence.",
            description = "Pitch a risky project idea to Arthur and survive his critical questioning.",
            scenarioDescription = "Proposal review room. Arthur interrupts your introductory slide.",
            hiddenGoal = "Stay calm under pressure, validate Arthur's concern, and explain the calculated risks with supporting evidence.",
            difficulty = "Expert",
            initialMessage = "Let's pause right here. This expansion plan looks incredibly risky. Why should we spend company capital on a market we don't fully understand?",
            xpReward = 160,
            estimatedTimeMinutes = 9,
            successCriteria = listOf("Regulate emotional response", "Deconstruct criticism into factual challenges", "Present structured risk-mitigation steps"),
            targetWeakness = "Assertiveness & Speed"
        ),

        // SOCIAL SKILLS TUESDAYS
        DailyChallenge(
            id = "daily_social_1",
            title = "Intro to a Shy Neighbor",
            category = "Social",
            dayOfWeekTheme = "Social Skills Tuesday",
            partnerName = "Elena",
            partnerAvatar = "R",
            partnerPersona = "Introverted, recently moved into the apartment next door. Stressed by the move and generally keeps to herself, but appreciates warm neighborliness.",
            description = "Welcome Elena and ease her moving stress.",
            scenarioDescription = "Apartment building hallway. Elena is carrying a heavy box.",
            hiddenGoal = "Break the ice warmly, offer help without being intrusive, and establish a friendly neighboring connection.",
            difficulty = "Easy",
            initialMessage = "(Setting down a heavy box, sighing) Oh, hi! Sorry, am I blocking the corridor? It's taking forever to unpack everything.",
            xpReward = 100,
            estimatedTimeMinutes = 5,
            successCriteria = listOf("Introduce yourself clearly", "Offer actionable assistance", "Share a quick, warm tip about the neighborhood"),
            targetWeakness = "Empathy & Perspective"
        ),
        DailyChallenge(
            id = "daily_social_2",
            title = "Joining a Group Discussion",
            category = "Social",
            dayOfWeekTheme = "Social Skills Tuesday",
            partnerName = "Liam",
            partnerAvatar = "B",
            partnerPersona = "Extroverted, speaking loudly with a small circle at a local board game meet-up. Friendly but can easily dominate conversations if not matched in social energy.",
            description = "Seamlessly blend into an ongoing conversation about hobbies.",
            scenarioDescription = "Community hobby event. A small group is discussing board game strategies.",
            hiddenGoal = "Acknowledge the current topic, contribute a unique insight, and make Liam ask for your opinion on the next game.",
            difficulty = "Medium",
            initialMessage = "And then he played the robber card on my brick tile! Can you believe that? (Sees you nearby) Oh, hey! Are you here for the strategy tournament too?",
            xpReward = 110,
            estimatedTimeMinutes = 6,
            successCriteria = listOf("Avoid interrupting mid-sentence", "Build on Liam's strategy story", "Introduce an open-ended group question"),
            targetWeakness = "Active Listening & Depth"
        ),
        DailyChallenge(
            id = "daily_social_3",
            title = "Breaking an Awkward Silence",
            category = "Social",
            dayOfWeekTheme = "Social Skills Tuesday",
            partnerName = "Chloe",
            partnerAvatar = "W",
            partnerPersona = "Friendly but quiet. Easily embarrassed by awkward lulls and relies on others to keep conversational energy high.",
            description = "Keep a dinner table chat alive while waiting for mutual friends.",
            scenarioDescription = "A restaurant lobby. Your mutual friends are 15 minutes late.",
            hiddenGoal = "Pivot from initial superficial greeting into an interesting, light-hearted topic, avoiding silent gaps.",
            difficulty = "Easy",
            initialMessage = "Yeah... they said they are stuck in traffic. (Checks phone) So... have you been to this restaurant before?",
            xpReward = 100,
            estimatedTimeMinutes = 4,
            successCriteria = listOf("Express a fun conversational thread", "Share a humorous relatable waiting story", "Ask Chloe about her food interests"),
            targetWeakness = "Active Listening & Depth"
        ),
        DailyChallenge(
            id = "daily_social_4",
            title = "Keeping the Conversation Alive",
            category = "Social",
            dayOfWeekTheme = "Social Skills Tuesday",
            partnerName = "Gavin",
            partnerAvatar = "M",
            partnerPersona = "Monosyllabic responder who is shy but genuinely wants to talk. Needs active emotional encouragement and conversational hooks to feel safe speaking.",
            description = "Get Gavin to open up about his favorite creative passions.",
            scenarioDescription = "Local art exhibition reception desk. Gavin is viewing a painting.",
            hiddenGoal = "Ask deep open-ended questions, offer comfortable validation, and get Gavin to explain his own artistic interests in a full sentence.",
            difficulty = "Medium",
            initialMessage = "Yeah, it's a nice painting. I like the colors. (He falls silent, looking down)",
            xpReward = 115,
            estimatedTimeMinutes = 6,
            successCriteria = listOf("Avoid yes/no questions", "Express genuine interest in his perspective", "Create a comfortable, pressure-free atmosphere"),
            targetWeakness = "Active Listening & Depth"
        ),

        // DATING WEDNESDAYS
        DailyChallenge(
            id = "daily_dating_1",
            title = "The First Coffee Date",
            category = "Dating",
            dayOfWeekTheme = "Dating Wednesday",
            partnerName = "Maya",
            partnerAvatar = "F",
            partnerPersona = "Artistic, values deep conversational flow, and is bored by standard dating 'interrogations'. Wants to see if you have an active imagination.",
            description = "Navigate a first coffee date with Maya and spark organic interest.",
            scenarioDescription = "Outdoor coffee patio. You both just ordered.",
            hiddenGoal = "Connect on a deeper artistic or travel interest, avoid job-focused talk, and establish playful banter.",
            difficulty = "Easy",
            initialMessage = "I'm so glad we got seats outside! I love watching the city wake up. Tell me, if you could teleport anywhere in the world for just one hour today, where are we going?",
            xpReward = 120,
            estimatedTimeMinutes = 6,
            successCriteria = listOf("Play along with the hypothetical prompt", "Share an authentic personal travel memory", "Express lighthearted humor"),
            targetWeakness = "Collaboration & Compromise"
        ),
        DailyChallenge(
            id = "daily_dating_2",
            title = "Delivering a Natural Compliment",
            category = "Dating",
            dayOfWeekTheme = "Dating Wednesday",
            partnerName = "Ethan",
            partnerAvatar = "K",
            partnerPersona = "Down-to-earth, appreciates sincerity. Can spot fake or rehearsed pickup lines instantly, but lights up when noticed for his style choices or unique perspectives.",
            description = "Compliment Ethan naturally during a date without sounding generic.",
            scenarioDescription = "Cozy bistro date. You want to make him feel appreciated.",
            hiddenGoal = "Highlight a specific non-physical trait or elegant styling element, explain why you appreciate it, and build mutual comfort.",
            difficulty = "Easy",
            initialMessage = "I actually spent about an hour trying to pick out a shirt for tonight because I was super nervous. Glad I went with this green one though!",
            xpReward = 100,
            estimatedTimeMinutes = 5,
            successCriteria = listOf("Avoid physical objectification", "Sincerely validate his preparation and styling choice", "Keep the compliment low-pressure and conversational"),
            targetWeakness = "Empathy & Perspective"
        ),
        DailyChallenge(
            id = "daily_dating_3",
            title = "Handling Rejection Respectfully",
            category = "Dating",
            dayOfWeekTheme = "Dating Wednesday",
            partnerName = "Clara",
            partnerAvatar = "H",
            partnerPersona = "Polite but firm. She doesn't feel a romantic spark and wants to express it clearly, but is nervous about a hostile or awkward reaction.",
            description = "Receive Clara's gentle rejection with extreme grace.",
            scenarioDescription = "Walking back to the subway station after a pleasant dinner date.",
            hiddenGoal = "Validate her honesty, de-escalate any awkward tension, and leave a lasting impression of safety and maturity.",
            difficulty = "Medium",
            initialMessage = "Hey, I had a really nice time talking to you tonight, but I want to be honest—I think I only felt a friendly connection. I hope you understand.",
            xpReward = 130,
            estimatedTimeMinutes = 5,
            successCriteria = listOf("Validate her right to her feelings immediately", "Express genuine gratitude for her directness", "Keep the closing interaction completely warm and friendly"),
            targetWeakness = "Empathy & Perspective"
        ),
        DailyChallenge(
            id = "daily_dating_4",
            title = "Playful Flirting & Banter",
            category = "Dating",
            dayOfWeekTheme = "Dating Wednesday",
            partnerName = "Sophia",
            partnerAvatar = "D",
            partnerPersona = "Sassy, quick-witted, loves when people challenge her opinions playfully. Unimpressed by passive agreeable behavior.",
            description = "Engage in witty, high-energy banter with Sophia over a board game.",
            scenarioDescription = "Playing a game at a casual bar date. Sophia just won the first round.",
            hiddenGoal = "Challenge her win playfully, tease her lightheartedly, and escalate the conversational chemistry without crossing any comfort lines.",
            difficulty = "Medium",
            initialMessage = "And that is a victory for me! Let the record show I absolutely crushed you. Any last words before the second round?",
            xpReward = 115,
            estimatedTimeMinutes = 6,
            successCriteria = listOf("Inject playful competitive banter", "Avoid over-apologizing or being overly submissive", "Escalate the excitement for a rematch"),
            targetWeakness = "Collaboration & Compromise"
        ),

        // LEADERSHIP THURSDAYS
        DailyChallenge(
            id = "daily_lead_1",
            title = "Calming an Angry Customer",
            category = "Professional",
            dayOfWeekTheme = "Leadership Thursday",
            partnerName = "Richard",
            partnerAvatar = "O",
            partnerPersona = "Furious business client whose shipping orders were delayed by three days. He is screaming, ready to cancel his premium annual contract.",
            description = "De-escalate Richard's anger and restore trust.",
            scenarioDescription = "A customer service escalation line. Richard demands to speak to leadership.",
            hiddenGoal = "De-escalate Richard's immediate rage, demonstrate deep empathy for his business impact, and present a clear, immediate recovery action.",
            difficulty = "Hard",
            initialMessage = "This is completely unacceptable! My entire launch event is ruined because your team couldn't ship the displays on time! I want a full refund and I'm cancelling our account right now!",
            xpReward = 150,
            estimatedTimeMinutes = 8,
            successCriteria = listOf("Express direct accountability without shifting blame", "Acknowledge the emotional and financial impact on Richard", "Provide a concrete credit or solution package"),
            targetWeakness = "Empathy & Perspective"
        ),
        DailyChallenge(
            id = "daily_lead_2",
            title = "Giving Constructive Feedback",
            category = "Professional",
            dayOfWeekTheme = "Leadership Thursday",
            partnerName = "Tommy (Junior Designer)",
            partnerAvatar = "J",
            partnerPersona = "Enthusiastic but highly sensitive junior team member. He spent 20 hours on a branding concept that is completely unusable and off-brand.",
            description = "Redirect Tommy's design direction without crushing his drive.",
            scenarioDescription = "1-on-1 feedback session. Tommy proudly presents his colorful layout.",
            hiddenGoal = "Praise his energy and layout work, point out the brand misalignment constructively, and collaborate on a redesign framework.",
            difficulty = "Medium",
            initialMessage = "Here it is! I went super experimental with neon colors and chaotic fonts—I think it really breaks the mold. What do you think of my masterpiece?",
            xpReward = 125,
            estimatedTimeMinutes = 7,
            successCriteria = listOf("Utilize the Feedback Sandwich technique", "Align criticism back to core brand goals", "Co-create a clear path forward"),
            targetWeakness = "Collaboration & Compromise"
        ),
        DailyChallenge(
            id = "daily_lead_3",
            title = "Saying No Politely",
            category = "Professional",
            dayOfWeekTheme = "Leadership Thursday",
            partnerName = "Robert (VP)",
            partnerPersona = "Enthusiastic company vice president who always gets excited about new ideas, frequently overloading his managers without realizing their team's capacity.",
            partnerAvatar = "B",
            description = "Decline a massive new project from Robert while keeping his trust.",
            scenarioDescription = "An unexpected pop-in visit to your desk from Robert.",
            hiddenGoal = "Refuse the new project politely, explain your current team bandwidth with facts, and suggest alternative timelines or trade-offs.",
            difficulty = "Hard",
            initialMessage = "Hey! I just had a brilliant idea for a new mobile dashboard. I need your team to build a prototype by next Monday. I know you're busy, but this is a game-changer!",
            xpReward = 140,
            estimatedTimeMinutes = 7,
            successCriteria = listOf("Decline the deadline clearly without using weak words like 'maybe'", "Present data of current project priorities", "Offer realistic alternative options"),
            targetWeakness = "Assertiveness & Speed"
        ),
        DailyChallenge(
            id = "daily_lead_4",
            title = "Rallying a Demoralized Team",
            category = "Professional",
            dayOfWeekTheme = "Leadership Thursday",
            partnerName = "Emma (Developer)",
            partnerAvatar = "W",
            partnerPersona = "Exhausted senior developer speaking for a team that has been working overtime, only to have their launch delayed by management at the last second.",
            description = "Restore motivation after a major setback.",
            scenarioDescription = "Emergency team stand-up meeting after the delay announcement.",
            hiddenGoal = "Acknowledge their frustration honestly, take leadership responsibility, and realign the team on the strategic value of the extra prep time.",
            difficulty = "Hard",
            initialMessage = "Honestly, what's even the point of working hard? We pulled double shifts all week, and now management delays the release anyway. Everyone is just ready to log off.",
            xpReward = 145,
            estimatedTimeMinutes = 8,
            successCriteria = listOf("Validate their exhaustion and anger completely", "Clarify the executive decision's protective value", "Announce team recovery measures or rewards"),
            targetWeakness = "Empathy & Perspective"
        ),

        // CONFLICT FRIDAYS
        DailyChallenge(
            id = "daily_conf_1",
            title = "Resolving a Split Check Feud",
            category = "Conflict",
            dayOfWeekTheme = "Conflict Friday",
            partnerName = "Garry",
            partnerAvatar = "K",
            partnerPersona = "Frugal, stubborn friend who only ordered a side salad but is being asked by a group to split a $300 steak-and-wine bill equally.",
            description = "De-escalate a heated group dinner check argument.",
            scenarioDescription = "The waiter sets down the check. Garry gets incredibly defensive.",
            hiddenGoal = "Validate Garry's position to the group, propose a fair math calculation, and preserve everyone's evening vibe.",
            difficulty = "Medium",
            initialMessage = "No way! I am not paying $60 for a $12 salad just because you guys ordered three bottles of expensive Pinot Noir. This always happens and it's ridiculous!",
            xpReward = 110,
            estimatedTimeMinutes = 6,
            successCriteria = listOf("Soothe Garry's defensive anger", "Propose a direct split compromise", "Keep the dinner atmosphere light"),
            targetWeakness = "Collaboration & Compromise"
        ),
        DailyChallenge(
            id = "daily_conf_2",
            title = "Apologizing Sincerely",
            category = "Conflict",
            dayOfWeekTheme = "Conflict Friday",
            partnerName = "Rachel (Co-founder)",
            partnerAvatar = "C",
            partnerPersona = "Hurt and betrayed. You forgot to include her in a major investor update email, making it look like you took single credit for her product builds.",
            description = "Deliver a complete, un-defensive apology to Rachel.",
            scenarioDescription = "Behind closed doors in the office conference room.",
            hiddenGoal = "Accept complete responsibility, explain what happened without making excuses, and rebuild her trust with actionable safeguards.",
            difficulty = "Hard",
            initialMessage = "I saw the investor update thread. My name wasn't even mentioned once. Is this how you plan to treat your co-founder going forward?",
            xpReward = 135,
            estimatedTimeMinutes = 7,
            successCriteria = listOf("Avoid saying 'I'm sorry, BUT...'", "Own the operational mistake explicitly", "Detail how you will correct the narrative immediately"),
            targetWeakness = "Empathy & Perspective"
        ),
        DailyChallenge(
            id = "daily_conf_3",
            title = "Managing a Loud Roommate",
            category = "Conflict",
            dayOfWeekTheme = "Conflict Friday",
            partnerName = "Jasper",
            partnerAvatar = "A",
            partnerPersona = "Friendly but utterly oblivious roommate who leaves dirty blender cups in the sink and plays video games with speakers on past midnight.",
            description = "Establish clean roommate boundaries with Jasper.",
            scenarioDescription = "Shared apartment kitchen on a Sunday morning.",
            hiddenGoal = "Address the house rules firmly and kindly, avoid accusatory language, and secure Jasper's commitment to a daily cleanup rule.",
            difficulty = "Medium",
            initialMessage = "Morning! Oh, sorry about the mess, I had to rush out to catch a game last night. I'll get to it eventually, don't worry!",
            xpReward = 115,
            estimatedTimeMinutes = 6,
            successCriteria = listOf("Use 'I' statements instead of 'You always' statements", "Agree on a clear cleaning/noise boundary", "Maintain flatmate rapport"),
            targetWeakness = "Assertiveness & Speed"
        ),
        DailyChallenge(
            id = "daily_conf_4",
            title = "The Stolen Project Credit",
            category = "Conflict",
            dayOfWeekTheme = "Conflict Friday",
            partnerName = "Kevin (Co-worker)",
            partnerAvatar = "X",
            partnerPersona = "Ambitious and slippery. He presented your research results during a team meeting and claimed them as 'my findings' to look good for promotion.",
            description = "Confront Kevin privately about his presentation theft.",
            scenarioDescription = "Breakroom coffee corner. You grab Kevin for a quick chat.",
            hiddenGoal = "Call out the credit theft directly, reject his deflections, and establish a firm boundary for future collaboration reports.",
            difficulty = "Hard",
            initialMessage = "Oh, hey! Great meeting, right? The team loved the research findings. What's up?",
            xpReward = 145,
            estimatedTimeMinutes = 8,
            successCriteria = listOf("Address the intellectual theft with direct facts", "Refuse to accept passive gaslighting or 'joking' deflections", "Secure his agreement to credit your name on the email reports"),
            targetWeakness = "Assertiveness & Speed"
        ),

        // FUN SATURDAYS
        DailyChallenge(
            id = "daily_fun_1",
            title = "Escape Room Team Panic",
            category = "Fun",
            dayOfWeekTheme = "Fun Saturday",
            partnerName = "Barney",
            partnerAvatar = "Y",
            partnerPersona = "Stressed, easily panicked buddy who is convinced the escape room lock is broken and is ready to push the emergency panic escape button.",
            description = "Calm Barney's panic and coordinate the final room puzzle.",
            scenarioDescription = "Locked space-station themed escape room with 4 minutes remaining.",
            hiddenGoal = "Soothe Barney's panic, delegate puzzle tasks clearly, and solve the final padlock clue together.",
            difficulty = "Easy",
            initialMessage = "Guys, this lock is definitely broken! We are going to lose, and my heart is beating like crazy. I'm hitting the emergency exit door right now!",
            xpReward = 110,
            estimatedTimeMinutes = 5,
            successCriteria = listOf("Inject calm, grounding humor", "Give Barney a specific, easy task to focus on", "Solve the lock clue collaboratively"),
            targetWeakness = "Collaboration & Compromise"
        ),
        DailyChallenge(
            id = "daily_fun_2",
            title = "Zombie Survival Debate",
            category = "Fun",
            dayOfWeekTheme = "Fun Saturday",
            partnerName = "Zack",
            partnerAvatar = "Z",
            partnerPersona = "Self-proclaimed zombie survival expert who wants to hoard all supplies inside a noisy department store. Highly opinionated and logical.",
            description = "Negotiate a safer survival plan with Zack.",
            scenarioDescription = "Whispering inside an abandoned hardware store during a fictional outbreak.",
            hiddenGoal = "Convince Zack to move to the countryside instead of the mall, highlighting risks of noise and crowd traps.",
            difficulty = "Medium",
            initialMessage = "Listen, the department store has mattresses, canned food, and heavy metal shutters. It's the perfect fort! Why would we wander into the open woods like you suggest?",
            xpReward = 115,
            estimatedTimeMinutes = 6,
            successCriteria = listOf("Deconstruct mall traps logically", "Propose a structured alternative countryside map", "Maintain survival-party cohesion"),
            targetWeakness = "Collaboration & Compromise"
        ),
        DailyChallenge(
            id = "daily_fun_3",
            title = "Road Trip Playlist Negotiator",
            category = "Fun",
            dayOfWeekTheme = "Fun Saturday",
            partnerName = "Penny",
            partnerAvatar = "F",
            partnerPersona = "Enthusiastic but stubborn passenger who wants to play her favorite upbeat pop track on repeat for 10 hours straight. Easily bored.",
            description = "Negotiate a balanced road trip music rotation.",
            scenarioDescription = "Cruising on Route 66. Penny holds the aux cord.",
            hiddenGoal = "Validate her fun vibe, suggest a creative genre playlist rotation, and secure a shared musical agreement.",
            difficulty = "Easy",
            initialMessage = "Alright, next up is my favorite pop anthem for the fifth time! It's such a road-trip banger, you can't say no to this beat!",
            xpReward = 100,
            estimatedTimeMinutes = 4,
            successCriteria = listOf("Acknowledge her high-energy playlist vibe", "Propose a fair '3 songs each' aux-cord rule", "Keep the road trip mood extremely upbeat"),
            targetWeakness = "Collaboration & Compromise"
        ),
        DailyChallenge(
            id = "daily_fun_4",
            title = "The Ultimate Movie Debate",
            category = "Fun",
            dayOfWeekTheme = "Fun Saturday",
            partnerName = "Oliver",
            partnerAvatar = "V",
            partnerPersona = "Slightly snobby cinema buff who claims that modern blockbuster superhero movies are 'absolute trash' and 'not real art'.",
            description = "Defend lighthearted films against a film critic.",
            scenarioDescription = "Exiting the theater lobby after a cinematic screening.",
            hiddenGoal = "Respect his critical viewpoints, argue for the cultural and stress-relief value of popular blockbusters, and get him to agree to watch a fun action movie next week.",
            difficulty = "Medium",
            initialMessage = "I mean, that was just two hours of meaningless explosions and green screens. How can anyone call that cinema? It's insulting to real directors.",
            xpReward = 120,
            estimatedTimeMinutes = 6,
            successCriteria = listOf("Acknowledge his cinematic standard points", "Explain the psychological value of popcorn-entertainment", "Secure his open-mindedness for a casual movie night"),
            targetWeakness = "Active Listening & Depth"
        ),

        // EMOTIONAL INTELLIGENCE SUNDAYS
        DailyChallenge(
            id = "daily_eq_1",
            title = "Comforting an Anxious Friend",
            category = "Emotional Intelligence",
            dayOfWeekTheme = "Surprise Challenge",
            partnerName = "Leo",
            partnerAvatar = "J",
            partnerPersona = "Nervous, suffering from severe imposter syndrome. He is hyperventilating in his car before his final interview for a dream job.",
            description = "Calm Leo's nerves and build his self-belief.",
            scenarioDescription = "A parked car outside the corporate headquarters.",
            hiddenGoal = "Help Leo regulate his breathing, validate his qualifications with specific past proofs, and get him to enter the building confidently.",
            difficulty = "Medium",
            initialMessage = "My hands are literally shaking. I can't do this. Everyone inside has Ivy League degrees and I'm just going to embarrass myself. Maybe I should just drive home.",
            xpReward = 130,
            estimatedTimeMinutes = 6,
            successCriteria = listOf("Lead a quick breathing/grounding exercise", "Remind him of his specific past project victory", "Provide unconditional friendly validation"),
            targetWeakness = "Empathy & Perspective"
        ),
        DailyChallenge(
            id = "daily_eq_2",
            title = "Responding to Bad News",
            category = "Emotional Intelligence",
            dayOfWeekTheme = "Surprise Challenge",
            partnerName = "Nina",
            partnerAvatar = "C",
            partnerPersona = "Crushed and crying. Her beloved family dog of 14 years passed away this morning, and she feels completely empty and isolated.",
            description = "Support Nina through her sudden grief with pure empathy.",
            scenarioDescription = "Sitting together on a quiet park bench.",
            hiddenGoal = "Offer a safe emotional space, avoid toxic positivity or saying 'everything happens for a reason', and let her know she isn't alone.",
            difficulty = "Hard",
            initialMessage = "I woke up and he just... wasn't breathing. He's been with me through high school, college, everything. I don't know how to go back to my empty apartment.",
            xpReward = 140,
            estimatedTimeMinutes = 7,
            successCriteria = listOf("Strictly avoid offering logical 'solutions' or toxic positivity", "Validate the profound weight of her loss", "Offer simple, physical or presence-based support"),
            targetWeakness = "Empathy & Perspective"
        ),
        DailyChallenge(
            id = "daily_eq_3",
            title = "Celebrating a Friend's Win",
            category = "Emotional Intelligence",
            dayOfWeekTheme = "Surprise Challenge",
            partnerName = "Marcus",
            partnerAvatar = "K",
            partnerPersona = "Thrilled but humble. He just got accepted into a prestigious creative writing master's program but is downplaying his achievement.",
            description = "Amplify Marcus's excitement and make him celebrate.",
            scenarioDescription = "A local celebration toast at a pub table.",
            hiddenGoal = "React with high genuine excitement, highlight the magnitude of his hard work, and get him to fully toast his success.",
            difficulty = "Easy",
            initialMessage = "I mean, yeah, I got the acceptance letter! It's cool, but I'm sure they had some spots to fill or someone cancelled last minute. It's probably not that big of a deal.",
            xpReward = 110,
            estimatedTimeMinutes = 5,
            successCriteria = listOf("Express high emotional energy and excitement", "Correct his self-deprecating humble statements", "Propose a structured, celebratory toast action"),
            targetWeakness = "Collaboration & Compromise"
        ),
        DailyChallenge(
            id = "daily_eq_4",
            title = "Expressing Deep Gratitude",
            category = "Emotional Intelligence",
            dayOfWeekTheme = "Surprise Challenge",
            partnerName = "Coach Henry",
            partnerAvatar = "E",
            partnerPersona = "Caring, retired academic mentor who spent months editing your master's thesis and writing recommendation letters, expecting nothing in return.",
            description = "Thank Henry sincerely for his pivotal role in your life.",
            scenarioDescription = "His university office during graduation week.",
            hiddenGoal = "Detail the specific advice that changed your path, present a small meaningful token, and express lasting professional and personal gratitude.",
            difficulty = "Easy",
            initialMessage = "Well, look at you in your graduation gown! It seems like just yesterday we were arguing about research methodologies in the library. I'm very proud of you.",
            xpReward = 100,
            estimatedTimeMinutes = 5,
            successCriteria = listOf("State a specific piece of advice that influenced your life", "Acknowledge the sacrifice of his free time", "Commit to staying in touch long-term"),
            targetWeakness = "Empathy & Perspective"
        )
    )

    /**
     * Retrieves the Daily Challenge for the current day.
     * Implements smart personalization based on the user's weakest skill from Communication DNA.
     * Ensures rotation and maps to the weekly themes.
     */
    fun getDailyChallengeForDate(calendar: Calendar, weakestSkill: String?): DailyChallenge {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val targetTheme = weeklyThemes[dayOfWeek] ?: "Surprise Challenge"

        // Filter challenges by theme
        var themeChallenges = challenges.filter { it.dayOfWeekTheme == targetTheme }
        if (themeChallenges.isEmpty()) {
            themeChallenges = challenges
        }

        // Smart selection: If we have a user weakness, try to find a challenge in this theme that matches
        // the target weakness.
        if (weakestSkill != null) {
            val personalMatch = themeChallenges.find { it.targetWeakness == weakestSkill }
            if (personalMatch != null) {
                return personalMatch
            }
        }

        // Fallback: Rotation using dayOfMonth to ensure variety
        val index = (dayOfMonth + dayOfWeek) % themeChallenges.size
        return themeChallenges[index]
    }
}

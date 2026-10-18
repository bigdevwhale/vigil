package app.vigil.content

/** The catalog in English. */
internal val catalogEn: Map<String, CatalogText> = mapOf(
    // Body
    "no_sugar" to CatalogText(
        "No sugar", "No sweets, no sugar in your tea",
        "Sugar is the most everyday of cravings: in every snack and every \"I've earned it\". After three weeks your tongue learns the sweetness of an apple again.",
        "Days three and five bring the cravings and the temper. Keep nuts and water at hand rather than willpower.",
    ),
    "no_alcohol" to CatalogText(
        "No alcohol", "Not a drop: no wine, no beer, no \"just to be sociable\"",
        "A sober month is the most honest test of what drink means in your life. Many people see for the first time in years how they sleep and how they wake.",
        "Fridays and the first party are the hardest. Decide in advance what you'll drink instead and what you'll say to \"just one glass\".",
    ),
    "no_alcohol_100" to CatalogText(
        "A hundred sober days", "No alcohol for a hundred days in a row",
        "A hundred days is when sobriety stops being a vow and becomes a habit. Not a break any more, but a new life.",
        "Days 40 to 60 are the most dangerous: it feels as if you've proved it all and \"once won't hurt\". Read again why you began.",
    ),
    "no_fastfood" to CatalogText(
        "No fast food", "No burgers, no kebabs, no fries",
        "Fast food is food that decides for you. A month without it brings back cooking and noticing what you eat.",
        "Hunger on the road and late evenings are the traps. Carry something simple with you.",
    ),
    "no_meat" to CatalogText(
        "No meat", "No meat or poultry; fish is your call",
        "Giving up meat is one of the oldest fasts. It teaches variety and attention to the plate.",
        "The first two weeks crave something filling. Add beans, eggs and cheese, and don't replace meat with bread.",
    ),
    "plant_only" to CatalogText(
        "Plants only", "No meat, fish, milk or eggs",
        "The strict plant fast, as kept on the strictest days. It asks you to think about food ahead, and that is the practice.",
        "Read the labels: milk and eggs hide in pastry and sauces. Plan tomorrow's lunch today.",
    ),
    "no_caffeine" to CatalogText(
        "No caffeine", "No coffee, no tea, no energy drinks",
        "Two weeks without caffeine bring back your own rhythm: morning alertness comes by itself, not from a cup.",
        "Days two to four bring headaches and drowsiness. Sleep more, drink water, and don't start in a deadline week.",
    ),
    "coffee_one" to CatalogText(
        "One cup of coffee", "No more than one cup a day", "Measure, not refusal: one cup drunk with attention instead of five on autopilot.",
        "The second cup asks for itself after lunch. Swap it for a walk or a glass of water.", unit = "cup",
    ),
    "no_snacks" to CatalogText(
        "No snacking", "Breakfast, lunch and dinner only",
        "Snacks eat your attention: the hand reaches for food out of boredom, not hunger. Three meals, and quiet between them.",
        "Evenings in front of a screen are the hardest. Put food out of sight rather than relying on will.",
    ),
    "no_late_food" to CatalogText(
        "Nothing after 7 pm", "The kitchen closes in the evening",
        "An empty stomach at night means better sleep and a light morning. One simple rule that changes the whole evening.",
        "Hunger at 9 pm is usually boredom or tiredness. Unsweetened tea, and bed earlier.",
    ),
    "fast_16_8" to CatalogText(
        "The 16:8 window", "Eat only between noon and 8 pm",
        "Sixteen hours without food: the body learns to wait, and hunger stops being an order.",
        "The first week brings morning hunger. Water, plain tea and busy hands.",
    ),
    "cold_shower" to CatalogText(
        "Cold shower", "Every morning, under cold water",
        "Cold water is a small daily feat. Whoever bears a minute of it bears the day more calmly.",
        "Don't think, just turn the tap. Count to thirty and keep your breath even.",
    ),
    "cold_shower_40" to CatalogText(
        "Forty days of cold", "A cold shower forty days in a row",
        "Forty days is the length of a fast and a trial. The cold turns from a feat into a quiet morning joy.",
        "Skipping it when ill is common sense, not a sin: use an indulgence and come back.",
    ),
    "steps" to CatalogText(
        "10,000 steps", "Ten thousand steps every day",
        "Walking is the simplest of practices: it airs the body and the head.",
        "Rainy days decide everything. Keep a backup route: a mall, stairs, laps around home.",
    ),
    "early_rise" to CatalogText(
        "Up at 6", "Rise with the dawn every day",
        "The early hour belongs only to you. Monasteries rise early not for discipline but for the silence.",
        "An early rise begins with an early night. Put the alarm away from the bed.",
    ),
    "lights_out" to CatalogText(
        "In bed by 11", "Every night in bed before eleven",
        "Sleep is the foundation of every other vow. A tired person breaks them all.",
        "\"One more episode\" is the enemy. Set a reminder for 10:30.",
    ),
    "no_lift" to CatalogText(
        "No lifts", "Stairs only: at home, at work, everywhere",
        "A small daily ascent. The stairs don't argue and don't wait.",
        "With bags and tired legs you'll want the button. Go slowly, but go.",
    ),
    "stretch" to CatalogText(
        "Stretching", "Ten minutes of stretching every day",
        "Supple body, supple mind: they grow together. Ten minutes is shorter than an episode.",
        "Tie it to something fixed: right after brushing your teeth.",
    ),
    "water" to CatalogText(
        "Water", "Two litres of water a day",
        "The simplest care for the body, and the first one forgotten.",
        "Keep a bottle on the desk. What's in sight gets drunk.",
    ),
    "no_smoking" to CatalogText(
        "No smoking", "No cigarettes, no vapes, no hookah",
        "The hardest and most precious vow of the body. Forty days is when the craving clearly weakens.",
        "Days three and seven are the peaks. The candle of waiting works best here: a craving lasts minutes.",
    ),
    // Digital
    "no_social" to CatalogText(
        "No social media", "No feeds, no stories, no \"just checking\"",
        "A month without feeds gives back the boring minutes, and thoughts are born of boredom.",
        "Delete the apps. Your hand will reach for the empty spot; that's normal.",
    ),
    "no_shorts" to CatalogText(
        "No short videos", "No Reels, no Shorts, no TikTok",
        "Short videos chop attention into fifteen-second pieces. A month, and you can read a book again.",
        "Bedtime is the danger. The phone charges away from the bed.",
    ),
    "no_youtube" to CatalogText(
        "No YouTube", "Two weeks without videos in the background",
        "Background video fills the silence you ought to hear.",
        "Eating without a video feels empty at first. Try just eating.",
    ),
    "no_news" to CatalogText(
        "No news", "Don't read or watch the news",
        "News sells anxiety. Three weeks without it, and what matters will still reach you.",
        "You'll want to \"stay informed\". If something important happens, someone will tell you.",
    ),
    "no_games" to CatalogText(
        "No games", "No mobile or computer games",
        "Games give victories without effort. A month without them and real things grow interesting again.",
        "Evenings after work are the trap. Decide in advance how you'll fill them.",
    ),
    "no_series" to CatalogText(
        "No series", "Not a single episode",
        "A series is built so that you can't stop. A month of quiet evenings.",
        "The first week the evenings feel empty. That emptiness is room for something new.",
    ),
    "no_porn" to CatalogText(
        "No porn", "Ninety days of clear eyes",
        "Ninety days is how long the brain takes to unlearn a habit. Quiet instead of stimulus.",
        "Late night, tiredness and loneliness travel together. Go to bed when you're tired.",
    ),
    "phone_bedroom" to CatalogText(
        "No phone in the bedroom", "From 10 pm to 7 am the phone sleeps in another room",
        "The bedroom is for sleep. Without the phone the evenings grow longer and the mornings are yours.",
        "You'll need a plain alarm clock. Buy the simplest one: that's part of the vow.",
    ),
    "phone_morning" to CatalogText(
        "Phone-free mornings", "The first hours of the day without a screen",
        "Start the morning with a feed and you live someone else's day. The first hour is yours.",
        "Don't pick up the phone until you're dressed and have had breakfast.",
    ),
    "grayscale" to CatalogText(
        "Grey screen", "The phone in black and white",
        "Without colour the phone loses half its pull. A simple, honest trick.",
        "Turn it on in the accessibility settings, and don't turn it off \"for a minute\".",
    ),
    "screen_hour" to CatalogText(
        "One hour of fun", "No more than 60 minutes of screen entertainment a day",
        "Measure instead of a ban: one honest hour, not the whole evening.",
        "Set a limit in the phone's digital wellbeing and let it stand guard.", unit = "minutes",
    ),
    // Speech
    "no_swearing" to CatalogText(
        "No swearing", "Not one curse word",
        "A word is a deed too. Three weeks of clean speech change the tone of your thoughts.",
        "It slips out by itself when you're upset. Each time: a pause, and a different word.",
    ),
    "no_complaints" to CatalogText(
        "No complaining", "No complaints, out loud or in messages",
        "Twenty-one days without complaining: an old practice that turns your eyes from what's wrong to what can be done.",
        "A complaint often sounds like \"I'm just telling you\". If you speak of something bad, add what you'll do.",
    ),
    "no_gossip" to CatalogText(
        "No gossip", "Don't talk about people who aren't there",
        "The rule is simple: say about someone only what you'd say to their face.",
        "It's hardest in company where everyone's at it. You can just stay quiet.",
    ),
    "no_criticism" to CatalogText(
        "No judging", "Don't judge others, out loud or in your head",
        "Judging is a habit of the mind. Three weeks of watching it reveal a lot about yourself.",
        "The judging thought will come. Let it go before you finish it.",
    ),
    "no_must" to CatalogText(
        "No \"must\"", "A week without \"I must\" and \"you must\"",
        "\"Must\" turns life into a duty. Swap it for \"I choose\" or \"I want\".",
        "It slips out dozens of times a day. Noticing is half the vow.",
    ),
    "silent_morning" to CatalogText(
        "Silent mornings", "Until 9 am, no word without need",
        "A monastic rule: morning in silence. The day's words grow out of the morning's quiet.",
        "Tell the people at home: the silence isn't sulking, it's a vow.",
    ),
    "no_sarcasm" to CatalogText(
        "No sarcasm", "Speak plainly, without barbs",
        "Sarcasm is anger dressed as a joke. Two weeks of plain speech.",
        "When you want to jab, say plainly what stung you.",
    ),
    // Money
    "no_spend" to CatalogText(
        "No unneeded buying", "Only food, home and what's necessary",
        "A month without shopping shows how much of what you bought you really needed.",
        "Write the list of necessities in advance. Anything not on it waits a week.",
    ),
    "no_delivery" to CatalogText(
        "No food delivery", "Cook, or eat where it's made",
        "Delivery is a convenience that quietly eats both money and the skill of cooking.",
        "Keep three easy recipes for tired evenings.",
    ),
    "no_marketplace" to CatalogText(
        "No online shops", "No ordering and no scrolling",
        "Scrolling a shop is the same feed, only with price tags.",
        "Delete the app. What you need you'll buy in a shop, or in a week.",
    ),
    "no_taxi" to CatalogText(
        "No taxis", "Walk, ride the bus, cycle",
        "Three weeks without taxis: more steps, more city, less spent.",
        "Running late is the temptation. Leave ten minutes earlier.",
    ),
    "cash_only" to CatalogText(
        "Cash only", "Pay only with paper money",
        "Cash can be felt. When you see the money melt, you spend differently.",
        "Take out a week's sum. When it's gone, the week is lived.",
    ),
    "no_coffee_out" to CatalogText(
        "No takeaway coffee", "Coffee only at home or at work",
        "A small daily spend becomes a noticeable sum in a month. And a reason for a travel mug.",
        "The morning walk past the café is the test. Bring your own coffee.",
    ),
    // Mind
    "read" to CatalogText(
        "Reading", "Twenty pages of a book a day",
        "Twenty pages a day is more than a dozen books a year.",
        "Read at the same time each day. The book lies where the phone usually does.",
    ),
    "read_100" to CatalogText(
        "A hundred days of reading", "A book every day for a hundred days",
        "A hundred days, and reading becomes part of you rather than a task.",
        "If a book won't go, change it. The vow is to reading, not to that book.",
    ),
    "no_headphones" to CatalogText(
        "No headphones", "A week without music or podcasts in your ears",
        "The city, your steps, your own thoughts: everything headphones drown out.",
        "The first days the quiet feels empty. Then you begin to hear yourself in it.",
    ),
    "one_thing" to CatalogText(
        "One thing at a time", "No multitasking: one thing, to the end",
        "Attention split in two works at half strength. Two weeks of wholeness.",
        "Close the extra tabs. Phone face down.",
    ),
    "journal" to CatalogText(
        "Journal", "A few lines every evening",
        "A journal is a conversation with yourself. After a month you see what the month was really about.",
        "Three sentences are enough. Every day is what matters.",
    ),
    "meditate" to CatalogText(
        "Still mind", "Ten minutes of meditation every day",
        "Ten minutes of stillness: the hardest thing you can do while doing nothing.",
        "Thoughts will wander. Bringing them back is the practice.",
    ),
    "meditate_108" to CatalogText(
        "108 days of stillness", "Meditation 108 days in a row",
        "108 is the number of beads on a prayer rope. A full round of daily stillness.",
        "The middle of the road is the most boring. Boredom is something to watch too.",
    ),
    "walk_bare" to CatalogText(
        "A walk without the phone", "Half an hour of walking with empty hands",
        "A walk without the phone is a walk, not moving around with a screen.",
        "Leave the phone at home. The world won't fall in thirty minutes.",
    ),
    "words" to CatalogText(
        "Ten words", "Ten new words of a foreign language a day",
        "Three hundred words in a month, and simple texts begin to open.",
        "Repeat yesterday's words before learning new ones.",
    ),
    // Heart
    "good_deed" to CatalogText(
        "A good deed", "One good deed a day",
        "Kindness is a muscle. Twenty-one days, and noticing who needs a hand becomes a habit.",
        "It needn't be big: hold a door, write to someone who's lonely.",
    ),
    "call_kin" to CatalogText(
        "Call someone dear", "Every day, phone someone close",
        "No message replaces the voice of someone dear.",
        "A list of five names, and call them in turn.",
    ),
    "gratitude" to CatalogText(
        "Gratitude", "Every day, say thank you out loud",
        "Thanks said aloud change the one who says them too.",
        "Start simple: \"thank you for being here\", at home.",
    ),
    "no_arguing" to CatalogText(
        "No online arguments", "Don't argue in the comments",
        "No one was ever convinced by an argument online. A month of peace.",
        "Your fingers are already typing a reply. Delete it and close the tab.",
    ),
    "no_interrupting" to CatalogText(
        "No interrupting", "Hear people out to the end",
        "Listening to the end is a rare gift. Two weeks of practice.",
        "When you badly want to cut in, take a breath.",
    ),
    "compliment" to CatalogText(
        "A true compliment", "One honest kind word a day",
        "Notice the good in people, and tell them.",
        "Only the truth. An insincere compliment doesn't count.",
    ),
    // Spirit
    "lent" to CatalogText(
        "Lent", "No meat, dairy or eggs until Easter",
        "Lent: the forty days before Easter. Not a diet but a time of gathering yourself and of quiet.",
        "Fasting isn't only about food. Add something from Speech or Heart to it.",
    ),
    "nativity_fast" to CatalogText(
        "Nativity Fast", "From 28 November until Christmas",
        "Forty days before the Orthodox Christmas: a milder fast than Lent, with fish on many days.",
        "December is full of parties. Decide ahead how you'll be at them.",
    ),
    "silence_day" to CatalogText(
        "A day of silence", "Twenty-four hours without a word",
        "One day of full silence: a short retreat without going anywhere.",
        "Pick a day off and tell the people around you. Write notes if you really must.",
    ),
    "no_soft_bed" to CatalogText(
        "No soft bed", "A week sleeping on the floor or something hard",
        "An old austerity: giving up a comfort you don't notice while you have it.",
        "The first nights are uncomfortable. A thin mat is allowed, a featherbed is not.",
    ),
    "no_hot_water" to CatalogText(
        "No hot water", "Wash only in cool water",
        "Two weeks without hot water: a stern and bracing trial.",
        "Start with hands and face, and don't stay under the water longer than you need.",
    ),
    "one_meal" to CatalogText(
        "One meal", "One meal a day", "A week of one meal: an ancient fast and a test of what hunger really was.",
        "Don't begin without the health for it. If you feel weak, use an indulgence.", unit = "meal",
    ),
    "no_salt_sugar" to CatalogText(
        "No salt or sugar", "Plain food for two weeks",
        "Without salt and sugar, food shows its own taste.",
        "The first three days everything tastes flat. Then taste comes back, finer.",
    ),
    "vigil" to CatalogText(
        "Night vigil", "Every evening, fifteen minutes of quiet by candlelight",
        "A vigil isn't sleeplessness but attention. Quiet before sleep gathers the day together.",
        "Lights off, a candle lit, the phone put away. Fifteen minutes is enough.",
    ),
    "dawn" to CatalogText(
        "Greet the dawn", "See the sunrise every morning",
        "Every dawn is new. Twenty-one dawns, twenty-one beginnings.",
        "In winter the dawn is late, which is easy. In summer it's a trial.",
    ),
    "prayer_rope" to CatalogText(
        "Prayer rope", "Every day, one round of the rope: a hundred and eight repetitions",
        "A prayer rope counts attention. A hundred and eight beads, a hundred and eight days.",
        "The words, a prayer, a mantra or just \"be calm\", are your choice.",
    ),
)

package com.timedoesntwait.domain

/**
 * Guesses an activity from free text using English + Korean keyword lists.
 * One clear winner → that activity. No match or a tie → null (Uncategorized).
 */
class CategoryGuesser(private val activities: List<Activity>) {

    fun guess(text: String): Activity? {
        val normalized = text.lowercase().trim()
        if (normalized.isEmpty()) return null
        val words = WORD_SPLIT.split(normalized).filter { it.isNotEmpty() }.toSet()

        val scores = activities.filterNot { it.archived }.associateWith { activity ->
            keywordsFor(activity).count { kw -> matches(kw, normalized, words) }
        }.filterValues { it > 0 }

        if (scores.isEmpty()) return null
        val best = scores.values.max()
        val winners = scores.filterValues { it == best }.keys
        return winners.singleOrNull()
    }

    private fun keywordsFor(activity: Activity): Set<String> = buildSet {
        add(activity.name.lowercase())
        addAll(activity.keywords.map { it.lowercase().trim() }.filter { it.isNotEmpty() })
        activity.nameKey?.let { addAll(BUILT_IN_KEYWORDS[it].orEmpty()) }
    }

    private fun matches(keyword: String, text: String, words: Set<String>): Boolean =
        if (keyword.any { it in '가'..'힣' }) {
            // Korean attaches particles to nouns (라면을, 운동했다), so match as a substring.
            text.contains(keyword)
        } else if (keyword.contains(' ')) {
            text.contains(keyword)
        } else {
            words.contains(keyword) || words.any { it.length > keyword.length && it.startsWith(keyword) && keyword.length >= 4 }
        }

    companion object {
        private val WORD_SPLIT = Regex("[^\\p{L}\\p{N}]+")

        /** Keyed by [Activity.nameKey] of the built-in activities. */
        val BUILT_IN_KEYWORDS: Map<String, Set<String>> = mapOf(
            "grind" to setOf(
                "work", "working", "meeting", "email", "emails", "code", "coding", "debug", "deploy", "report",
                "presentation", "slides", "client", "project", "office", "task", "tasks", "standup",
                "업무", "일했", "회의", "미팅", "이메일", "메일", "코딩", "개발", "보고서", "발표", "출근", "야근", "작업",
            ),
            "snack" to setOf(
                "eat", "ate", "eating", "lunch", "dinner", "breakfast", "brunch", "snack", "coffee", "tea", "ramen",
                "food", "meal", "cook", "cooking", "cooked", "pizza", "burger", "noodles", "rice", "matcha", "latte",
                "밥", "점심", "저녁", "아침", "간식", "라면", "커피", "식사", "요리", "먹", "카페", "배달", "브런치",
            ),
            "vibe" to setOf(
                "music", "song", "songs", "concert", "relax", "relaxing", "chill", "chilled", "netflix", "movie",
                "youtube", "tv", "drama", "podcast", "scroll", "scrolling", "instagram", "tiktok",
                "음악", "노래", "휴식", "쉬었", "넷플릭스", "영화", "유튜브", "드라마", "예능", "팟캐스트", "인스타",
            ),
            "study" to setOf(
                "study", "studied", "studying", "class", "lecture", "homework", "read", "reading", "book", "course",
                "exam", "learn", "learning", "practice", "research", "algorithm", "algorithms", "chapter",
                "공부", "수업", "강의", "숙제", "과제", "독서", "읽었", "읽기", "시험", "학원", "복습", "예습",
            ),
            "walk" to setOf(
                "walk", "walked", "walking", "run", "ran", "running", "gym", "workout", "exercise", "yoga",
                "bike", "cycling", "swim", "swimming", "hike", "hiking", "commute", "stroll", "pilates",
                "산책", "걷", "달리기", "러닝", "운동", "헬스", "요가", "자전거", "수영", "등산", "출퇴근", "필라테스",
            ),
            "nap" to setOf(
                "nap", "napped", "sleep", "slept", "sleeping", "rest", "rested", "bed", "doze", "dozed",
                "낮잠", "잤", "수면", "잠들", "누워",
            ),
            "game" to setOf(
                "game", "games", "gaming", "steam", "valorant", "minecraft", "nintendo",
                "league", "zelda", "pokemon", "board game",
                "게임", "롤", "발로란트", "마인크래프트", "닌텐도", "보드게임", "플스",
            ),
            "chat" to setOf(
                "chat", "chatted", "talk", "talked", "call", "called", "phone", "friend", "friends", "family",
                "mom", "dad", "date", "party", "hangout", "hung out", "text", "texting", "kakao",
                "통화", "전화", "친구", "가족", "엄마", "아빠", "데이트", "수다", "대화", "카톡", "만남", "모임",
            ),
        )
    }
}

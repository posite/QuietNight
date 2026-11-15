package com.example.quietnight.data

import android.content.Context
import com.example.quietnight.domain.repository.SleepRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomSleepRepository @Inject constructor(
    private val dao: SnoreDao,
    @param:ApplicationContext private val context: Context
) : SleepRepository {
    override fun observeHistory() = dao.getHistory().map { rows -> rows.map { it.toDomain() } }

    override suspend fun clipsForDate(date: Long) = dao.getClipsByDate(date).map { it.toDomain() }

    override suspend fun saveFeedback(date: Long, feedback: com.example.quietnight.domain.model.MorningFeedback) =
        dao.updateFeedback(date, feedback.name)

    override suspend fun clearHistory() = withContext(Dispatchers.IO) {
        dao.clearAllClips()
        dao.clearAll()
        // 기존 정책대로 앱 내부의 코골이 녹음 디렉터리만 정리한다.
        File(context.filesDir, "snore_clips").deleteRecursively()
        Unit
    }
}

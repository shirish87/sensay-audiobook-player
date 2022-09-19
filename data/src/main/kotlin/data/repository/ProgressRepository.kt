package data.repository

import data.dao.ProgressDao
import data.entity.Progress
import javax.inject.Inject

class ProgressRepository @Inject constructor(
    private val progressDao: ProgressDao,
) {

    fun progressCount() = progressDao.progressCount()

    fun progressRestorable() = progressDao.progressRestorable()

    suspend fun deleteProgress(progress: Progress) = progressDao.delete(progress)
}

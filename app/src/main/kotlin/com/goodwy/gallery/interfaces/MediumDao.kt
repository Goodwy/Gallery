package com.goodwy.gallery.interfaces

import androidx.room.*
import com.goodwy.gallery.models.Medium

data class FolderRowCount(
    @ColumnInfo(name = "parent_path") val parentPath: String,
    @ColumnInfo(name = "row_count") val rowCount: Long
)

data class MediumCleanupCandidate(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "full_path") val path: String,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean,
    @ColumnInfo(name = "deleted_ts") val deletedTS: Long
)

@Dao
interface MediumDao {
    @Query("SELECT filename, full_path, parent_path, last_modified, date_taken, size, type, video_duration, is_favorite, deleted_ts, media_store_id FROM media WHERE deleted_ts = 0 AND parent_path = :path COLLATE NOCASE")
    fun getMediaFromPath(path: String): List<Medium>

    @Query("SELECT filename, full_path, parent_path, last_modified, date_taken, size, type, video_duration, is_favorite, deleted_ts, media_store_id FROM media WHERE deleted_ts = 0 AND is_favorite = 1")
    fun getFavorites(): List<Medium>

    @Query("SELECT COUNT(filename) FROM media WHERE deleted_ts = 0 AND is_favorite = 1")
    fun getFavoritesCount(): Long

    @Query("SELECT filename, full_path, parent_path, last_modified, date_taken, size, type, video_duration, is_favorite, deleted_ts, media_store_id FROM media WHERE deleted_ts != 0")
    fun getDeletedMedia(): List<Medium>

    @Query("SELECT COUNT(filename) FROM media WHERE deleted_ts != 0")
    fun getDeletedMediaCount(): Long

    @Query("SELECT COUNT(*) FROM media")
    fun getTotalRowCount(): Long

    @Query("SELECT parent_path, COUNT(*) AS row_count FROM media GROUP BY parent_path")
    fun getRowCountByFolder(): List<FolderRowCount>

    @Query("SELECT id, full_path, is_favorite, deleted_ts FROM media WHERE id > :afterId ORDER BY id LIMIT :limit")
    fun getCleanupCandidates(afterId: Long, limit: Int): List<MediumCleanupCandidate>

    @Query("SELECT filename, full_path, parent_path, last_modified, date_taken, size, type, video_duration, is_favorite, deleted_ts, media_store_id FROM media WHERE deleted_ts < :timestmap AND deleted_ts != 0")
    fun getOldRecycleBinItems(timestmap: Long): List<Medium>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(medium: Medium)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(media: List<Medium>)

    @Delete
    fun deleteMedia(vararg medium: Medium)

    @Query("DELETE FROM media WHERE full_path = :path COLLATE NOCASE")
    fun deleteMediumPath(path: String)

    @Query("DELETE FROM media WHERE parent_path = :path COLLATE NOCASE OR parent_path LIKE :childPathPattern COLLATE NOCASE")
    fun deleteMediaByParentPathWithChildren(path: String, childPathPattern: String)

    @Query("DELETE FROM media WHERE id IN (:ids)")
    fun deleteByIds(ids: List<Long>): Int

    @Query("UPDATE OR REPLACE media SET filename = :newFilename, full_path = :newFullPath, parent_path = :newParentPath WHERE full_path = :oldPath COLLATE NOCASE")
    fun updateMedium(oldPath: String, newParentPath: String, newFilename: String, newFullPath: String)

    @Query("UPDATE OR REPLACE media SET full_path = :newPath, deleted_ts = :deletedTS WHERE full_path = :oldPath COLLATE NOCASE")
    fun updateDeleted(newPath: String, deletedTS: Long, oldPath: String)

    @Query("UPDATE media SET date_taken = :dateTaken WHERE full_path = :path COLLATE NOCASE")
    fun updateFavoriteDateTaken(path: String, dateTaken: Long)

    @Query("UPDATE media SET is_favorite = :isFavorite WHERE full_path = :path COLLATE NOCASE")
    fun updateFavorite(path: String, isFavorite: Boolean)

    @Query("UPDATE media SET is_favorite = 0")
    fun clearFavorites()

    @Query("DELETE FROM media WHERE deleted_ts != 0")
    fun clearRecycleBin()
}

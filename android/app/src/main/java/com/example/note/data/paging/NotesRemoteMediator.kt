package com.example.note.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.note.data.local.NoteDatabase
import com.example.note.data.local.RemoteKeys
import com.example.note.data.local._NoteEntity
import com.example.note.data.remote.api.NoteApi

@OptIn(ExperimentalPagingApi::class)
class NotesRemoteMediator(
    private val api: NoteApi,
    private val db: NoteDatabase,
    private val feed: String = "main" // 여러 피드(예: mine/all) 지원시 키로 구분
) : RemoteMediator<Int, _NoteEntity>() {

    override suspend fun initialize(): InitializeAction =
        InitializeAction.LAUNCH_INITIAL_REFRESH

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, _NoteEntity>
    ): MediatorResult {
        return try {
            val cursor = when (loadType) {
                LoadType.REFRESH -> null
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> db.remoteKeys().get(feed)?.nextCursor
            }

            val pageSize = state.config.pageSize.coerceAtMost(50)
            val resp = api.listNotes(limit = pageSize, cursor = cursor, order = "desc")

            db.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    db.getDao().clear()
                    db.remoteKeys().clear(feed)
                }
                val entities = resp.items.map { dto ->
                    _NoteEntity(
                        id = dto.id,
                        userId = dto.userId,
                        userName = dto.userName,
                        title = dto.title,
                        content = dto.content,
                        summarize = dto.summarize,
                        sentiment = dto.sentiment,
                        createdAt = dto.createdAt?.takeUnless { it.isBlank() } ?: "1970-01-01T00:00:00Z"
                    )
                }
                db.getDao().upsertAll(entities)
                db.remoteKeys().upsert(RemoteKeys(feed = feed, nextCursor = resp.nextCursor))
            }

            MediatorResult.Success(endOfPaginationReached = resp.nextCursor == null)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}

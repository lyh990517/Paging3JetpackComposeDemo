package com.example.paging3demo.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.paging3demo.data.local.UnsplashDatabase
import com.example.paging3demo.data.remote.UnsplashApi
import com.example.paging3demo.model.UnsplashImage
import com.example.paging3demo.model.UnsplashRemoteKeys
import com.example.paging3demo.util.Constants.ITEMS_PER_PAGE

@ExperimentalPagingApi
// Paging 라이브러리를 사용하여 원격 데이터를 가져오는 클래스
class UnsplashRemoteMediator(
    private val unsplashApi: UnsplashApi, // 원격 데이터를 가져오는 API
    private val unsplashDatabase: UnsplashDatabase // 로컬 데이터베이스
) : RemoteMediator<Int, UnsplashImage>() { // RemoteMediator를 상속받아 페이징 처리

    // 데이터베이스 DAO 인스턴스들
    private val unsplashImageDao = unsplashDatabase.unsplashImageDao()
    private val unsplashRemoteKeysDao = unsplashDatabase.unsplashRemoteKeysDao()

    // 데이터를 로드하는 메서드
    override suspend fun load(
        loadType: LoadType, // 로드 유형(새로고침, 이전 페이지 로드, 다음 페이지 로드)
        state: PagingState<Int, UnsplashImage> // 현재 페이징 상태
    ): MediatorResult {
        return try {
            // 현재 페이지 번호 결정
            val currentPage = when (loadType) {
                // 처음 로드할 때
                LoadType.REFRESH -> {
                    val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                    remoteKeys?.nextPage?.minus(1) ?: 1
                }
                // 리스트의 시작에 데이터를 추가할 때
                LoadType.PREPEND -> {
                    val remoteKeys = getRemoteKeyForFirstItem(state)
                    val prevPage = remoteKeys?.prevPage
                        ?: return MediatorResult.Success(
                            endOfPaginationReached = remoteKeys != null
                        )
                    prevPage
                }
                // 리스트의 끝에 데이터를 추가할 때
                LoadType.APPEND -> {
                    val remoteKeys = getRemoteKeyForLastItem(state)
                    val nextPage = remoteKeys?.nextPage
                        ?: return MediatorResult.Success(
                            endOfPaginationReached = remoteKeys != null
                        )
                    nextPage
                }
            }

            // 원격 데이터 가져오기
            val response = unsplashApi.getAllImages(page = currentPage, perPage = ITEMS_PER_PAGE)
            val endOfPaginationReached = response.isEmpty()

            // 이전 페이지와 다음 페이지 계산
            val prevPage = if (currentPage == 1) null else currentPage - 1
            val nextPage = if (endOfPaginationReached) null else currentPage + 1

            // 트랜잭션 내부에서 데이터베이스 작업 수행
            unsplashDatabase.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    // 새로고침할 때 기존 데이터 삭제
                    unsplashImageDao.deleteAllImages()
                    unsplashRemoteKeysDao.deleteAllRemoteKeys()
                }
                // 가져온 데이터와 관련된 키를 추가
                val keys = response.map { unsplashImage ->
                    UnsplashRemoteKeys(
                        id = unsplashImage.id,
                        prevPage = prevPage,
                        nextPage = nextPage
                    )
                }
                unsplashRemoteKeysDao.addAllRemoteKeys(remoteKeys = keys)
                // 가져온 이미지를 DB에 추가
                unsplashImageDao.addImages(images = response)
            }
            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: Exception) {
            // 에러가 발생한 경우 처리
            return MediatorResult.Error(e)
        }
    }

    // 현재 포지션에 가장 가까운 키를 가져옴
    private suspend fun getRemoteKeyClosestToCurrentPosition(
        state: PagingState<Int, UnsplashImage>
    ): UnsplashRemoteKeys? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { id ->
                unsplashRemoteKeysDao.getRemoteKeys(id = id)
            }
        }
    }

    // 리스트의 처음 아이템에 대한 키를 가져옴
    private suspend fun getRemoteKeyForFirstItem(
        state: PagingState<Int, UnsplashImage>
    ): UnsplashRemoteKeys? {
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
            ?.let { unsplashImage ->
                unsplashRemoteKeysDao.getRemoteKeys(id = unsplashImage.id)
            }
    }

    // 리스트의 마지막 아이템에 대한 키를 가져옴
    private suspend fun getRemoteKeyForLastItem(
        state: PagingState<Int, UnsplashImage>
    ): UnsplashRemoteKeys? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()
            ?.let { unsplashImage ->
                unsplashRemoteKeysDao.getRemoteKeys(id = unsplashImage.id)
            }
    }
}

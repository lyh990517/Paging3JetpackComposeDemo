package com.example.paging3demo.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.paging3demo.data.remote.UnsplashApi
import com.example.paging3demo.model.UnsplashImage
import com.example.paging3demo.util.Constants.ITEMS_PER_PAGE

/**
 * 검색어로 이미지를 로드하는 PagingSource 구현체
 */
class SearchPagingSource(
    private val unsplashApi: UnsplashApi, // 유니스플래시 이미지 검색 API 호출을 위한 인터페이스
    private val query: String // 검색어
) : PagingSource<Int, UnsplashImage>() {

    /**
     * 페이지 데이터를 로드하는 함수
     * @param params 로드 정보 (key: 현재 페이지, loadSize: 로드할 데이터 개수)
     * @return 로드 결과 (data: 로드된 데이터, prevKey: 이전 페이지 키, nextKey: 다음 페이지 키)
     */
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, UnsplashImage> {
        val currentPage = params.key ?: 1 // key가 없으면 1페이지부터 시작
        return try {
            val response = unsplashApi.searchImages(query = query, perPage = ITEMS_PER_PAGE) // 검색 API 호출
            val endOfPaginationReached = response.images.isEmpty() // 더 로드할 데이터가 없는지 확인

            // 데이터가 존재하는 경우
            if (response.images.isNotEmpty()) {
                LoadResult.Page(
                    data = response.images, // 로드된 데이터
                    prevKey = if (currentPage == 1) null else currentPage - 1, // 이전 페이지 키 (1페이지일 경우 null)
                    nextKey = if (endOfPaginationReached) null else currentPage + 1 // 다음 페이지 키 (마지막 페이지일 경우 null)
                )
                // 데이터가 없는 경우
            } else {
                LoadResult.Page(
                    data = emptyList(), // 비어있는 리스트 반환
                    prevKey = null, // 이전 페이지 키 없음
                    nextKey = null // 다음 페이지 키 없음
                )
            }
        } catch (e: Exception) {
            LoadResult.Error(e) // 에러 발생 시 에러 결과 반환
        }
    }

    /**
     *  리스트를 새로고침할 때 사용하는 키를 반환하는 함수
     *  @param state 현재 PagingState
     *  @return 새로고침할 때 사용할 페이지 키 (anchorPosition)
     */
    override fun getRefreshKey(state: PagingState<Int, UnsplashImage>): Int? {
        return state.anchorPosition
    }

}

package com.example.paging3demo.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.paging3demo.data.local.UnsplashDatabase
import com.example.paging3demo.data.paging.SearchPagingSource
import com.example.paging3demo.data.paging.UnsplashRemoteMediator
import com.example.paging3demo.data.remote.UnsplashApi
import com.example.paging3demo.model.UnsplashImage
import com.example.paging3demo.util.Constants.ITEMS_PER_PAGE
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@ExperimentalPagingApi
class Repository @Inject constructor(
    // 유니스플래시 이미지 검색 API 호출을 위한 인터페이스
    private val unsplashApi: UnsplashApi,
    // 로컬 데이터베이스 관리 클래스
    private val unsplashDatabase: UnsplashDatabase
) {

    // 모든 이미지를 로드하는 Flow<PagingData<UnsplashImage>> 반환
    fun getAllImages(): Flow<PagingData<UnsplashImage>> {
        return Pager(
            // 페이지당 로드할 아이템 개수 설정 (ITEMS_PER_PAGE 상수 사용)
            config = PagingConfig(pageSize = ITEMS_PER_PAGE),
            // 로컬과 원격 데이터 간 동기화 및 캐싱 처리
            remoteMediator = UnsplashRemoteMediator(
                unsplashApi = unsplashApi,
                unsplashDatabase = unsplashDatabase
            ),
            // 로컬 데이터베이스에서 데이터 로드하는 구현체 지정
            pagingSourceFactory = { unsplashDatabase.unsplashImageDao().getAllImages() }
        ).flow
    }

    // 검색어로 이미지 검색하는 Flow<PagingData<UnsplashImage>> 반환
    fun searchImages(query: String): Flow<PagingData<UnsplashImage>> {
        return Pager(
            // 페이지당 로드할 아이템 개수 설정 (ITEMS_PER_PAGE 상수 사용)
            config = PagingConfig(pageSize = ITEMS_PER_PAGE),
            // 검색 로직 구현체 지정
            pagingSourceFactory = { SearchPagingSource(unsplashApi = unsplashApi, query = query) }
        ).flow
    }

}

package com.sobrr.agnes.data.model.base

import com.google.gson.annotations.SerializedName

open class BaseResponse<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: T?
) {
    fun isSuccess(): Boolean = code == 0 || code == 200
}

open class BaseNoResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String
) {
    fun isSuccess(): Boolean = code == 0 || code == 200
}

data class Pagination(
    @SerializedName("page") val page: Int = 1,
    @SerializedName("page_size") val pageSize: Int = 20,
    @SerializedName("total") val total: Long = 0,
    @SerializedName("has_more") val hasMore: Boolean = false
)

data class PaginationInfo(
    @SerializedName("page") val page: Int = 1,
    @SerializedName("page_size") val pageSize: Int = 20,
    @SerializedName("total") val total: Long = 0,
    @SerializedName("has_more") val hasMore: Boolean = false,
    @SerializedName("cursor") val cursor: String? = null
)
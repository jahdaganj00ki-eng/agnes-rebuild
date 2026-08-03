package com.sobrr.agnes.data.network

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.sobrr.agnes.data.model.base.BaseNoResponse
import com.sobrr.agnes.data.model.base.BaseResponse
import java.io.IOException
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Response
import retrofit2.Retrofit

// ApiResult wrapper for handling success/error in a functional way
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String, val rawResponse: String?) : ApiResult<Nothing>()
}

// Custom CallAdapter for ApiResult
class ApiResultCallAdapterFactory : CallAdapter.Factory() {
    override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
        if (returnType is! kotlin.reflect.KParameterizedType) return null
        val responseType = returnType.arguments[0].type
        return ApiResultCallAdapter(responseType)
    }
}

class ApiResultCallAdapter<T>(private val responseType: Type) : CallAdapter<T, ApiResult<T>> {
    override fun responseType(): Type = responseType

    override fun adapt(call: Call<T>): ApiResult<T> {
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                response.body()?.let {
                    return ApiResult.Success(it)
                }
                return ApiResult.Error(0, "Empty response", null)
            } else {
                val errorBody = response.errorBody()?.string()
                return ApiResult.Error(response.code(), response.message() ?: "HTTP Error", errorBody)
            }
        } catch (e: IOException) {
            return ApiResult.Error(-1, e.message ?: "Network error", null)
        } catch (e: Exception) {
            return ApiResult.Error(-2, e.message ?: "Unknown error", null)
        }
    }
}

// Gson TypeAdapter for BaseResponse to handle the envelope
class BaseResponseAdapterFactory(private val gson: Gson) : com.google.gson.TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: com.google.gson.reflect.TypeToken<T>): TypeAdapter<T>? {
        val rawType = type.rawType
        if (!BaseResponse::class.java.isAssignableFrom(rawType) && !BaseNoResponse::class.java.isAssignableFrom(rawType)) {
            return null
        }
        @Suppress("UNCHECKED_CAST")
        val delegate = gson.getDelegateAdapter(this, type) as TypeAdapter<T>
        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T?) {
                delegate.write(out, value)
            }

            @Suppress("UNCHECKED_CAST")
            override fun read(reader: JsonReader): T {
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull()
                    return null as T
                }
                return delegate.read(reader)
            }
        }
    }
}

// Main API interface
interface Api {
    companion object {
        private const val CONNECT_TIMEOUT = 30L
        private const val READ_TIMEOUT = 60L
        private const val WRITE_TIMEOUT = 60L
    }
}
import com.example.splashscreen.InsertUsers
import com.example.splashscreen.Users
import com.example.splashscreen.GetUserByEmail
import com.example.splashscreen.ProductResponse
import com.example.splashscreen.Products
import com.example.splashscreen.Supplier
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @Headers("Content-Type: application/json")
    @POST("rpc/insert_user")
    fun insertUser(@Body request: InsertUsers): Call<Boolean>

    @Headers("Content-Type: application/json")
    @POST("rpc/get_user_email")
    suspend fun getUserByEmail(@Body request: Map<String, String>): Response<String>

    @GET("rpc/get_all_products")
    suspend fun getAllProducts(): Response<List<Products>>

    @GET("rpc/get_all_products")
    suspend fun getProducts(): Response<ProductResponse>

    @Headers("Content-Type: application/json")
    @POST("rpc/get_supplier_by_id")
    suspend fun getSupplierById(@Body request: Map<String, Long>): Response<List<Supplier>>
}


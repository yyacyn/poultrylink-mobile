import com.example.splashscreen.BuyerDetails
import com.example.splashscreen.CartItem
import com.example.splashscreen.InsertUsers
import com.example.splashscreen.Users
import com.example.splashscreen.GetUserByEmail
import com.example.splashscreen.InsertBuyer
import com.example.splashscreen.InsertCart
import com.example.splashscreen.ProductResponse
import com.example.splashscreen.Products
import com.example.splashscreen.Supplier
import com.example.splashscreen.UpdateBuyer
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

    @POST("rpc/get_user_id_by_email") // Replace with your actual endpoint
    suspend fun getUserIdByEmail(@Body requestBody: Map<String, String>): Response<Int>

    @POST("rpc/get_product_rating")
    suspend fun getProductRating(@Body request: Map<String, Long>): Response<List<Map<String, Any>>>

    @GET("rpc/get_product_reviews")
    suspend fun getProductReviews(@Query("product_id") productId: Long): Response<List<Map<String, Any>>>

    @Headers("Content-Type: application/json")
    @POST("rpc/get_same_category_products")
    suspend fun getSameCategoryProducts(@Body request: Map<String, Long>): Response<List<Products>>

    @POST("rpc/get_products_by_category")
    suspend fun getProductsByCategory(@Body requestBody: Map<String, String>): Response<List<Products>>

    @Headers("Content-Type: application/json")
    @POST("rpc/insert_buyer_for_user")
    fun insertBuyer(@Body request: InsertBuyer): Call<Boolean>

    @Headers("Content-Type: application/json")
    @POST("rpc/get_user_hashed_password") // Ensure this endpoint matches your Supabase function
    suspend fun getUserHashedPassword(@Body request: Map<String, String>): Response<String>

    @POST("rpc/update_buyer_profile")
    suspend fun updateUserProfile(@Body profileData: UpdateBuyer): Response<Boolean>

    @POST("rpc/get_buyer_details")
    suspend fun getBuyerDetails(@Body requestBody: Map<String, Long>): Response<List<BuyerDetails>>

    @POST("rpc/insert_cart_item")
    fun insertCartItem(@Body cart: InsertCart): Call<Boolean>

    @POST("rpc/get_user_cart_items")
    suspend fun getCartItems(@Body requestBody: Map<String, Long>): Response<List<Map<String, Any>>>

}


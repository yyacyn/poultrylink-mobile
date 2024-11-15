import com.example.splashscreen.Buyer
import com.example.splashscreen.BuyerData
import com.example.splashscreen.BuyerDetails
import com.example.splashscreen.BuyerProfileRequest
import com.example.splashscreen.BuyerResponse
import com.example.splashscreen.CartUpdateRequest
import com.example.splashscreen.InsertUsers
import com.example.splashscreen.InsertBuyer
import com.example.splashscreen.InsertCart
import com.example.splashscreen.InsertUser
import com.example.splashscreen.LoginResponse
import com.example.splashscreen.ProductResponse
import com.example.splashscreen.Products
import com.example.splashscreen.RegisterResponse
import com.example.splashscreen.ReviewResponse
import com.example.splashscreen.SupplierResponse
import com.example.splashscreen.UpdateProfileRequest
import com.example.splashscreen.UpdateProfileResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
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


    @POST("rpc/get_buyer_details")
    suspend fun getBuyerDetails(@Body requestBody: Map<String, Long>): Response<List<BuyerDetails>>

    @POST("rpc/insert_cart_item")
    fun insertCartItem(@Body cart: InsertCart): Call<Boolean>

    @POST("rpc/get_user_cart_items")
    suspend fun getCartItems(@Body requestBody: Map<String, Long>): Response<List<Map<String, Any>>>

    @Headers("Content-Type: application/json")
    @POST("rpc/update_cart_item")
    suspend fun updateCartItem(@Body request: CartUpdateRequest): Response<Boolean>

    @POST("register")
    fun registerUser(@Body request: InsertUser): Call<RegisterResponse>

    @POST("postprofilep")
    fun createBuyerProfilep(
        @Body buyerProfileRequest: BuyerProfileRequest): Call<BuyerResponse>

    @POST("postprofile")
    fun createBuyerProfile(
        @Header("Authorization") token: String,
        @Body buyerProfileRequest: BuyerProfileRequest): Call<BuyerResponse>

    @POST("login")
    suspend fun loginUser(@Body credentials: Map<String, String>): Response<LoginResponse>

    @GET("produk")
    fun getProduk(): Call<List<Products>>

    @GET("getprofile")
    fun getProfile(@Header("Authorization") token: String): Call<BuyerResponse>

    @GET("getallproducts")
    fun getProducts(@Header("Authorization") token: String): Call<ProductResponse>

    @GET("ulasanall")
    fun getReviews(@Header("Authorization") token: String): Call<ReviewResponse>

    @POST("logout")
    fun logout(@Header("Authorization") token: String): Call<Void>

    @GET("logoutmobile")
    fun logoutMobile(@Header("Authorization") token: String): Call<Map<String, Boolean>>

    @GET("ulasanall")
    fun getReviewsByProductId(@Header("Authorization") token: String, @Query("product_id") productId: Long): Call<ReviewResponse>

    @POST("updateprofile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest,
    ): Response<Any>

    @GET("getSupplier")
    fun getSupplier(@Header("Authorization") token: String): Call<SupplierResponse>

}


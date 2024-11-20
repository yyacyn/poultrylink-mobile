import com.example.splashscreen.Buyer
import com.example.splashscreen.BuyerData
import com.example.splashscreen.BuyerDetails
import com.example.splashscreen.BuyerProfileRequest
import com.example.splashscreen.BuyerResponse
import com.example.splashscreen.CancelOrderRequest
import com.example.splashscreen.CancelOrderResponse
import com.example.splashscreen.CartResponse
import com.example.splashscreen.CartUpdateRequest
import com.example.splashscreen.DeleteCartRequest
import com.example.splashscreen.DeleteCartResponse
import com.example.splashscreen.InsertUsers
import com.example.splashscreen.InsertBuyer
import com.example.splashscreen.InsertCart
import com.example.splashscreen.InsertCartData
import com.example.splashscreen.InsertCartResponse
import com.example.splashscreen.InsertOrder
import com.example.splashscreen.InsertUser
import com.example.splashscreen.LoginResponse
import com.example.splashscreen.OrderDetailResponse
import com.example.splashscreen.OrderResponse
import com.example.splashscreen.PostReviewResponse
import com.example.splashscreen.ProductResponse
import com.example.splashscreen.Products
import com.example.splashscreen.RegisterResponse
import com.example.splashscreen.RetrieveOrderRequest
import com.example.splashscreen.ReviewPostRequest
import com.example.splashscreen.ReviewResponse
import com.example.splashscreen.SupplierResponse
import com.example.splashscreen.UpdateCartRequest
import com.example.splashscreen.UpdateProfileRequest
import com.example.splashscreen.UpdateProfileResponse
import com.example.splashscreen.User
import com.example.splashscreen.Users
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {

    @POST("register")
    fun registerUser(@Body request: InsertUser): Call<RegisterResponse>

    @POST("postprofile")
    fun createBuyerProfile(@Header("Authorization") token: String, @Body buyerProfileRequest: BuyerProfileRequest): Call<BuyerResponse>

    @POST("login")
    suspend fun loginUser(@Body credentials: Map<String, String>): Response<LoginResponse>

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
    suspend fun updateProfile(@Header("Authorization") token: String, @Body request: UpdateProfileRequest, ): Response<Any>

    @GET("getSupplier")
    fun getSupplier(@Header("Authorization") token: String): Call<SupplierResponse>

    @GET("getallcarts")
    fun getAllCarts(@Header("Authorization") token: String): Call<CartResponse>

    @GET("me")
    fun getUser(@Header("Authorization") token: String): Call<Users>

    @POST("addtocartmobile")
    fun addToCart(@Header("Authorization") token: String, @Body insertCartRequest: InsertCartData): Call<InsertCartResponse>

    @POST("deletecartpost")
    fun deleteCart(@Header("Authorization") token: String, @Body request: DeleteCartRequest): Call<DeleteCartResponse>

    @POST("updatecart")
    fun updateCart(@Header("Authorization") token: String, @Body request: UpdateCartRequest): Call<CartResponse>

    @POST("order")
    fun createOrder(@Header("Authorization") token: String, @Body request: InsertOrder): Call<OrderResponse>

    @GET("orderdetailsall")
    fun getOrderDetail(@Header("Authorization") token: String): Call<OrderDetailResponse>

    @POST("cancelordermobile")
    fun cancelOrder(@Header("Authorization") token: String, @Body request: CancelOrderRequest): Call<CancelOrderResponse>

    @POST("retrieveordermobile")
    fun retrieveOrder(@Header("Authorization") token: String, @Body request: RetrieveOrderRequest): Call<OrderDetailResponse>

    @POST("confirmordermobile")
    fun confirmOrder(@Header("Authorization") token: String, @Body request: RetrieveOrderRequest): Call<CancelOrderResponse>

    @POST("ulasan")
    fun postReview(@Header("Authorization") token: String, @Body request: ReviewPostRequest): Call<PostReviewResponse>
}


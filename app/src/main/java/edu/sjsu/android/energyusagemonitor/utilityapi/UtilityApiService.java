package edu.sjsu.android.energyusagemonitor.utilityapi;

import edu.sjsu.android.energyusagemonitor.utilityapi.models.*;
import retrofit2.Call;
import retrofit2.http.*;

public interface UtilityApiService {
    // Below code not required for test data.
//    @POST("api/v2/forms")
//    Call<FormResponse> createForm(@Header("Authorization") String apiToken);
//
//    @POST("api/v2/forms/{formUid}/test-submit")
//    Call<TestSubmitResponse> testSubmit(
//            @Header("Authorization") String apiToken,
//            @Path("formUid") String formUid,
//            @Body TestSubmitRequest request
//    );
//
//    @GET("api/v2/authorizations")
//    Call<AuthorizationResponse> getAuthorizations(
//            @Header("Authorization") String apiToken,
//            @Query("referrals") String referral,
//            @Query("include") String include
//    );
//
//    @POST("api/v2/meters/historical-collection")
//    Call<HistoricalCollectionResponse> activateMeters(
//            @Header("Authorization") String apiToken,
//            @Body HistoricalCollectionRequest request
//    );
//
//    @GET("api/v2/meters/{uid}")
//    Call<MeterResponse> getMeter(
//            @Header("Authorization") String apiToken,
//            @Path("uid") String meterUid
//    );

    @GET("api/v2/bills")
    Call<BillsResponse> getBills(
            @Header("Authorization") String apiToken,
            @Query("meters") String meterUid
    );
}
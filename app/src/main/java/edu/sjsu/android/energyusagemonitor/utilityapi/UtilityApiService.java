package edu.sjsu.android.energyusagemonitor.utilityapi;

import edu.sjsu.android.energyusagemonitor.utilityapi.models.AuthorizationsResponse;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.BillsResponse;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.FormResponse;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.HistoricalCollectionRequest;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.HistoricalCollectionResponse;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.MeterResponse;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.TestSubmitRequest;
import edu.sjsu.android.energyusagemonitor.utilityapi.models.TestSubmitResponse;
import retrofit2.Call;
import retrofit2.http.*;

public interface UtilityApiService {
    @POST("api/v2/forms")
    Call<FormResponse> createForm(@Header("Authorization") String apiToken);

    @POST("api/v2/forms/{formUid}/test-submit")
    Call<TestSubmitResponse> testSubmitForm(
            @Header("Authorization") String apiToken,
            @Path("formUid") String formUid,
            @Body TestSubmitRequest request
    );

    @GET("api/v2/authorizations")
    Call<AuthorizationsResponse> getAuthorizations(
            @Header("Authorization") String apiToken,
            @Query("referrals") String referralCode,
            @Query("include") String include
    );

    @POST("api/v2/meters/historical-collection")
    Call<HistoricalCollectionResponse> activateMeters(
            @Header("Authorization") String apiToken,
            @Body HistoricalCollectionRequest request
    );

    @GET("api/v2/meters/{meterUid}")
    Call<MeterResponse> getMeter(
            @Header("Authorization") String apiToken,
            @Path("meterUid") String meterUid
    );

    @GET("api/v2/bills")
    Call<BillsResponse> getBills(
            @Header("Authorization") String apiToken,
            @Query("meters") String meterUid
    );
}
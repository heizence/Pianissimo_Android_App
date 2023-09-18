package com.example.pianissimo.Modules;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.pianissimo.Activities.MainActivity;
import com.example.pianissimo.R;
import com.google.gson.Gson;
import com.example.pianissimo.Modules.secretKeys;

public class httpRequestAPIs {
    /*
        sendGetRequest, sendPostRequest : 각 activity 에서 http 요청을 보내는 GET, POST method.

        activity 에서 import 해서 실행하는 방법

        1.import
        import com.example.pianissimo.Modules.httpRequestAPIs;

        2.콜백 함수를 전달해 줄 Callee_success, Callee_failed 객체 생성

        // 요청 성공 시 실행할 callback
        class Callee_success extends httpRequestAPIs.Callee_success {
            public void call(httpRequestAPIs.ResponseObject responseObj) {
                // 실행시킬 callback 함수 작성
            }
        }

        // 요청 실패 시 실행할 callback
        class Callee_failed extends httpRequestAPIs.Callee_failed {
            public void call(int statusCode) {
                // 실행시킬 callback 함수 작성
            }
        }

        3.요청하고자 하는 API 에 파라미터로 Callee_success, Callee_failed 객체 생성하여 넘겨주기
        public void doRequest() {
            httpRequestAPIs.SOME_API(...args, new Callee_success(), new Callee_failed());
        }
        */

    public static String emulator_ipAddress = "10.0.2.2";   // 에뮬레이터 private ip 주소

    public static boolean isHome = true;   // 재택근무 여부
    public static String ipAddressToUse = isHome ? secretKeys.HOME_IP_ADDRESS : secretKeys.OTHER_IP_ADDRESS;
    public static String MAIN_SERVER_URL = "http://" + ipAddressToUse + ":8000/";  // Main PHP server 주소
    public static Boolean isEmulator = checkIsEmulator.check();

    public static String signalingServer_ip_address = isEmulator ? emulator_ipAddress : ipAddressToUse;
    public static String webRTC_Signaling_Server_URL = "http://" + signalingServer_ip_address + ":8080";    // WebRTC signaling server 주소

    private static void sendGetRequest(
            // 파라미터 순서 함부로 바꾸지 말 것!
            String path,
            String params,
            Caller_success callerSuccess,
            Callee_success calleeSuccess,
            Caller_failed callerFailed,
            Callee_failed calleeFailed
    ) {
        Context applicationContext = MainActivity.getContextOfApplication();    // Context from main activity.
        RequestQueue queue = Volley.newRequestQueue(applicationContext);    // 요청 전송을 위한 RequestQueue 생성

        String url = MAIN_SERVER_URL + path + ".php" + params;  // 요청을 보내줄 URL 주소
        String authToken = "Bearer " + new JWT_Token().token;
        System.out.println("## [HTTP API] check authToken : " + authToken);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", authToken);

        // RequestQueue 에 추가해 줄 요청 객체 생성
        // 요청 실패 시 실행해 줄 매서드
        // 요청 성공 시 실행해 줄 매서드

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    System.out.println("## [HTTP API] onResponse : " + response);

                    // string 형식의 응답을 사용 가능한 객체 형식으로 변환
                    Gson gson = new Gson();
                    ResponseObject responseObj = gson.fromJson(response, ResponseObject.class);

                    // 요청이 성공일 경우(statusCode 가 200 번대)
                    // http 요청 보낸 activity 에서 정의한 성공 콜백 실행하기
                    callerSuccess.register(calleeSuccess, responseObj);
                }, error -> {
            if (error.networkResponse != null) {
                System.out.println("## [HTTP API] onError : " + error);
            } else {
                int statusCode = error.networkResponse.statusCode;

                // 잘못된 JWT 로 요청 시도 시 에러 발생시키기
                if (statusCode == 401) {
                    System.out.println("## [HTTP API] 401 unauthorized. 잘못된 요청입니다!");
                } else {
                    // 요청이 실패일 경우
                    // http 요청 보낸 activity 에서 정의한 실패 콜백 실행하기
                    callerFailed.register(calleeFailed, statusCode);
                }
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                return headers;
            }
        };
        // Set the retry policy for the request
        int initialTimeoutMs = 10000; // Initial timeout duration in milliseconds
        int maxNumRetries = 3; // Maximum number of retries
        float backoffMultiplier = 2.0f; // Backoff multiplier for exponential backoff
        RetryPolicy retryPolicy = new DefaultRetryPolicy(
                initialTimeoutMs,
                maxNumRetries,
                backoffMultiplier);

        stringRequest.setRetryPolicy(retryPolicy);
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private static void sendPostRequest(
            // 파라미터 순서 함부로 바꾸지 말 것!
            String path,
            HashMap<String, String> params,
            Caller_success callerSuccess,
            Callee_success calleeSuccess,
            Caller_failed callerFailed,
            Callee_failed calleeFailed
    ) {
        Context applicationContext = MainActivity.getContextOfApplication();    // Context from main activity.
        RequestQueue queue = Volley.newRequestQueue(applicationContext);    // 요청 전송을 위한 RequestQueue 생성

        String url = MAIN_SERVER_URL + path + ".php";  // 요청을 보내줄 URL 주소
        String authToken = "Bearer " + new JWT_Token().token;
        System.out.println("## [HTTP API] check authToken : " + authToken);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", authToken);

        // RequestQueue 에 추가해 줄 요청 객체 생성
        // 요청 실패 시 실행해 줄 매서드
        // 요청 성공 시 실행해 줄 매서드
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    System.out.println("## [HTTP API] onResponse : " + response);

                    // string 형식의 응답을 사용 가능한 객체 형식으로 변환
                    Gson gson = new Gson();
                    ResponseObject responseObj = gson.fromJson(response, ResponseObject.class);

                    // 요청이 성공일 경우(statusCode 가 200 번대)
                    // http 요청 보낸 activity 에서 정의한 성공 콜백 실행하기
                    callerSuccess.register(calleeSuccess, responseObj);
                }, error -> {
            System.out.println("## [HTTP API] onError : " + error);
            if (error != null) {
                if (error.networkResponse != null) {
                    System.out.println("## [HTTP API] error.networkResponse : " + error.networkResponse);
                } else {
                    return;
                }
            }
            int statusCode = error.networkResponse.statusCode;

            // 잘못된 JWT 로 요청 시도 시 에러 발생시키기
            if (statusCode == 401) {
                System.out.println("## [HTTP API] 401 unauthorized. 잘못된 요청입니다!");
            } else {
                // 요청이 실패일 경우
                // http 요청 보낸 activity 에서 정의한 실패 콜백 실행하기
                callerFailed.register(calleeFailed, statusCode);
            }

        }) {
            @Override
            protected Map<String, String> getParams() {
                return params;
            }

            @Override
            public Map<String, String> getHeaders() {
                return headers;
            }
        };

        // Set the retry policy for the request
        int initialTimeoutMs = 10000; // Initial timeout duration in milliseconds
        int maxNumRetries = 3; // Maximum number of retries
        float backoffMultiplier = 2.0f; // Backoff multiplier for exponential backoff
        RetryPolicy retryPolicy = new DefaultRetryPolicy(
                initialTimeoutMs,
                maxNumRetries,
                backoffMultiplier);

        stringRequest.setRetryPolicy(retryPolicy);
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }


    /*************************** 콜백 함수 전달을 위한 클래스 **************************/
    // 콜백 함수 전달을 위한 interface(요청 성공 시)
    interface Callback_success {
        void call(ResponseObject data);
    }

    // 콜백 함수 전달을 위한 interface(요청 실패 시)
    interface Callback_failed {
        void call(int statusCode);
    }

    // 호출을 하는 주체(요청 성공 시)
    static class Caller_success {
        public void register(Callback_success callback, ResponseObject resObj) {
            callback.call(resObj);
        }
    }

    // 호출을 하는 주체(요청 실패 시)
    static class Caller_failed {
        public void register(Callback_failed callback, int statusCode) {
            callback.call(statusCode);
        }
    }

    // 요청 후 콜백 함수를 실행시키는 주체(요청 성공 시)
    public static class Callee_success implements Callback_success {
        public void call(ResponseObject resObj) {
        }  // Do not delete!
    }

    // 요청 후 콜백 함수를 실행시키는 주체(요청 실패 시)
    public static class Callee_failed implements Callback_failed {
        public void call(int statusCode) {
        }  // Do not delete!
    }

    /*****************************************************/

    // http GET 요청을 위한 요청 params 생성
    public static class RequestParams {
        private String paramString;

        public RequestParams() {
            paramString = "?";
        }

        public void addKeyValuePair(String key, String value) {
            // 파라미터가 없는 상태에서 추가할 때는 그대로 추가
            if (paramString.equals("?")) {
                paramString += key + "=" + value;
            }
            // 파라미터가 았는 상태에서 추가할 때는 & 와 같이 추가
            else {
                paramString += "&" + key + "=" + value;
            }
        }

        public String getParams() {
            return paramString;
        }
    }

    // http POST 요청을 위한 요청 객체 생성
    public static class RequestObject {
        private HashMap<String, String> keyValuePairs;

        public RequestObject() {
            keyValuePairs = new HashMap<String, String>();
        }

        public void addKeyValuePair(String key, String value) {
            keyValuePairs.put(key, value);
        }

        public HashMap<String, String> getKeyValuePairs() {
            return keyValuePairs;
        }
    }

    // http 요청 후 받는 응답 객체 생성
    // 서버에서 정의된 응답 객체 형식과 맞아야 됨! 변경할 때 서버 쪽도 꼭 같이 확인하기
    public static class ResponseObject {
        public String message;
        public String statusCode;

        /*
        서버에서 응답으로 받을 때 data 의 최초 형태는 string 임.
        Gson 을 이용해서 숫자, 문자, 배열, 객체 등 원래 서버에서 보내줬던 형식으로 변환해서 사용.
        */
        public String data;

        public ResponseObject(String messageInput, String statusCodeInput, String dataInput) {
            this.message = messageInput;
            this.statusCode = statusCodeInput;
            this.data = dataInput;
        }
    }

    /*************************** API 목록 **************************/

    // http API 요청 시 필요한 토큰 가져오는 class
    public static class JWT_Token {
        public String token;

        public JWT_Token() {
            // sharedPreference 에 저장된 token 불러오기
            Context applicationContext = MainActivity.getContextOfApplication();    // From main activity.
            SharedPreferences sharedPref = applicationContext.getSharedPreferences(
                    applicationContext.getString(R.string.sharedPreferenceMain), Context.MODE_PRIVATE);
            String token = sharedPref.getString(applicationContext.getString(R.string.store_app_token), "");
            this.token = token;
        }
    }

    // 로그인
    public static void signin(String email, String password, Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestObject obj = new RequestObject();
        obj.addKeyValuePair("email", email);
        obj.addKeyValuePair("password", password);
        sendPostRequest("app/users/signin", obj.getKeyValuePairs(),
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    // 비밀번호 재발급
    public static void reissuePassword(String email, Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestObject obj = new RequestObject();
        obj.addKeyValuePair("email", email);
        sendPostRequest("app/users/reissuePassword", obj.getKeyValuePairs(),
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    // 비밀번호 변경
    public static void editPassword(String newPassword, Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestObject obj = new RequestObject();
        //obj.addKeyValuePair("token", new JWT_Token().token);
        obj.addKeyValuePair("newPassword", newPassword);
        sendPostRequest("app/users/editPassword", obj.getKeyValuePairs(),
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    // 아이디(이메일) 찾기
    public static void findId(String phoneNumber, Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestObject obj = new RequestObject();
        obj.addKeyValuePair("phoneNumber", phoneNumber);
        sendPostRequest("app/users/findId", obj.getKeyValuePairs(),
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    // 공지사항 데이터 불러오기
    public static void getNotices(int pageIndex, Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestParams params = new RequestParams();
        params.addKeyValuePair("pageIndex", String.valueOf(pageIndex));
        sendGetRequest("app/notice/getNotices", params.getParams(),
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    // 전체 레슨 예약 상태 불러오기
    public static void getLessons(String startDate, String endDate, String instructorName, Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestParams params = new RequestParams();
        params.addKeyValuePair("startDate", startDate);
        params.addKeyValuePair("endDate", endDate);
        params.addKeyValuePair("instructorName", instructorName);
        sendGetRequest("app/lesson/getLessons", params.getParams(),
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    // 강사 이름 리스트 불러오기
    public static void getInstructorsName(Callee_success calleeSuccess, Callee_failed calleeFailed) {
        sendGetRequest("app/lesson/getInstructorsName", "",
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    // 내 레슨 예약 현황 불러오기
    public static void getMyLessonStatus(int pageIndex, String studentId, Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestParams params = new RequestParams();
        params.addKeyValuePair("pageIndex", String.valueOf(pageIndex));
        params.addKeyValuePair("studentId", studentId);
        sendGetRequest("app/lesson/getMyLessonStatus", params.getParams(),
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    // 레슨 예약하기
    public static void bookLesson(String studentId, String lessonId, Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestObject obj = new RequestObject();
        obj.addKeyValuePair("studentId", studentId);
        obj.addKeyValuePair("lessonId", lessonId);
        sendPostRequest("app/lesson/bookLesson", obj.getKeyValuePairs(),
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    // 예약된 레슨 취소하기
    public static void cancelLesson(String lessonId, Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestObject obj = new RequestObject();
        obj.addKeyValuePair("lessonId", lessonId);
        sendPostRequest("app/lesson/cancelLesson", obj.getKeyValuePairs(),
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    // 내 레슨 내역 불러오기
    public static void getMyLessonHistory(int pageIndex, String studentId, Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestParams params = new RequestParams();
        params.addKeyValuePair("pageIndex", String.valueOf(pageIndex));
        params.addKeyValuePair("studentId", studentId);
        sendGetRequest("app/lesson/getMyLessonHistory", params.getParams(),
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    // 레슨 평가하기
    public static void rateLesson(String lessonId, int rateNumber, Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestObject obj = new RequestObject();
        obj.addKeyValuePair("lessonId", lessonId);
        obj.addKeyValuePair("rateNumber", String.valueOf(rateNumber));
        sendPostRequest("app/lesson/rate", obj.getKeyValuePairs(),
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    // 연습실 예약 현황 불러오기
    public static void getPracticeRoomBookStatus(String startDate, String endDate, String roomId, Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestParams params = new RequestParams();
        params.addKeyValuePair("startDate", startDate);
        params.addKeyValuePair("endDate", endDate);
        params.addKeyValuePair("roomId", roomId);
        sendGetRequest("app/practiceRoom/getBookStatus", params.getParams(),
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    // 연습실 예약하기
    public static void bookPracticeRoom(String studentId, String roomId, String roomUsageDate, int roomUsageStartTime, Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestObject obj = new RequestObject();
        obj.addKeyValuePair("studentId", studentId);
        obj.addKeyValuePair("roomId", roomId);
        obj.addKeyValuePair("roomUsageDate", roomUsageDate);
        obj.addKeyValuePair("roomUsageStartTime", String.valueOf(roomUsageStartTime));

        sendPostRequest("app/practiceRoom/bookPracticeRoom", obj.getKeyValuePairs(),
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    // 내 연습실 예약 현황 불러오기
    public static void getMyPracticeRoomStatus(int pageIndex, String studentId, Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestParams params = new RequestParams();
        params.addKeyValuePair("pageIndex", String.valueOf(pageIndex));
        params.addKeyValuePair("studentId", studentId);
        sendGetRequest("app/practiceRoom/getMyPracticeRoomStatus", params.getParams(),
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    // 예약된 연습실 취소하기
    public static void cancelPracticeRoom(String bookStatusId, Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestObject obj = new RequestObject();
        obj.addKeyValuePair("bookStatusId", bookStatusId);
        sendPostRequest("app/practiceRoom/cancelPracticeRoom", obj.getKeyValuePairs(),
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    // 연습실 이름 리스트 불러오기
    public static void getPracticeRoomsName(Callee_success calleeSuccess, Callee_failed calleeFailed) {
        sendGetRequest("app/practiceRoom/getPracticeRoomsName", "",
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    // 마이페이지 사용자 정보 불러오기
    public static void getUserInfo(String studentId, Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestParams params = new RequestParams();
        params.addKeyValuePair("studentId", String.valueOf(studentId));
        sendGetRequest("app/users/getUserInfo", params.getParams(),
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    // 프로필 이미지 변경하기
    public static void editProfileImg(String studentId, String imageFileString, Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestParams params = new RequestParams();
        RequestObject obj = new RequestObject();
        obj.addKeyValuePair("studentId", studentId);
        obj.addKeyValuePair("imageFileString", imageFileString);
        sendPostRequest("app/users/editProfileImg", obj.getKeyValuePairs(),
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    // 학원 결제내역 불러오기
    public static void getPaymentHistory(int pageIndex, String studentId, Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestParams params = new RequestParams();
        params.addKeyValuePair("pageIndex", String.valueOf(pageIndex));
        params.addKeyValuePair("studentId", studentId);
        sendGetRequest("app/users/getPaymentHistories", params.getParams(),
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    // 실시간 방송방 생성하기
    public static void createLiveRoomData(String roomId, String hostId, String roomName, String liveStartedAt, String thumbnailImage, int numberOfWatchers,
                                          Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestObject obj = new RequestObject();
        obj.addKeyValuePair("roomId", roomId);
        obj.addKeyValuePair("hostId", hostId);
        obj.addKeyValuePair("roomName", roomName);
        obj.addKeyValuePair("liveStartedAt", liveStartedAt);
        obj.addKeyValuePair("thumbnailImage", thumbnailImage);
        obj.addKeyValuePair("numberOfWatchers", String.valueOf(numberOfWatchers));
        sendPostRequest("app/broadcast/createLiveRoom", obj.getKeyValuePairs(),
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    // 실시간 방송 종료 후 데이터 삭제 생성하기
    public static void deleteLiveRoomData(String roomId, Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestObject obj = new RequestObject();
        obj.addKeyValuePair("roomId", roomId);
        sendPostRequest("app/broadcast/deleteLiveRoom", obj.getKeyValuePairs(),
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    // 방송 목록 불러오기(실시간, 지난 방송 모두 포함)
    public static void getBroadCastData(int pageIndex, Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestParams params = new RequestParams();
        params.addKeyValuePair("pageIndex", String.valueOf(pageIndex));
        sendGetRequest("app/broadcast/getBroadCastData", params.getParams(),
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    /*****************************************************/

    // 방송방 실시간 시청자 수 업데이트
    public static void updateNumberOfWatchers(String roomId, int updatedNumberOfWatchers, Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestObject obj = new RequestObject();
        obj.addKeyValuePair("roomId", roomId);
        obj.addKeyValuePair("updatedNumberOfWatchers", String.valueOf(updatedNumberOfWatchers));
        sendPostRequest("app/broadcast/updateNumberOfWatchers", obj.getKeyValuePairs(),
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    // 방송방 실시간 채팅 내역 저장하기
    public static void saveChatHistory(String roomId, String senderUserId, String chatMessage, int timeLapsedMillis, Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestObject obj = new RequestObject();
        obj.addKeyValuePair("roomId", roomId);
        obj.addKeyValuePair("senderUserId", senderUserId);
        obj.addKeyValuePair("chatMessage", chatMessage);
        obj.addKeyValuePair("timeLapsed", String.valueOf(timeLapsedMillis));

        sendPostRequest("app/broadcast/saveChatHistory", obj.getKeyValuePairs(),
                new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    /************************** 테스트용 API 목록 **************************/

    public static void getTest(Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestParams params = new RequestParams();
        params.addKeyValuePair("param1", "this is param1");
        params.addKeyValuePair("param2", "this is param2");
        params.addKeyValuePair("param3", "this is param3");
        params.addKeyValuePair("param4", "this is param4");
        sendGetRequest("requestTest", params.getParams(), new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    public static void postTest(Callee_success calleeSuccess, Callee_failed calleeFailed) {
        RequestObject obj = new RequestObject();
        obj.addKeyValuePair("params", "value1");
        sendPostRequest("requestTest", obj.getKeyValuePairs(), new Caller_success(), calleeSuccess, new Caller_failed(), calleeFailed);
    }

    /********************************************************************/
}

package com.coding404.demo.aws.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LambdaException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
public class S3Service {
	
//	//어세스키
//	@Value("${aws_access_key_id}")
//	private String aws_access_key_id;
//	//시크릿키
//	@Value("${aws_secret_access_key}")
//	private String aws_secret_access_key; 
		

	@Autowired
	private S3Client s3;
	
	@Autowired
	private LambdaClient lambdaClient;
	
	//버킷명
	@Value("${aws_bucket_name}")
	private String aws_bucket_name;
	
	

	public void getBucketList() {
		
//		Region region = Region.AP_NORTHEAST_2;
		
		//1st - 외부파일로 사용하는방법
//		//자격증명객체
//		ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.create();
//		//s3클라이언트
//		S3Client s3 = S3Client.builder()
//				.region(region)
//				.credentialsProvider(credentialsProvider)
//				.build();

		//2nd - 어플리케이션에 직접 작성하는 방법
//		AwsBasicCredentials credentials = AwsBasicCredentials.create(aws_access_key_id, aws_secret_access_key);
//		StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);
//		
//		//s3클라이언트
//		S3Client s3 = S3Client.builder()
//				.region(region)
//				.credentialsProvider(credentialsProvider)
//				.build();
		
		///////////////////////////////////////////////////////////////////////////////
		//자격증명객체를 빈으로 관리
		//s3기능사용
		
        // List buckets
        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
        ListBucketsResponse listBucketsResponse = s3.listBuckets(listBucketsRequest);
        listBucketsResponse.buckets().stream().forEach(x -> System.out.println(x.name()));
		
	}

	//s3파일업로드
	public void putS3Object(String originName, byte[] originData) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("x-amz-meta-myVal", "test");
            PutObjectRequest putOb = PutObjectRequest.builder()
                .bucket(aws_bucket_name) //버킷명
                .key(originName) //파일명
                .metadata(metadata)
                .build();

            //s3.putObject(putOb, RequestBody.fromFile(new File(objectPath))  ); //로컬파일 업로드시
            PutObjectResponse response = s3.putObject(putOb, RequestBody.fromBytes(originData)  );
            System.out.println("Successfully placed " + originName +" into bucket "+aws_bucket_name);

            
            System.out.println("성공실패여부:" + response.sdkHttpResponse().statusCode() ); //성공실패여부
            
            
            ////////////////////////////////////////////////////////////////////////
            //데이터베이스에 어떤 데이터를 넣어놓을 것인지?????
            
            
        } catch (S3Exception e) {
            System.err.println(e.getMessage());
            //System.exit(1); 
        }
	}
	
	//버킷의 객체 목록보기
    public void listBucketObjects() {

        try {
            ListObjectsRequest listObjects = ListObjectsRequest
                .builder()
                .bucket(aws_bucket_name)
                .build();

            ListObjectsResponse res = s3.listObjects(listObjects);
            List<S3Object> objects = res.contents();
            for (S3Object myValue : objects) {
                System.out.print("\n The name of the key is " + myValue.key());
                System.out.print("\n The object is " + (myValue.size() / 1024) + " KBs");
                System.out.print("\n The owner is " + myValue.owner());
            }

        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            //System.exit(1);
        }
    }
	
    
    //버킷 객체 삭제
    public void deleteBucketObjects(String keyName) {

        ArrayList<ObjectIdentifier> keys = new ArrayList<>();
        
        //삭제할 객체
        ObjectIdentifier objectId = ObjectIdentifier.builder()
            .key(keyName)
            .build();

        //리스트에 추가
        keys.add(objectId);
  
        // Delete multiple objects in one request.
        Delete del = Delete.builder()
            .objects(keys)
            .build();

        try {
            DeleteObjectsRequest multiObjectDeleteRequest = DeleteObjectsRequest.builder()
                .bucket(aws_bucket_name)
                .delete(del)
                .build();

            //삭제요청
            DeleteObjectsResponse result = s3.deleteObjects(multiObjectDeleteRequest);
            
            System.out.println("Multiple objects are deleted!");
            System.out.println(result.sdkHttpResponse().statusCode());
            
        
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            //System.exit(1);
        }
    }
    
	///////////////////////////////lambda호출//////////////////////////
    
    public void invokeFunction() {
    	
    	//aws sdk => 자격증명얻음 => 실행시킬 서비스 객체생성 => 호출
    	
    	//이거를 설정파일에 옴기셔도 됩니다.
		//자격증명을 생성하는 객체입니다.
//		AwsBasicCredentials credentials = AwsBasicCredentials.create(aws_access_key_id, aws_secret_access_key);
//
//		//람다를 핸들링 하기위한 람다를 생성합니다.
//		
//		LambdaClient awsLambda = LambdaClient.builder()
//				.region(Region.AP_NORTHEAST_2)//서울 리전
//				.credentialsProvider(StaticCredentialsProvider.create(credentials))
//				.build();
    	
    	
    	
    	//실행시킬 람다함수
    	String functionName = "demo-api-hello";
    	
    	
        InvokeResponse res = null ;
       try {
           //Need a SdkBytes instance for the payload
           String json = "{\"Hello \":\"람다야 이거좀 받아가라\"}";
           SdkBytes payload = SdkBytes.fromUtf8String(json) ;

           //Setup an InvokeRequest
           InvokeRequest request = InvokeRequest.builder()
                   .functionName(functionName)
                   .payload(payload)
                   .build();

           res = lambdaClient.invoke(request);
           
           String value = res.payload().asUtf8String() ;
           System.out.println(value);

       } catch(LambdaException e) {
           System.err.println(e.getMessage());
           //System.exit(1);
       }
   }
    
    
    
    
    
	
	
	
	
	
	

}

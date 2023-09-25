package com.coding404.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.coding404.demo.aws.service.S3Service;
import com.coding404.demo.aws.service.SesService;

import software.amazon.awssdk.services.sqs.model.Message;

@RestController
public class CloudRestController {

	//s3, 람다
	@Autowired
	private S3Service s3;
	
	//ses, sns, sqs
	@Autowired
	private SesService ses;
	
	
	@PostMapping("/cloudUpload")
	public ResponseEntity<String> cloudUpload(@RequestParam("file_data") MultipartFile file) {
		
		//System.out.println(file);
		
		try {
			//파일명
			String originName = file.getOriginalFilename();
			//파일데이터
			byte[] originData = file.getBytes();
			
			s3.putS3Object(originName, originData);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return new ResponseEntity<>("응답데이터는 여러분이 알아서 처리", HttpStatus.OK);
	}
	
	//버킷의 객체 목록확인
	@GetMapping("/list_bucket_objects")
	public ResponseEntity<String> list_bucket_objects() {
		
		s3.listBucketObjects();
		
		return new ResponseEntity<>("응답데이터는 여러분이 알아서 처리", HttpStatus.OK);
	}
	
	//버킷의 객체 삭제
	@DeleteMapping("/delete_bucket_object")
	public ResponseEntity<String > delete_bucket_object(@RequestParam("bucket_obj_name") String bucket_obj_name ) {
		
		s3.deleteBucketObjects(bucket_obj_name);
		
		return new ResponseEntity<>("응답데이터는 여러분이 알아서 처리", HttpStatus.OK);
	}
	
	
	///////////////////////////////lambda호출//////////////////////////
	
	//람다함수 호출
	@GetMapping("/lambda_call")
	public ResponseEntity<String> lambda_call() {
		
		s3.invokeFunction();
		
		return new ResponseEntity<>("응답데이터는 여러분이 알아서 처리", HttpStatus.OK);
	}
	
	
	
	/////////////////////////////SES//////////////////////////////////
	@GetMapping("/send_email")
	public ResponseEntity<String> send_email() {

		//이 데이터 화면에서 전달받을 수 있도록 처리.
		String sender = ""; //발신자 주소
		String recipient = ""; //수신자 주소
		String subject = "Amazon SES test (AWS SDK for Java)"; //제목
		String HTMLBODY = "<h1>Amazon SES test (AWS SDK for Java)</h1>"
			      + "<p>This email was sent with <a href='https://aws.amazon.com/ses/'>"
			      + "Amazon SES</a> using the <a href='https://aws.amazon.com/sdk-for-java/'>" 
			      + "AWS SDK for Java</a>";
		
		ses.sendEmail(sender, recipient, subject, HTMLBODY);
		
		return new ResponseEntity<>("응답데이터는 여러분이 알아서 처리", HttpStatus.OK);
	}

	
	//sns주제게시
	@GetMapping("/send_sns")
	public ResponseEntity<String> send_sns() {
		
		ses.sendSns();
		
		return new ResponseEntity<>("응답", HttpStatus.OK);
	}
	
	
	//sqs메시지 당기기
	@GetMapping("/poll_sqs")
	public ResponseEntity<String> poll_sqs() {
		
		List<Message> list = ses.pollSqs();
		
		System.out.println("===============================================");
		for(Message m : list) {
			System.out.println( m.body() );
		}
		//메시지 소비후에는 삭제 처리 작업(중복 메시지 수신 방지)
		
		
		return new ResponseEntity<>("응답", HttpStatus.OK);
	}
	
	
	
	
	
	
	
	
}

package com.movieflix.movieApi.controllers;

import com.movieflix.movieApi.auth.entities.ForgotPassword;
import com.movieflix.movieApi.auth.entities.User;
import com.movieflix.movieApi.auth.repositories.ForgotPasswordRepository;
import com.movieflix.movieApi.auth.repositories.UserRepository;
import com.movieflix.movieApi.dto.MailBody;
import com.movieflix.movieApi.service.EmailService;
import com.movieflix.movieApi.utils.ChangePassword;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Random;

@RestController
@RequestMapping("/forgotPassword")
public class ForgotPasswordController {
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ForgotPasswordRepository forgotPasswordRepository;
    private final PasswordEncoder passwordEncoder;

    public ForgotPasswordController(UserRepository userRepository, EmailService emailService, ForgotPasswordRepository forgotPasswordRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.forgotPasswordRepository = forgotPasswordRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // send mail for email verification
    @PostMapping("/verifyMail/{email}")
    public ResponseEntity<String> verifyEmail(@PathVariable String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Please provide a valid email"));

        int otp = otpGenerator();
        MailBody mailBody = MailBody.builder()
                .to(email)
                .text("This is the otp for your Forgot password Request")
                .subject("OTP for Forgot Password Request ")
                .build();
        ForgotPassword fp = ForgotPassword.builder()
                .otp(otp).expirationTime(new Date(System.currentTimeMillis() + 70 * 1000))
                .user(user)
                .build();

        emailService.sendSimpleMessage(mailBody);
        forgotPasswordRepository.save(fp);
        return ResponseEntity.ok("Email sent for verification");


    }

    @PostMapping("/verifyOtp?{otp}/{email}")
    public ResponseEntity<String> verifyOtp(@PathVariable Integer otp, @PathVariable String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Please provide a valid email"));
        ForgotPassword fp = forgotPasswordRepository.findByOtpAndUser(otp, user).orElseThrow(() -> new RuntimeException("Invalid OTP for email" + email));

        if (fp.getExpirationTime().before(Date.from(Instant.now()))) {
            forgotPasswordRepository.deleteById(fp.getFpid());
            return new ResponseEntity<>("OTP has expired ! ", HttpStatus.EXPECTATION_FAILED);
        }

        return ResponseEntity.ok("OTP Verified");
    }

    @PostMapping("/changePassword/{email}")
    public ResponseEntity<String> changePasswordHandler(@RequestBody ChangePassword changePassword,
                                                        @PathVariable String email) {
        if (!Objects.equals(changePassword.password(), changePassword.repeatPassword())) {
            return new ResponseEntity<>("Pls enter the password again! ", HttpStatus.EXPECTATION_FAILED);
        }
        String encodedPassword = passwordEncoder.encode(changePassword.password());
        userRepository.updatePassword(email,encodedPassword);
        return  ResponseEntity.ok("Password   has been changes !");

    }


    private Integer otpGenerator() {
        Random random = new Random();
        return random.nextInt(100_000, 999_999);
    }


}

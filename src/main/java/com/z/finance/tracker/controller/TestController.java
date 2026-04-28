package com.z.finance.tracker.controller;

import com.z.finance.tracker.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class TestController {

//    @Autowired
//    UserService userService;
//
//    @GetMapping("/{id}")
//    public ResponseEntity<User> getUserById(@PathVariable Long id){
//        User user = userService.getUserById(id);
//        return ResponseEntity.ok(user);
//    }
//
//    @PostMapping
//    public ResponseEntity<User> insertUser(@RequestBody User user) {
//       User saved = userService.insertUser(user);
//       return ResponseEntity.status(HttpStatus.CREATED).body(saved);
//    }


    public static void main(String[] args) {
        List<String> list = List.of("A", "B", "C");
        Map<String, Integer> collect = list.stream().collect(Collectors.toMap(a -> a, a -> a.length()));
        System.out.println(collect.get("A"));
    }
}

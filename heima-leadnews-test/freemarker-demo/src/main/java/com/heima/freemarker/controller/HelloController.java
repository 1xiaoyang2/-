package com.heima.freemarker.controller;

import com.heima.freemarker.entity.Student;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class HelloController {

    @GetMapping("/basic")
    public String hello(Model model){
        model.addAttribute("name","freemarker");
        Student student = new Student();
        student.setAge(18);
        student.setName("paopao");
        model.addAttribute("stu",student);
        return "01-basic";
    }
}

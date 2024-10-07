package com.tbfg.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.tbfg.dao.ProfessorDAO;
import com.tbfg.dao.UserDAO;
import com.tbfg.dto.ProfessorDTO;
import com.tbfg.dto.UserDTO;

import jakarta.servlet.http.HttpSession;

@Controller
public class MyPageController {

    @Autowired
    private UserDAO userDAO;  // UserDAO 객체를 주입받음
    @Autowired
    private ProfessorDAO professorDAO;  // ProfessorDAO 객체를 주입받음
    

    // 마이페이지를 보여주는 메서드
    @GetMapping("/myPage")
    public String showMyPage(HttpSession session, Model model) {
        // 세션에서 로그인된 사용자 정보를 가져옴
        Object user = session.getAttribute("loggedInUser");

        // 만약 사용자 정보가 없으면, 세션 만료로 간주하고 에러 메시지와 함께 로그인 페이지로 이동
        if (user == null) {
            model.addAttribute("error", "세션이 만료되었습니다. 다시 로그인 해주세요.");
            return "login"; // 로그인 페이지로 리다이렉트
        }

        // 사용자 정보를 모델에 추가하여 마이페이지를 렌더링
        model.addAttribute("user", user);
        return "myPage"; // 마이페이지로 이동
    }
    
    

    @PostMapping("/updateUser")
    @ResponseBody
    public Map<String, Object> updateUser(@RequestParam String id,
                                          @RequestParam(required = false) String oldPassword,
                                          @RequestParam(required = false) String newPassword,
                                          @RequestParam(required = false) String major,
                                          @RequestParam(required = false) Integer grade,
                                          @RequestParam(required = false) String classNumber,
                                          HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        // ID를 통해 사용자가 교수인지 학생인지 확인
        boolean isProfessor = professorDAO.isProExists(id);
        Object user = isProfessor ? professorDAO.getProById(id) : userDAO.getUserById(id);

        // 사용자가 존재하지 않는 경우
        if (user == null) {
            response.put("success", false);
            response.put("message", "사용자를 찾을 수 없습니다.");
            return response;
        }

        // 비밀번호 변경의 경우
        if (oldPassword != null && newPassword != null) {
            boolean passwordCorrect = isProfessor ? 
                professorDAO.checkProPassword(id, oldPassword) : 
                userDAO.checkPassword(id, oldPassword);

            if (passwordCorrect) {
                if (isProfessor) {
                    ProfessorDTO professor = (ProfessorDTO) user;
                    professor.setPass(newPassword);
                    professorDAO.updatePro(professor);
                } else {
                    UserDTO student = (UserDTO) user;
                    student.setPass(newPassword);
                    userDAO.updateUser(student);
                }
                session.setAttribute("loggedInUser", user);
                response.put("success", true);
                response.put("message", "비밀번호가 성공적으로 변경되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "기존 비밀번호가 일치하지 않습니다.");
            }
        } 
        // 학년/반 수정의 경우 (학생만 해당)
        else if (grade != null || classNumber != null) {
            if (user instanceof UserDTO) {
                UserDTO student = (UserDTO) user;
                if (grade != null) student.setGrade(grade);
                if (classNumber != null) student.setClassNumber(classNumber);
                userDAO.updateUser(student);
                session.setAttribute("loggedInUser", student);
                response.put("success", true);
                response.put("message", "학년/반 정보가 성공적으로 수정되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "교수는 학년/반 정보를 수정할 수 없습니다.");
            }
        }
        // 그 외의 정보 수정
        else {
            if (user instanceof UserDTO) {
                UserDTO student = (UserDTO) user;
                if (major != null) student.setMajor(major);
                userDAO.updateUser(student);
            } else if (user instanceof ProfessorDTO) {
                ProfessorDTO professor = (ProfessorDTO) user;
                if (major != null) professor.setMajor(major);
                professorDAO.updatePro(professor);
            }
            session.setAttribute("loggedInUser", user);
            response.put("success", true);
            response.put("message", "정보가 성공적으로 수정되었습니다.");
        }
        
        return response;
    }


    @PostMapping("/deleteUser")
    @ResponseBody
    public Map<String, Object> deleteUser(@RequestParam String id, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        // 로깅 추가
        System.out.println("Attempting to delete user with ID: " + id);
        
        boolean isProfessor = professorDAO.isProExists(id);
        System.out.println("Is Professor: " + isProfessor);
        
        try {
            if (isProfessor) {
                try {
                    Object professor = professorDAO.getProById(id);
                    if (professor != null) {
                        professorDAO.deletePro(id);
                        response.put("success", true);
                        response.put("message", "교수 계정이 성공적으로 삭제되었습니다.");
                    } else {
                        response.put("success", false);
                        response.put("message", "교수를 찾을 수 없습니다.");
                    }
                } catch (Exception e) {
                    System.out.println("Error while deleting professor: " + e.getMessage());
                    response.put("success", false);
                    response.put("message", "교수 계정 삭제 중 오류가 발생했습니다.");
                }
            } else {
                try {
                    Object user = userDAO.getUserById(id);
                    System.out.println("User found: " + (user != null));
                    if (user != null) {
                        userDAO.deleteUser(id);
                        response.put("success", true);
                        response.put("message", "학생 계정이 성공적으로 삭제되었습니다.");
                    } else {
                        response.put("success", false);
                        response.put("message", "학생을 찾을 수 없습니다.");
                    }
                } catch (Exception e) {
                    System.out.println("Error while deleting student: " + e.getMessage());
                    response.put("success", false);
                    response.put("message", "학생 계정 삭제 중 오류가 발생했습니다.");
                }
            }
            
            if ((Boolean) response.get("success")) {
                session.invalidate();
            }
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
            response.put("success", false);
            response.put("message", "계정 삭제 중 예기치 않은 오류가 발생했습니다.");
        }
        
        System.out.println("Delete user response: " + response);
        return response;
    }


    // 사용자가 입력한 비밀번호가 맞는지 확인하는 메서드
    @PostMapping("/checkPassword")
    @ResponseBody
    public boolean checkPassword(@RequestParam String id, @RequestParam String oldPassword) {
        // ID를 통해 사용자가 교수인지 학생인지 확인
        boolean isProfessor = professorDAO.isProExists(id);
        
        // 사용자가 교수인 경우
        if (isProfessor) {
            // 교수의 비밀번호 확인
            return professorDAO.checkProPassword(id, oldPassword);
        } else {
            // 학생의 비밀번호 확인
            Object user = userDAO.getUserById(id);
            
            // 사용자가 존재하고 비밀번호가 일치하면 true 반환, 그렇지 않으면 false 반환
            return user != null && userDAO.checkPassword(id, oldPassword);
        }
    }
}

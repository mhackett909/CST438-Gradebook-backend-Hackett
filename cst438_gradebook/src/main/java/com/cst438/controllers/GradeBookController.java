package com.cst438.controllers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cst438.domain.Assignment;
import com.cst438.domain.AssignmentListDTO;
import com.cst438.domain.AssignmentGrade;
import com.cst438.domain.AssignmentGradeRepository;
import com.cst438.domain.AssignmentRepository;
import com.cst438.domain.Course;
import com.cst438.domain.CourseDTOG;
import com.cst438.domain.CourseRepository;
import com.cst438.domain.Enrollment;
import com.cst438.domain.GradebookDTO;
import com.cst438.services.RegistrationService;

@RestController
@CrossOrigin(origins = {"http://localhost:3000","http://localhost:3001"})
public class GradeBookController {
	
	@Autowired
	AssignmentRepository assignmentRepository;
	
	@Autowired
	AssignmentGradeRepository assignmentGradeRepository;
	
	@Autowired
	CourseRepository courseRepository;
	
	@Autowired
	RegistrationService registrationService;
	
	// get assignments for an instructor that need grading
	@GetMapping("/gradebook")
	public AssignmentListDTO getAssignmentsNeedGrading( ) {
		
		String email = "dwisneski@csumb.edu";  // user name (should be instructor's email) 
		
		List<Assignment> assignments = assignmentRepository.findNeedGradingByEmail(email);
		AssignmentListDTO result = new AssignmentListDTO();
		for (Assignment a: assignments) {
			result.assignments.add(new AssignmentListDTO.AssignmentDTO(a.getId(), a.getCourse().getCourse_id(), a.getName(), a.getDueDate().toString() , a.getCourse().getTitle()));
		}
		return result;
	}
	
	@GetMapping("/gradebook/{id}")
	public GradebookDTO getGradebook(@PathVariable("id") Integer assignmentId  ) {
		
		String email = "dwisneski@csumb.edu";  // user name (should be instructor's email) 
		Assignment assignment = checkAssignment(assignmentId, email);
		
		// get the enrollment for the course
		//  for each student, get the current grade for assignment, 
		//   if the student does not have a current grade, create an empty grade
		GradebookDTO gradebook = new GradebookDTO();
		gradebook.assignmentId= assignmentId;
		gradebook.assignmentName = assignment.getName();
		for (Enrollment e : assignment.getCourse().getEnrollments()) {
			GradebookDTO.Grade grade = new GradebookDTO.Grade();
			grade.name = e.getStudentName();
			grade.email = e.getStudentEmail();
			// does student have a grade for this assignment
			AssignmentGrade ag = assignmentGradeRepository.findByAssignmentIdAndStudentEmail(assignmentId,  grade.email);
			if (ag != null) {
				grade.grade = ag.getScore();
				grade.assignmentGradeId = ag.getId();
			} else {
				grade.grade = "";
				AssignmentGrade agNew = new AssignmentGrade(assignment, e);
				agNew = assignmentGradeRepository.save(agNew);
				grade.assignmentGradeId = agNew.getId();  // key value generated by database on save.
			}
			gradebook.grades.add(grade);
		}
		return gradebook;
	}
	
	@PostMapping("/course/{course_id}/finalgrades")
	@Transactional
	public void calcFinalGrades(@PathVariable int course_id) {
		System.out.println("Gradebook - calcFinalGrades for course " + course_id);
		
		// check that this request is from the course instructor 
		String email = "dwisneski@csumb.edu";  // user name (should be instructor's email) 
		
		Course c = courseRepository.findById(course_id).orElse(null);
		if (!c.getInstructor().equals(email)) {
			throw new ResponseStatusException( HttpStatus.UNAUTHORIZED, "Not Authorized. " );
		}
		
		CourseDTOG cdto = new CourseDTOG();
		cdto.course_id = course_id;
		cdto.grades = new ArrayList<>();
		for (Enrollment e: c.getEnrollments()) {
			double total=0.0;
			int count = 0;
			for (AssignmentGrade ag : e.getAssignmentGrades()) {
				count++;
				total = total + Double.parseDouble(ag.getScore());
			}
			double average = total/count;
			CourseDTOG.GradeDTO gdto = new CourseDTOG.GradeDTO();
			gdto.grade=letterGrade(average);
			gdto.student_email=e.getStudentEmail();
			gdto.student_name=e.getStudentName();
			cdto.grades.add(gdto);
			System.out.println("Course="+course_id+" Student="+e.getStudentEmail()+" grade="+gdto.grade);
		}
		
		registrationService.sendFinalGrades(course_id, cdto);
	}
	
	private String letterGrade(double grade) {
		if (grade >= 90) return "A";
		if (grade >= 80) return "B";
		if (grade >= 70) return "C";
		if (grade >= 60) return "D";
		return "F";
	}
	
	@PutMapping("/gradebook/{id}")
	@Transactional
	public void updateGradebook (@RequestBody GradebookDTO gradebook, @PathVariable("id") Integer assignmentId ) {
		
		String email = "dwisneski@csumb.edu";  // user name (should be instructor's email) 
		checkAssignment(assignmentId, email);  // check that user name matches instructor email of the course.
		
		// for each grade in gradebook, update the assignment grade in database 
		System.out.printf("%d %s %d\n",  gradebook.assignmentId, gradebook.assignmentName, gradebook.grades.size());
		
		for (GradebookDTO.Grade g : gradebook.grades) {
			System.out.printf("%s\n", g.toString());
			AssignmentGrade ag = assignmentGradeRepository.findById(g.assignmentGradeId).orElse(null);
			if (ag == null) {
				throw new ResponseStatusException( HttpStatus.BAD_REQUEST, "Invalid grade primary key. "+g.assignmentGradeId);
			}
			ag.setScore(g.grade);
			System.out.printf("%s\n", ag.toString());
			
			assignmentGradeRepository.save(ag);
		}
		
	}

	@PostMapping("/gradebook/add")
	public void addAssignment(@RequestBody AssignmentListDTO.AssignmentDTO assignment) throws ParseException {
		String email = "dwisneski@csumb.edu";  // user name (should be instructor's email) 
		
		Course c = courseRepository.findById(assignment.courseId).orElse(null);
		if (c == null) {
			throw new ResponseStatusException( HttpStatus.BAD_REQUEST, "Invalid course primary key. "+assignment.courseId);
		}
		if (!c.getInstructor().equals(email)) {
			throw new ResponseStatusException( HttpStatus.UNAUTHORIZED, "Not Authorized. " );
		}
		if (assignment.assignmentName == null || assignment.assignmentName.isBlank()) {
			throw new ResponseStatusException( HttpStatus.BAD_REQUEST, "Invalid assignment name.");
		}
		if (assignment.dueDate == null || assignment.dueDate.isBlank()) {
			throw new ResponseStatusException( HttpStatus.BAD_REQUEST, "Invalid date format.");
		}
		

		SimpleDateFormat obj = new SimpleDateFormat("dd-MM-yyyy"); 
		long date = obj.parse(assignment.dueDate).getTime();
		
		Assignment a = new Assignment();
		a.setCourse(c);
		a.setNeedsGrading(0);
		a.setName(assignment.assignmentName);
		a.setDueDate(new java.sql.Date(date));

		assignmentRepository.save(a);
	}
	
	@PostMapping("/gradebook/edit")
	public void editAssignmentName(@RequestBody	AssignmentListDTO.AssignmentDTO assignment) {
		String email = "dwisneski@csumb.edu";  // user name (should be instructor's email) 
		
		if (assignment.assignmentName == null || assignment.assignmentName.isBlank()) {
			throw new ResponseStatusException( HttpStatus.BAD_REQUEST, "Invalid assignment name.");
		}
		
		Assignment old = assignmentRepository.findById(assignment.assignmentId).orElse(null);
		
		if (old == null) {
			throw new ResponseStatusException( HttpStatus.BAD_REQUEST, "Invalid assignment primary key. "+assignment.assignmentId);
		}
		
		Course c = old.getCourse();
		if (!c.getInstructor().equals(email)) {
			throw new ResponseStatusException( HttpStatus.UNAUTHORIZED, "Not Authorized. " );
		}
		
		old.setName(assignment.assignmentName);
		assignmentRepository.save(old);
	}
	
	@PutMapping("/gradebook/delete")
	public void deleteAssignment(@RequestBody AssignmentListDTO.AssignmentDTO assignment) {
		String email = "dwisneski@csumb.edu";  // user name (should be instructor's email) 
		
		Assignment a = assignmentRepository.findById(assignment.assignmentId).orElse(null);
		int a_id = assignment.assignmentId;
		
		if (a == null) {
			throw new ResponseStatusException( HttpStatus.BAD_REQUEST, "Invalid assignment primary key. "+a_id);
		}
		
		Course c = a.getCourse();
		if (!c.getInstructor().equals(email)) {
			throw new ResponseStatusException( HttpStatus.UNAUTHORIZED, "Not Authorized. " );
		}
		
		List<AssignmentGrade> grades = assignmentGradeRepository.findByAssignmentId(assignment.assignmentId);

		// Make sure no grades for assignment
		for (AssignmentGrade g : grades) {
			if (!g.getScore().isEmpty()) {
				throw new ResponseStatusException( HttpStatus.BAD_REQUEST, "Cannot delete. Grades exist for assignment "+a_id);
			}
		}
		
		for (AssignmentGrade g :grades) {
			assignmentGradeRepository.delete(g);
		}

		assignmentRepository.deleteById(a_id);
		
	}
	
	private Assignment checkAssignment(int assignmentId, String email) {
		// get assignment 
		Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);
		if (assignment == null) {
			throw new ResponseStatusException( HttpStatus.BAD_REQUEST, "Assignment not found. "+assignmentId );
		}
		// check that user is the course instructor
		if (!assignment.getCourse().getInstructor().equals(email)) {
			throw new ResponseStatusException( HttpStatus.UNAUTHORIZED, "Not Authorized. " );
		}
		
		return assignment;
	}

}

package com.cst438;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.cst438.domain.Assignment;
import com.cst438.domain.AssignmentGrade;
import com.cst438.domain.AssignmentGradeRepository;
import com.cst438.domain.AssignmentRepository;
import com.cst438.domain.Course;
import com.cst438.domain.CourseRepository;
import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;

@SpringBootTest
public class EndToEndTestAddAssignment {
	public static final String EDGE_DRIVER_FILE_LOCATION = "C:\\Users\\Michael\\Dropbox\\Backup\\Michael\\Shared\\Documents\\CSUMB\\CST438 Software Engineering\\Week 5\\msedgedriver.exe";

	public static final String URL = "http://localhost:3000";
	public static final String TEST_USER_EMAIL = "test@csumb.edu";
	public static final String TEST_INSTRUCTOR_EMAIL = "dwisneski@csumb.edu";
	public static final int SLEEP_DURATION = 1000; // 1 second.
	public static final String TEST_ASSIGNMENT_NAME = "Test Assignment";
	public static final String TEST_ASSIGNMENT_DATE = "10/13/2021";
	public static final String TEST_COURSE_TITLE = "Test Course";
	public static final String TEST_STUDENT_NAME = "Test";

	@Autowired
	CourseRepository courseRepository;

	@Autowired
	AssignmentRepository assignmentRepository;

	@Test
	public void addAssignmentTest() throws Exception {

//		Database setup:  create test course		
		Course c = new Course();
		c.setCourse_id(99999);
		c.setInstructor(TEST_INSTRUCTOR_EMAIL);
		c.setSemester("Fall");
		c.setYear(2021);
		c.setTitle(TEST_COURSE_TITLE);
		
		courseRepository.save(c);

		System.setProperty("webdriver.edge.driver", EDGE_DRIVER_FILE_LOCATION);
		WebDriver driver = new EdgeDriver();
		// Puts an Implicit wait for 10 seconds before throwing exception
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

		driver.get(URL);
		Thread.sleep(SLEEP_DURATION);
		try {			
			// locate and click new assignment button
			driver.findElement(By.xpath("//a[@id='new_assignment']")).click();
			Thread.sleep(SLEEP_DURATION);


			//Select course from drop down
			Select dropdown = new Select(driver.findElement(By.id("select_course")));
			dropdown.selectByVisibleText(TEST_COURSE_TITLE);
			Thread.sleep(SLEEP_DURATION);
			
			//Enter assignment name
			WebElement element = driver.findElement(By.xpath("//input[@id='assignment_name']"));
			element.sendKeys(TEST_ASSIGNMENT_NAME);
			Thread.sleep(SLEEP_DURATION);
			
			//Enter assignment date
			element = driver.findElement(By.xpath("//input[@id='assignment_date']"));
			element.sendKeys(TEST_ASSIGNMENT_DATE);
			Thread.sleep(SLEEP_DURATION);
			
			// Locate submit button and click
			driver.findElement(By.xpath("//button[@id='Submit' and @name='submit_assignment']")).click();
			Thread.sleep(SLEEP_DURATION);
			
			// Go back to previous page
			driver.navigate().back();
			Thread.sleep(SLEEP_DURATION);

			// Verify new assignment is added for the test course
			List<WebElement> elements  = driver.findElements(By.xpath("//div[@data-field='assignmentName']/div"));
			boolean found = false;
			for (WebElement we : elements) {
				System.out.println(we.getText()); // for debug
				if (we.getText().equals(TEST_ASSIGNMENT_NAME)) {
					found=true;
					we.findElement(By.xpath("descendant::input")).click();
					break;
				}
			}
			assertTrue( found, "Unable to locate TEST ASSIGNMENT in list of assignments to be graded.");

		} catch (Exception ex) {
			throw ex;
		} finally {
			// clean up database so the test is repeatable.
			List<Assignment> assignments = assignmentRepository.findNeedGradingByEmail(TEST_INSTRUCTOR_EMAIL);
			for (Assignment a : assignments) {
				if (a.getName().equals(TEST_ASSIGNMENT_NAME)) {
					assignmentRepository.delete(a);
					break;
				}
			}
			courseRepository.delete(c);

			driver.quit();
		}

	}
}

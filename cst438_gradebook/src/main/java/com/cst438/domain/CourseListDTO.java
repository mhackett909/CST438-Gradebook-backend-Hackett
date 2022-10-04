package com.cst438.domain;

import java.util.ArrayList;

/*
 * a transfer object that is a list of assignment details
 */
public class CourseListDTO {

	public static class CourseDTO {
		public int course_id;
		public String title;
		public String instructor;
		public int year;
		public String semester;
		
		public CourseDTO() { }

		public CourseDTO(int course_id, String title, String instructor, int year, String semester) {
			this.course_id = course_id;
			this.title = title;
			this.instructor = instructor;
			this.year = year;
			this.semester = semester;
		}

		@Override
		public String toString() {
			return "[course_id=" + course_id + ", title=" + title + ", instructor="
					+ instructor + ", year=" + year + ", semester=" + semester + "]";
		}


		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CourseDTO other = (CourseDTO) obj;
			if (course_id != other.course_id)
				return false;
			if (title == null) {
				if (other.title != null)
					return false;
			} else if (!title.equals(other.title))
				return false;
			if (instructor == null) {
				if (other.instructor != null)
					return false;
			} else if (!instructor.equals(other.instructor))
				return false;
			if (year != other.year) 
					return false;
			if (semester == null) {
				if (other.semester != null)
					return false;
			} else if (!semester.equals(other.semester))
				return false;
			return true;
		}
	}

	public ArrayList<CourseDTO> courses = new ArrayList<>();

	@Override
	public String toString() {
		return "CourseListDTO " + courses;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CourseListDTO other = (CourseListDTO) obj;
		if (courses == null) {
			if (other.courses != null)
				return false;
		} else if (!courses.equals(other.courses))
			return false;
		return true;
	}
	
}

//1. Create the Course class
public class Course {
    private String courseName;
    private int duration; 

    public Course(String courseName, int duration) {
        this.courseName = courseName;
        this.duration = duration;
    }

    public String getCourseName() {
        return courseName;
    }

    public int getDuration() {
        return duration;
    }
}

//2. Create the Student class
public class Student {
    private String name;
    private Course course;

    public Student(String name, Course course) {
        this.name = name;
        this.course = course;
    }

    public void printDetails() {
        System.out.println("Student Name: " + name);
        System.out.println("Enrolled Course: " + course.getCourseName());
        System.out.println("Course Duration: " + course.getDuration() + " weeks");
    }
}

//3. Create Java-based Spring Configuration using @Configuration and @Bean
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public Course course() {
        return new Course("Java Spring Boot", 6);
    }

    @Bean
    public Student student() {
        return new Student("Rahul", course());
    }
}

//4. Main class to load Spring context and run the app
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class MainApp {
    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        Student student = context.getBean(Student.class);
        student.printDetails();
    }
}

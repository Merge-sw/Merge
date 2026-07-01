package com.merge.backend.identity.security;

import com.merge.backend.identity.repository.StudentRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentUserDetailsService implements UserDetailsService {

    private final StudentRepository studentRepository;

    public StudentUserDetailsService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return studentRepository.findByEmail(email)
                .map(student -> new User(
                        student.getEmail(),
                        student.getPasswordHash(),
                        List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
                ))
                .orElseThrow(() -> new UsernameNotFoundException("Student not found: " + email));
    }
}

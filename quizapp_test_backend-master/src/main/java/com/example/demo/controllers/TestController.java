package com.example.demo.controllers;

import com.example.demo.model.*;
import com.example.demo.repositories.*;
import com.example.demo.services.EmailService;
import com.example.demo.services.QuestionService;
import com.example.demo.services.TestService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

@RestController
@RequestMapping("/api")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private AdministrateurRepository administrateurRepository;

    @Autowired
    private ThemeRepository domaineRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private NiveauRepository levelRepository;

    @Autowired
    private CompetencyRepository competencyRepository;

    @Autowired
    private CandidatRepository candidatsRepository;

    @Autowired
    private EmailService emailService;
    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ReponseRepository answerRepository;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private TestService testService;

    @Autowired
    private ScoreRepository scoreRepository;

    @GetMapping("/tests")
    public ResponseEntity<List<Test>> getAllTests() {
        List<Test> tests = testService.getAllTests();
        return ResponseEntity.ok(tests);
    }

    @GetMapping("/tests/{id}")
    public ResponseEntity<Test> getTestById(@PathVariable Long id) {
        Optional<Test> test = testService.getTestById(id);
        return test.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/tests/{id}")
    public ResponseEntity<Void> deleteTestById(@PathVariable Long id) {
        logger.info("Attempting to delete test with id: {}", id);
        try {
            testService.deleteTestById(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error occurred while deleting test with id " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/tests/{id}/results")
    public ResponseEntity<List<CandidateResult>> getCandidateByTestId(@PathVariable Long id) {
        List<CandidateResult> results = new ArrayList<>();

        // Récupérer tous les candidats inscrits au test
        Optional<Test> testOpt = testRepository.findById(id);
        if (testOpt.isEmpty()) {
            logger.error("Test with id " + id + " not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(results); // Test non trouvé
        }

        Test test = testOpt.get();
        List<Condidats> candidates = test.getCandidates();

        logger.info("Number of candidates found: " + candidates.size());

        for (Condidats candidate : candidates) {
            logger.info("Processing candidate with ID: " + candidate.getId() + ", Name: " + candidate.getName());

            // Récupérer le score pour chaque candidat pour ce test
            Optional<Score> scoreOpt = scoreRepository.findByTestIdAndCandidatId(test.getId(), candidate.getId());

            int scorePercentage;
            if (scoreOpt.isPresent()) {
                scorePercentage = scoreOpt.get().getScorePercentage();
                logger.info("Score for candidate " + candidate.getName() + ": " + scorePercentage);
            } else {
                scorePercentage = -1; // Utiliser -1 ou une autre valeur spéciale pour indiquer que le candidat n'a
                                      // pas encore de score
                logger.warn("No score found for candidate " + candidate.getName());
            }

            // Ajouter un nouvel objet CandidateResult à la liste
            CandidateResult result = new CandidateResult(candidate, scorePercentage);
            results.add(result);
        }

        logger.info("Total results prepared: " + results.size());

        return ResponseEntity.ok(results);
    }

    public static class CandidateResult {
        private Condidats candidate;
        private int scorePercentage;

        public CandidateResult(Condidats candidate, int scorePercentage) {
            this.candidate = candidate;
            this.scorePercentage = scorePercentage;
        }

        public Condidats getCandidate() {
            return candidate;
        }

        public int getScorePercentage() {
            return scorePercentage;
        }

        public String getFormattedScore() {
            if (scorePercentage == -1) {
                return "N/A"; // Afficher "N/A" si le score est absent
            } else {
                return scorePercentage + "%"; // Afficher le pourcentage s'il est présent
            }
        }
    }

    @GetMapping("/tests/{id}/candidates")
    public ResponseEntity<List<Condidats>> getCandidatesByTestId(@PathVariable Long id) {
        List<Condidats> candidates = testService.getCandidatesByTestId(id);

        // No filtering based on scores, return all candidates
        return candidates.isEmpty() ? ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                : ResponseEntity.ok(candidates);
    }

    @GetMapping("/tests/{id}/competencies")
    public ResponseEntity<List<Competency>> getCompetenciesByTestId(@PathVariable Long testId) {
        List<Competency> competencies = testService.getCompetenciesByTestId(testId);
        return ResponseEntity.ok(competencies);
    }

    @GetMapping("/tests/{id}/questions")
    public ResponseEntity<List<Question>> getQuestionsByTestId(@PathVariable Long id) {
        List<Question> questions = testService.getQuestionsByTestId(id);
        return questions != null ? ResponseEntity.ok(questions) : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @PostMapping("/tests/{testId}/sendToCandidate")
    public ResponseEntity<String> sendTestToCandidate(
            @PathVariable Long testId, @RequestBody SendTestRequest request) {
        try {
            // Vérifier l'existence du test
            Optional<Test> testOpt = testService.getTestById(testId);
            if (!testOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Test not found.");
            }
            Test test = testOpt.get();

            // Vérifier si le candidat existe, sinon l'ajouter
            Optional<Condidats> candidateOpt = candidatsRepository.findByEmail(request.getCandidateEmail());
            Condidats candidate;
            if (candidateOpt.isPresent()) {
                candidate = candidateOpt.get();
            } else {
                // Créer un nouveau candidat si nécessaire
                candidate = new Condidats();
                candidate.setEmail(request.getCandidateEmail());
                candidate.setName(request.getCandidateName()); // Ajouter le nom du candidat
                candidatsRepository.save(candidate);
            }

            // Créer le lien de test
            String testLink = "http://localhost:3000/TakeTest/" + test.getId() + "?email=" + candidate.getEmail();

            // Envoyer l'email
            String emailBody = "You are invited to take the test: " + test.getName() + "\n\n"
                    + "Please click on the following link to take the test:\n" + testLink;
            emailService.sendEmail(candidate.getEmail(), "Test Invitation", emailBody);

            // Ajouter le candidat au test
            List<Condidats> candidates = test.getCandidates();
            if (candidates == null) {
                candidates = new ArrayList<>();
            }
            if (!candidates.contains(candidate)) {
                candidates.add(candidate);
                test.setCandidates(candidates);
                testRepository.save(test);
            }

            return ResponseEntity.ok("Email sent successfully to " + candidate.getEmail());
        } catch (Exception e) {
            logger.error("Error sending test to candidate", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send email.");
        }
    }

    @PostMapping("/createAndSendTest")
    public ResponseEntity<String> createAndSendTest(@RequestBody TestRequest testRequest) {
        try {
            logger.info("Received test request: {}", testRequest);

            // Vérification si un test avec le même nom existe déjà
            if (testService.doesTestExistByName(testRequest.getTestName())) {
                logger.error("Test with the name '{}' already exists.", testRequest.getTestName());
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Un test avec ce nom existe déjà.");
            }

            // Retrieve and verify related entities
            Optional<Administrateur> administratorOpt = administrateurRepository
                    .findByEmail(testRequest.getAdminEmail());
            if (!administratorOpt.isPresent()) {
                logger.error("Administrator not found with email: {}", testRequest.getAdminEmail());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Administrator not found.");
            }
            Administrateur administrator = administratorOpt.get();

            Optional<Theme> domaineOpt = domaineRepository.findById(testRequest.getDomaineId());
            if (!domaineOpt.isPresent()) {
                logger.error("Domain not found with ID: {}", testRequest.getDomaineId());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Domain not found.");
            }
            Theme domaine = domaineOpt.get();

            Optional<Role> roleOpt = roleRepository.findById(testRequest.getRoleId());
            if (!roleOpt.isPresent()) {
                logger.error("Role not found with ID: {}", testRequest.getRoleId());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Role not found.");
            }
            Role role = roleOpt.get();

            Optional<Level> levelOpt = levelRepository.findById(testRequest.getLevelId());
            if (!levelOpt.isPresent()) {
                logger.error("Level not found with ID: {}", testRequest.getLevelId());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Level not found.");
            }
            Level level = levelOpt.get();

            List<Competency> competencies = competencyRepository.findAllById(testRequest.getCompetencyIds());
            if (competencies.isEmpty()) {
                logger.error("No competencies found for IDs: {}", testRequest.getCompetencyIds());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No competencies found.");
            }

            List<Condidats> candidates = testRequest.getCandidates();
            if (candidates == null || candidates.isEmpty()) {
                logger.error("No candidates provided in the request.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No candidates provided.");
            }

            // Enregistrer les candidats
            for (Condidats candidate : candidates) {
                if (candidate.getId() == null) {
                    candidatsRepository.save(candidate);
                }
            }

            List<Question> questions = questionService.findQuestionsByCompetencyIds(testRequest.getCompetencyIds());

            Test test = new Test();
            test.setName(testRequest.getTestName());
            test.setAdministrator(administrator);
            test.setDomaine(domaine);
            test.setRole(role);
            test.setLevel(level);
            test.setCompetencies(competencies);
            test.setCandidates(candidates);

            // Set the test reference in each question
            for (Question question : questions) {
                question.setTest(test);
            }

            test.setQuestions(questions);

            logger.info("Saving test: {}", test);
            testRepository.save(test);

            String testLink = "http://localhost:3000/TakeTest/" + test.getId();

            // Envoi d'email aux candidats
            for (Condidats candidate : candidates) {
                if (candidate.getEmail() != null && !candidate.getEmail().isEmpty()) {
                    logger.info("Sending email to: {}", candidate.getEmail());
                    String emailBody = "You are invited to take the test: " + testRequest.getTestName() + "\n\n"
                            + "Please click on the following link to take the test:\n" + testLink + "?email="
                            + candidate.getEmail();
                    emailService.sendEmail(candidate.getEmail(), "Test Invitation", emailBody);
                }
            }

            logger.info("Test created and emails sent successfully.");
            return ResponseEntity.ok("Emails sent successfully!");
        } catch (Exception e) {
            logger.error("Error creating test and sending emails", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send emails.");
        }
    }

    public static class SendTestRequest {
        private String candidateEmail;
        private String candidateName;
        private Long testId;

        // Getter pour candidateEmail
        public String getCandidateEmail() {
            return candidateEmail;
        }

        // Setter pour candidateEmail
        public void setCandidateEmail(String candidateEmail) {
            this.candidateEmail = candidateEmail;
        }

        public String getCandidateName() {
            return candidateName;
        }

        public void setCandidateName(String candidateName) {
            this.candidateName = candidateName;
        }

        // Setter pour testId
        public void setTestId(Long testId) {
            this.testId = testId;
        }
    }

    public static class TestRequest {
        private String testName;
        private String adminEmail;
        private Long domaineId;
        private Long roleId;
        private Long levelId;
        private List<Long> competencyIds;
        private List<Condidats> candidates;

        // Getters and setters
        public String getTestName() {
            return testName;
        }

        public void setTestName(String testName) {
            this.testName = testName;
        }

        public String getAdminEmail() {
            return adminEmail;
        }

        public void setAdminEmail(String adminEmail) {
            this.adminEmail = adminEmail;
        }

        public Long getDomaineId() {
            return domaineId;
        }

        public void setDomaineId(Long domaineId) {
            this.domaineId = domaineId;
        }

        public Long getRoleId() {
            return roleId;
        }

        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }

        public Long getLevelId() {
            return levelId;
        }

        public void setLevelId(Long levelId) {
            this.levelId = levelId;
        }

        public List<Long> getCompetencyIds() {
            return competencyIds;
        }

        public void setCompetencyIds(List<Long> competencyIds) {
            this.competencyIds = competencyIds;
        }

        public List<Condidats> getCandidates() {
            return candidates;
        }

        public void setCandidates(List<Condidats> candidates) {
            this.candidates = candidates;
        }
    }

    @PostMapping("/tests/{id}/submit")
    public ResponseEntity<String> submitTest(@PathVariable Long id, @RequestParam String email,
            @RequestBody List<AnswerRequest> answerRequests) {
        logger.info("Received request with Test ID: {}, Email: {}, Answers: {}", id, email, answerRequests);

        Optional<Test> testOpt = testService.getTestById(id);
        if (!testOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Test not found.");
        }

        Test test = testOpt.get();

        Optional<Condidats> candidatOpt = candidatsRepository.findByEmail(email);
        if (!candidatOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Candidate not found.");
        }

        Condidats candidat = candidatOpt.get();
        int correctAnswers = 0;
        int totalQuestions = test.getQuestions().size();

        for (AnswerRequest answerRequest : answerRequests) {
            Optional<Question> questionOpt = questionRepository.findById(answerRequest.getQuestionId());
            if (questionOpt.isPresent()) {
                Question question = questionOpt.get();
                boolean isCorrect = answerRequest.isEstCorrecte();

                logger.info("Processing answer: Question ID = {}, Answer Text = {}, IsCorrect = {}",
                        question.getId(), answerRequest.getTexteReponse(), isCorrect);

                if (isCorrect) {
                    correctAnswers++;
                }

                Answer answer = new Answer();
                answer.setTexteReponse(answerRequest.getTexteReponse());
                answer.setEstCorrecte(isCorrect);
                answer.setCandidat(candidat);
                answer.setQuestion(question);
                answerRepository.save(answer);

                logger.info("Saved answer: Question ID = {}, Candidat ID = {}, EstCorrecte = {}",
                        question.getId(), candidat.getId(), isCorrect);
            }
        }

        // Creating and saving the score
        Score score = new Score();
        score.setCandidat(candidat);
        score.setTest(test);
        score.setCorrectAnswers(correctAnswers);
        score.setTotalQuestions(totalQuestions);

        scoreRepository.save(score);

        return ResponseEntity.ok("Test submitted successfully!");
    }

    public static class AnswerRequest {
        private Long questionId;
        private Long candidatId;
        private String texteReponse;
        private boolean estCorrecte;

        public Long getQuestionId() {
            return questionId;
        }

        public void setQuestionId(Long questionId) {
            this.questionId = questionId;
        }

        public Long getCandidatId() {
            return candidatId;
        }

        public void setCandidatId(Long candidatId) {
            this.candidatId = candidatId;
        }

        public String getTexteReponse() {
            return texteReponse;
        }

        public void setTexteReponse(String texteReponse) {
            this.texteReponse = texteReponse;
        }

        public boolean isEstCorrecte() {
            return estCorrecte;
        }

        public void setEstCorrecte(boolean estCorrecte) {
            this.estCorrecte = estCorrecte;
        }
    }
}

package com.example.demo.model;

import jakarta.persistence.*;

@Entity
public class Answer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "texte_reponse")
    private String texteReponse;

    @Column(name = "est_correcte")
    private boolean estCorrecte;

    @ManyToOne
    @JoinColumn(name = "candidat_id")
    private Condidats candidat;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Condidats getCandidat() {
        return candidat;
    }

    public void setCandidat(Condidats candidat) {
        this.candidat = candidat;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }
}

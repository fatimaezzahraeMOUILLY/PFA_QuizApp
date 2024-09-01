import React, { useState, useEffect } from 'react';
import Navbar from '../components/Navbar';
import axios from 'axios';
import Swal from 'sweetalert2';
import {
  Typography,
  List,
  ListItem,
  ListItemText,
  Button,
  Box,
  Modal,
  TextField,
} from '@mui/material';
import SettingsIcon from '@mui/icons-material/Settings';
import PersonIcon from '@mui/icons-material/Person';
import AddIcon from '@mui/icons-material/Add';
import SearchIcon from '@mui/icons-material/Search';
import InputAdornment from '@mui/material/InputAdornment';


function TestList() {
  const [tests, setTests] = useState([]);
  const [selectedTest, setSelectedTest] = useState(null);
  const [isParamsModalOpen, setIsParamsModalOpen] = useState(false);
  const [isCandidatesModalOpen, setIsCandidatesModalOpen] = useState(false);
  const [isInviteModalOpen, setIsInviteModalOpen] = useState(false);
  const [testParams, setTestParams] = useState({});
  const [candidates, setCandidates] = useState([]);
  const [questions, setQuestions] = useState([]);
  const [candidateEmail, setCandidateEmail] = useState('');
  const [candidateName, setCandidateName] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [error, setError] = useState(null);


  useEffect(() => {
    const fetchTests = async () => {
      try {
        const response = await fetch('http://localhost:8088/api/tests');
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.json();
        console.log('Fetched tests:', data); // Log the fetched tests
        setTests(data);
      } catch (error) {
        console.error('Error fetching tests:', error);
      }
    };
    fetchTests();
  }, []);

  const handleSearchChange = (event) => {
    setSearchQuery(event.target.value);
  };

  const filteredCandidates = candidates.filter(candidate =>
    candidate.name.toLowerCase().includes(searchQuery.toLowerCase())
  );
  
  

  const handleTestClick = async (test) => {
    setSelectedTest(test);
    try {
      const response = await fetch(`http://localhost:8088/api/tests/${test.id}`);
      const data = await response.json();
      console.log('Fetched test params:', data); // Log the fetched test params
      setTestParams(data);
    } catch (error) {
      console.error('Error fetching test params:', error);
    }

    try {
      const response = await fetch(`http://localhost:8088/api/tests/${test.id}/questions`);
      const data = await response.json();
      console.log('Fetched test questions:', data); // Log the fetched test questions
      setQuestions(data);
    } catch (error) {
      console.error('Error fetching test questions:', error);
    }
  };

  const handleParamsModalOpen = async () => {
    setIsParamsModalOpen(true);
    try {
      const response = await fetch(`http://localhost:8088/api/tests/${selectedTest.id}`);
      const data = await response.json();
      setTestParams(data);
    } catch (error) {
      console.error('Error fetching test params:', error);
    }
  };

  const handleParamsModalClose = () => {
    setIsParamsModalOpen(false);
  };

  const handleCandidatesModalOpen = async () => {
    setIsCandidatesModalOpen(true);
    try {
      const response = await fetch(`http://localhost:8088/api/tests/${selectedTest.id}/candidates`);
      const data = await response.json();
      setCandidates(data);
    } catch (error) {
      console.error('Error fetching candidates:', error);
    }
  };

  const handleCandidatesModalClose = () => {
    setIsCandidatesModalOpen(false);
  };

  const handleInviteModalOpen = () => {
    setIsInviteModalOpen(true);
  };

  const handleInviteModalClose = () => {
    setIsInviteModalOpen(false);
  };

  // Pour capturer l'email saisi par l'utilisateur
const handleEmailChange = (event) => {
  setCandidateEmail(event.target.value);
};
const handleNameChange = (event) => {
  setCandidateName(event.target.value);
};

// Pour envoyer le test au candidat
const handleSendTestToCandidate = async () => {
  if (!candidateEmail || !candidateName || !selectedTest) {
    alert('Veuillez saisir un nom, un email, et sélectionner un test.');
    return;
  }

  const requestBody = {
    candidateEmail: candidateEmail,
    candidateName: candidateName, 
  };

  try {
    const response = await axios.post(`http://localhost:8088/api/tests/${selectedTest.id}/sendToCandidate`, requestBody);
    console.log('Response:', response.data);
    setIsInviteModalOpen(false);
    Swal.fire({
      title: 'Succès',
      text: response.data.message || 'Test envoyé avec succès!',
      icon: 'success',
      confirmButtonText: 'OK',
      customClass: {
        confirmButton: 'custom-confirm-button' 
    },
    didOpen: () => {
        const confirmButton = Swal.getConfirmButton();
        confirmButton.style.backgroundColor = '#232A56'; 
    }
  });
  } catch (error) {
    console.error('Erreur lors de l\'envoi du test:', error);
    setIsInviteModalOpen(false);
    Swal.fire({
      title: 'Erreur',
      text: error.response?.data || 'Échec de l\'envoi du test' ,
      icon: 'error',
      confirmButtonText: 'OK',
      customClass: {
        confirmButton: 'custom-confirm-button' 
    },
    didOpen: () => {
        const confirmButton = Swal.getConfirmButton();
        confirmButton.style.backgroundColor = '#232A56'; 
    }
  });
  }
};




  return (
    <div style={{ display: 'flex', minHeight: '100vh' }}>
      <Navbar/>
      <div style={{ display: 'flex', width: '100%', marginTop: '64px', backgroundColor: '#D9D9D9' }}>
        <Box sx={{ width: '20%', padding: 2, backgroundColor: '#232A56', color: '#fff' }}>
          <Typography variant="h6"sx={{ textDecoration: 'underline', marginBottom: '5%', fontSize: '2em' }}>Tests</Typography>
          {error && <p>{error}</p>}
          <List>
            {tests.map(test => (
              <ListItem
                button
                key={test.id}
                onClick={() => handleTestClick(test)}
                sx={{
                  backgroundColor: selectedTest && selectedTest.id === test.id ? 'rgba(255, 255, 255, 0.2)' : 'transparent',
                  borderRadius: '8px',
                  '&:before': {
                    content: selectedTest && selectedTest.id === test.id ? '"▶"' : '""',
                    marginRight: '8px',
                  },
                }}
              >
                <ListItemText primary={test.name} />
              </ListItem>
            ))}
          </List>
        </Box>
        <Box sx={{ width: '80%', padding: '20px', alignItems: 'center', justifyContent: 'center' }}>
          {selectedTest ? (
            <>
              <Typography variant="h5" style={{ textAlign: 'center', padding: '20px 0' , fontSize: '2em'}}>{selectedTest.name}</Typography>
              <div style={{ textAlign: 'right', margin: '2%' }}>
                <Button
                  type="button"
                  variant="contained"
                  color="primary"
                  style={{ borderRadius: 30, width: '5%', backgroundColor: '#232A56', color: '#fff', cursor: 'pointer', margin: '1%' }}
                  onClick={handleParamsModalOpen}
                >
                  <SettingsIcon />
                </Button>
                <Button
                  type="button"
                  variant="contained"
                  color="primary"
                  style={{ borderRadius: 30, width: '5%', backgroundColor: '#232A56', color: '#fff', cursor: 'pointer', margin: '1%' }}
                  onClick={handleCandidatesModalOpen}
                >
                  <PersonIcon />
                </Button>
                <Button
                  type="button"
                  variant="contained"
                  color="primary"
                  style={{ borderRadius: 30, width: '5%', backgroundColor: '#232A56', color: '#fff', cursor: 'pointer', margin: '1%' }}
                  onClick={handleInviteModalOpen}
                >
                  <AddIcon />
                </Button>
              </div>
              <div>
  <Typography variant="h6" style={{ fontSize: '27px', color: 'rgba(35, 42, 86, 0.66)', textAlign: 'center', paddingBottom: '3%' }}>Questions</Typography>
  <ul>
    {questions.map((question, index) => (
      <Box key={index} 
        sx={{ 
          border: '2px solid #555', 
          borderRadius: '8px', 
          padding: '16px', 
          marginBottom: '10px', 
          backgroundColor: '#f8f8f8'
        }}>
        <Typography variant="body1" style={{ marginBottom: '10px', fontWeight: 'bold', color: '#333' }}>
          {question.questionText}
        </Typography>
        <ul style={{ paddingLeft: '20px' }}>
          {question.choices.map((choice, idx) => (
            <li key={idx}>
              <Typography variant="body1" style={{ color: '#555' }}>
                {choice.choiceText}
              </Typography>
            </li>
          ))}
        </ul>
      </Box>
    ))}
  </ul>
</div>


            </>
          ) : (
            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', marginTop: '64px', color: '#888888' }}>
              <Typography variant="h4">Aucun test sélectionné</Typography>
            </div>
          )}
        </Box>
      </div>
      <Modal open={isParamsModalOpen} onClose={handleParamsModalClose}>
    <Box sx={{
      position: 'absolute',
      top: '50%',
      left: '50%',
      transform: 'translate(-50%, -50%)',
      width: 500,
      bgcolor: 'background.paper',
      boxShadow: 24,
      p: 4,
      borderRadius: 2,
    }}>
      <Typography variant="h6" style={{ fontSize: '1.5em', color: 'rgba(35, 42, 86, 0.66)', textAlign: 'center', paddingBottom: '5%' }}>Paramètres du test</Typography>
      {testParams ? (
        <Box sx={{ mt: 2 }}>
          <Typography variant="body1"><strong>Nom du test:</strong> {testParams.name}</Typography>
          <Typography variant="body1"><strong>Domaine:</strong> {testParams.domaine ? testParams.domaine.name : 'N/A'}</Typography>
          <Typography variant="body1"><strong>Niveau:</strong> {testParams.level ? testParams.level.name : 'N/A'}</Typography>
          <Typography variant="body1"><strong>Rôle:</strong> {testParams.role ? testParams.role.name : 'N/A'}</Typography>
          <Typography variant="body1"><strong>Email de l'admin:</strong> {testParams.administrator ? testParams.administrator.email : 'N/A'}</Typography>
        </Box>
      ) : (
        <Typography variant="body1">Aucun paramètre disponible.</Typography>
      )}
      
    </Box>
  </Modal>



  <Modal open={isCandidatesModalOpen} onClose={handleCandidatesModalClose}>
  <Box sx={{
    position: 'absolute',
    top: '50%',
    left: '50%',
    transform: 'translate(-50%, -50%)',
    width: 500,
    bgcolor: 'background.paper',
    boxShadow: 24,
    p: 4,
    borderRadius: 2,
    maxHeight: '70vh',
    overflowY: 'auto',
  }}>
    <Typography variant="h6" style={{ fontSize: '1.5em', color: 'rgba(35, 42, 86, 0.66)', textAlign: 'center', paddingBottom: '5%' }}>
      Candidats
    </Typography>
    <TextField
      fullWidth
      margin="normal"
      label="Rechercher par nom"
      variant="outlined"
      InputProps={{
        startAdornment: (
          <InputAdornment position="start">
            <SearchIcon />
          </InputAdornment>
        ),
      }}
      value={searchQuery}
      onChange={handleSearchChange} // Fonction pour mettre à jour la recherche
    />
    {candidates.length > 0 ? (
      <Box sx={{ mt: 2 }}>
        <List>
          {filteredCandidates.map(candidate => (
            <Box 
              key={candidate.id} 
              sx={{
                border: '1px solid #ccc',
                borderRadius: 1,
                padding: 2,
                marginBottom: 2,
              }}
            >
              <ListItem>
                <ListItemText primary={candidate.name} secondary={candidate.email} />
              </ListItem>
            </Box>
          ))}
        </List>
      </Box>
    ) : (
      <Typography variant="body1">Aucun candidat disponible.</Typography>
    )}
  </Box>
</Modal>



<Modal open={isInviteModalOpen} onClose={handleInviteModalClose}>
  <Box sx={{
    position: 'absolute',
    top: '50%',
    left: '50%',
    transform: 'translate(-50%, -50%)',
    width: 400,
    bgcolor: 'background.paper',
    boxShadow: 24,
    p: 4,
    borderRadius: 2,
  }}>
   <Typography variant="h6" style={{ fontSize: '1.5em', color: 'rgba(35, 42, 86, 0.66)', textAlign: 'center', paddingBottom: '5%' }}>
   Inviter un candidat</Typography>
    <TextField
      fullWidth
      type="email"
      name="email"
      margin="normal"
      label="Email du candidat"
      variant="outlined"
      value={candidateEmail}
      onChange={handleEmailChange} 
    />
    <TextField
      fullWidth
      type="text"
      name="name"
      margin="normal"
      label="Nom du candidat"
      variant="outlined"
      value={candidateName}
      onChange={handleNameChange} 
    />
    <Button
      type="button"
      variant="contained"
      color="primary"
      sx={{ marginTop: 2, borderRadius: 30, width: '100%', backgroundColor: '#232A56', color: '#fff', cursor: 'pointer', '&:hover': { backgroundColor: '#1A1E40', transform: 'scale(1.05)' } }}
      onClick={handleSendTestToCandidate} // Appel à la méthode handleSendTestToCandidate pour envoyer le test
    >
      Envoyer le test
    </Button>
  </Box>
</Modal>

    </div>
  );
}

export default TestList;

package com.msystem.walking.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.msystem.walking.MainActivity;
import com.msystem.walking.databinding.ActivityLoginBinding;
import com.msystem.walking.repository.AuthRepository;
import com.msystem.walking.repository.DataRepository;
import com.msystem.walking.model.User;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private AuthRepository authRepository;
    private DataRepository dataRepository;
    private FirebaseAuth firebaseAuth;
    private boolean isLoginMode = true; // true = login, false = register

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    handleSignInResult(task);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = AuthRepository.getInstance();
        dataRepository = DataRepository.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        authRepository.initializeGoogleSignIn(this);

        setupClickListeners();
        updateUIForMode();
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            showLoading(true);
            authRepository.firebaseAuthWithGoogle(account);
            // Observer será chamado automaticamente quando o login for bem-sucedido
        } catch (ApiException e) {
            showLoading(false);
            Toast.makeText(this, "Falha no login com Google: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupClickListeners() {
        // Botão principal (Login/Cadastrar)
        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            String name = binding.etName.getText().toString().trim();

            if (validateInput(email, password, name)) {
                showLoading(true);
                if (isLoginMode) {
                    // Login com email e senha
                    firebaseAuth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(task -> {
                                showLoading(false);
                                if (task.isSuccessful()) {
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                } else {
                                    Toast.makeText(LoginActivity.this,
                                        "Erro no login: " + task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                                }
                            });
                } else {
                    // Criar conta com email e senha
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    // Conta criada com sucesso, agora criar perfil do usuário
                                    FirebaseUser user = firebaseAuth.getCurrentUser();
                                    if (user != null) {
                                        // Criar objeto User para salvar no Firestore
                                        User newUser = new User();
                                        newUser.setUserId(user.getUid());
                                        newUser.setDisplayName(name);
                                        newUser.setEmail(email);
                                        newUser.setTotalPoints(0);
                                        newUser.setTerritoriesCount(0);
                                        newUser.setTotalDistance(0.0);
                                        newUser.setCurrentStreak(0);

                                        // Salvar usuário no Firestore
                                        dataRepository.saveUser(newUser);

                                        // Atualizar display name no Firebase Auth
                                        user.updateProfile(new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                                .setDisplayName(name)
                                                .build());

                                        showLoading(false);
                                        Toast.makeText(LoginActivity.this, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                        finish();
                                    }
                                } else {
                                    showLoading(false);
                                    Toast.makeText(LoginActivity.this,
                                        "Erro ao criar conta: " + task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                                }
                            });
                }
            }
        });

        // Toggle entre Login e Registro
        binding.tvToggleMode.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateUIForMode();
        });

        // Google Sign-in
        binding.btnGoogleSignIn.setOnClickListener(v -> {
            showLoading(true);
            Intent signInIntent = authRepository.getGoogleSignInClient().getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private boolean validateInput(String email, String password, String name) {
        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Email é obrigatório");
            binding.etEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Senha é obrigatória");
            binding.etPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            binding.etPassword.setError("Senha deve ter pelo menos 6 caracteres");
            binding.etPassword.requestFocus();
            return false;
        }

        if (!isLoginMode && TextUtils.isEmpty(name)) {
            binding.etName.setError("Nome é obrigatório");
            binding.etName.requestFocus();
            return false;
        }

        return true;
    }

    private void updateUIForMode() {
        if (isLoginMode) {
            // Modo Login
            binding.tilName.setVisibility(View.GONE);
            binding.btnLogin.setText("Entrar");
            binding.tvToggleMode.setText("Não tem conta? Cadastre-se");
        } else {
            // Modo Registro
            binding.tilName.setVisibility(View.VISIBLE);
            binding.btnLogin.setText("Cadastrar");
            binding.tvToggleMode.setText("Já tem conta? Fazer login");
        }

        // Limpar erros
        binding.etEmail.setError(null);
        binding.etPassword.setError(null);
        binding.etName.setError(null);
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!show);
        binding.btnGoogleSignIn.setEnabled(!show);
        binding.tvToggleMode.setEnabled(!show);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Observer para mudanças no estado de autenticação
        authRepository.getUserLiveData().observe(this, user -> {
            if (user != null) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });
    }
}

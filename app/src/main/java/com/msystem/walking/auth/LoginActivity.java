package com.msystem.walking.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.msystem.walking.MainActivity;
import com.msystem.walking.databinding.ActivityLoginBinding;
import com.msystem.walking.repository.AuthRepository;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private AuthRepository authRepository;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        authRepository.firebaseAuthWithGoogle(account);
                    } catch (ApiException e) {
                        Toast.makeText(this, "Falha no login com Google", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = AuthRepository.getInstance();
        authRepository.initializeGoogleSignIn(this);

        setupObservers();
        setupClickListeners();
    }

    private void setupObservers() {
        authRepository.getUserLiveData().observe(this, firebaseUser -> {
            if (firebaseUser != null) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }
        });
    }

    private void setupClickListeners() {
        binding.btnLogin.setOnClickListener(v -> {
            if (binding.etEmail.getText() == null || binding.etPassword.getText() == null) {
                Toast.makeText(this, "Erro nos campos de entrada", Toast.LENGTH_SHORT).show();
                return;
            }

            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
                return;
            }

            authRepository.signInWithEmailAndPassword(email, password);
        });

        binding.btnRegister.setOnClickListener(v -> {
            if (binding.etEmail.getText() == null || binding.etPassword.getText() == null || binding.etName.getText() == null) {
                Toast.makeText(this, "Erro nos campos de entrada", Toast.LENGTH_SHORT).show();
                return;
            }

            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            String name = binding.etName.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
                return;
            }

            authRepository.createUserWithEmailAndPassword(email, password, name);
        });

        binding.btnGoogleSignIn.setOnClickListener(v -> {
            if (authRepository.getGoogleSignInClient() == null) {
                Toast.makeText(this, "Google Sign-In não disponível. Use email/senha.", Toast.LENGTH_LONG).show();
                return;
            }

            try {
                Intent signInIntent = authRepository.getGoogleSignInClient().getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            } catch (Exception e) {
                Toast.makeText(this, "Erro no Google Sign-In. Use email/senha.", Toast.LENGTH_LONG).show();
            }
        });
    }
}

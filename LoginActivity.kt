package com.blackbotek.app1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val prefs = getSharedPreferences("BOT_CONFIG", Context.MODE_PRIVATE)

        // Si ya tiene ID registrado, ir directo al panel
        if (UserSession.getId(prefs) != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val etTelefono      = findViewById<EditText>(R.id.etTelefono)
        val etContrasena    = findViewById<EditText>(R.id.etContrasena)
        val checkSesion     = findViewById<CheckBox>(R.id.checkGuardarSesion)
        val btnRegistrar    = findViewById<Button>(R.id.btnRegistrar)

        btnRegistrar.setOnClickListener {
            val telefono   = etTelefono.text.toString().trim()
            val contrasena = etContrasena.text.toString().trim()

            if (telefono.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Generar ID único centralizado
            val nuevoID = UserSession.getOrCreateId(prefs)

            // Guardar datos básicos (SIN guardar la contraseña en texto plano)
            prefs.edit().apply {
                putString("telefono", telefono)
                putBoolean("guardar_sesion", checkSesion.isChecked)
                apply()
            }

            // Registrar en Firebase con estado "pendiente"
            FirebaseDatabase.getInstance()
                .getReference("Usuarios").child(nuevoID)
                .setValue(mapOf(
                    "id"            to nuevoID,
                    "telefono"      to telefono,
                    "estado"        to "pendiente",
                    "dias_restantes" to 0,
                    "fecha_registro" to System.currentTimeMillis()
                ))

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}

package com.rocha.centrotareas.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rocha.centrotareas.viewmodel.TareasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaTareas(vm: TareasViewModel) {
    val mensaje by vm.mensaje.collectAsState()
    val progreso by vm.progreso.collectAsState()
    val corriendo by vm.corriendo.collectAsState()
    var toques by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CentroTareas",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2)
                )
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tarjeta: Tarea con progreso (Pasos 3 y 4)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Tarea con progreso",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )

                    // Botón 1: Tócame (contador centrado)
                    Button(
                        onClick = { toques++ },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Tócame: $toques",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Fila de botones centrados: Iniciar y Cancelar (Paso 4)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { vm.tareaLarga() },
                            enabled = !corriendo,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1976D2),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFE2E8F0),
                                disabledContentColor = Color(0xFF94A3B8)
                            ),
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Iniciar",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { vm.cancelar() },
                            enabled = corriendo,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(
                                1.5.dp,
                                if (corriendo) Color(0xFF1976D2) else Color(0xFFCBD5E1)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF1976D2),
                                disabledContentColor = Color(0xFF94A3B8)
                            ),
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Cancelar",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Barra de progreso interactiva (0% a 100%)
                    LinearProgressIndicator(
                        progress = { progreso },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .height(6.dp),
                        color = Color(0xFF1976D2),
                        trackColor = Color(0xFFE2E8F0)
                    )

                    // Porcentaje numérico (ej. 40 % o 62 %)
                    Text(
                        text = "${(progreso * 100).toInt()} %",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold
                    )

                    // Texto del mensaje ("Listo", "Trabajando...", "Cancelado", "Terminó")
                    Text(
                        text = mensaje,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

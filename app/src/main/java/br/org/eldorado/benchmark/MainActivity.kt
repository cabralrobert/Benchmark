package br.org.eldorado.benchmark

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import kotlin.math.sqrt
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity() {

    // Componentes do layout
    private lateinit var btnStartBenchmark: Button
    private lateinit var tvResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnStartBenchmark = findViewById(R.id.btnStartBenchmark)
        tvResult = findViewById(R.id.tvResult)

        btnStartBenchmark.setOnClickListener {
            runBenchmark()
        }
    }

    private fun runBenchmark() {
        tvResult.text = "Executando benchmark..."
        Thread {
            val startTime = System.currentTimeMillis()

            val cpuScore = stressCpuPrimes()
            val gpuScore = stressGpuLong()
            val memoryScore = stressMemoryAdvanced()

            val totalScore = (5 * cpuScore) + (3 * gpuScore) + (2 * memoryScore) / 10

            val endTime = System.currentTimeMillis()
            val totalTime = endTime - startTime

            // Lê o número da última execução
            val runNumber = getLastRunNumber() + 1

            // Exporta os resultados para o CSV
            exportResultsToCsv(runNumber, totalTime, cpuScore, gpuScore, memoryScore, totalScore)

            runOnUiThread {
                tvResult.text = """
                    Benchmark concluído:
                    Execução: $runNumber
                    Tempo Total: ${totalTime / 1000} segundos
                    CPU Score: $cpuScore
                    GPU Score: $gpuScore
                    Memória Score: $memoryScore
                    Score Total: $totalScore
                """.trimIndent()
            }
        }.start()
    }

    private fun stressCpuPrimes(): Int {
        val iterations = 1_000_000
        val primes = mutableListOf<Int>()

        val timeTaken = measureTimeMillis {
            for (i in 2..iterations) {
                if (isPrime(i)) {
                    primes.add(i)
                }
            }
        }

        // Baseando o score em um fator de desempenho normalizado
        val referenceTime = 30000 // Milissegundos em um dispositivo de referência
        val score = ((referenceTime.toDouble() / timeTaken)).toInt()
        return score
    }

    private fun isPrime(number: Int): Boolean {
        if (number < 2) return false
        for (i in 2..sqrt(number.toDouble()).toInt()) {
            if (number % i == 0) return false
        }
        return true
    }

    private fun stressGpuLong(): Int {
        val bitmapSize = 10000 // Aumenta ainda mais o tamanho do bitmap para saturar a GPU
        val paint = Paint()
        val bitmap = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        var iterations = 0
        val startTime = System.currentTimeMillis()

        try {
            while (System.currentTimeMillis() - startTime < 30000) {
                for (i in 0..20000) { // Operações intensivas
                    paint.color = (0xFF000000 + i * 1234567 % 0xFFFFFF).toInt()
                    canvas.drawCircle(
                        (i * 10 % bitmapSize).toFloat(),
                        (i * 15 % bitmapSize).toFloat(),
                        (i % 50).toFloat(),
                        paint
                    )
                }
                iterations++
            }
        } catch (e: Exception) {
            Toast.makeText(applicationContext, "Error: " + e.message, Toast.LENGTH_LONG)
            // Captura qualquer erro que interrompa o teste (ex.: falta de memória gráfica)
        }

        val totalTime = System.currentTimeMillis() - startTime
        val score = ((iterations.toDouble() / 1000) * 2000).toInt() // Baseie o score em iterações
        return score
    }



    private fun stressMemoryAdvanced(): Int {
        val allocations = mutableListOf<ByteArray>()
        var allocatedMemory = 0L
        val baseBlockSize = 10 * 1024 * 1024 // Bloco inicial de 10 MB
        var operationsCount = 0L

        val startTime = System.currentTimeMillis()
        try {
            while (true) {
                // Aumentar dinamicamente o tamanho dos blocos
                val currentBlockSize = (baseBlockSize + allocatedMemory / (1024 * 1024)).toInt()
                val allocation = ByteArray(currentBlockSize)

                // Preencher os dados no bloco com valores específicos
                for (i in allocation.indices) {
                    allocation[i] = (i % 256).toByte()
                }

                allocations.add(allocation)
                allocatedMemory += currentBlockSize

                // Executar cálculos intensivos sobre os blocos
                for (block in allocations) {
                    for (i in block.indices step 8) { // Processar a cada 8 bytes para intensificar os cálculos
                        val value = block[i].toInt()
                        block[i] = ((value * value + i) % 256).toByte() // Cálculo mais pesado
                        operationsCount++
                    }
                }
            }
        } catch (e: OutOfMemoryError) {
            // Memória máxima atingida
        }

        val endTime = System.currentTimeMillis()
        val elapsedTime = endTime - startTime

        // Calcular score
        val memoryInGb = allocatedMemory.toDouble() / (1024 * 1024 * 1024) // Memória em GB
        val operationsPerMb = operationsCount.toDouble() / (allocatedMemory / (1024 * 1024)) // Operações por MB
        val normalizedTime = elapsedTime / 1000.0 // Tempo em segundos

        // Ajustar o score para a casa das centenas
        val rawScore = (memoryInGb * 100 + (operationsPerMb / normalizedTime * 10))
        val score = (rawScore / 10000).toInt() // Reduzir a escala para valores na casa das centenas

        return score
    }


    private fun getLastRunNumber(): Int {
        val file = File(getExternalFilesDir(null), "benchmark_results.csv")
        if (!file.exists()) return 0

        val lastLine = file.readLines().lastOrNull()
        return lastLine?.split(",")?.firstOrNull()?.toIntOrNull() ?: 0
    }

    private fun exportResultsToCsv(
        runNumber: Int,
        totalTime: Long,
        cpuScore: Int,
        gpuScore: Int,
        memoryScore: Int,
        totalScore: Int
    ) {
        val file = File(getExternalFilesDir(null), "benchmark_results.csv")
        val isNewFile = !file.exists()

        FileWriter(file, true).use { writer ->
            if (isNewFile) {
                writer.append("Execução,Tempo Total (ms),CPU Score,GPU Score,Memória Score,Score Total\n")
            }
            writer.append("$runNumber,$totalTime,$cpuScore,$gpuScore,$memoryScore,$totalScore\n")
        }
    }
}

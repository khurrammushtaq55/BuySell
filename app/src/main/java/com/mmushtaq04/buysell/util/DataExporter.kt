package com.mmushtaq04.buysell.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.gson.GsonBuilder
import com.mmushtaq04.buysell.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DataExporter {

    private const val TAG = "DataExporter"

    suspend fun exportDataZip(context: Context) = withContext(Dispatchers.IO) {
        runCatching {
            val db = AppDatabase.getInstance(context)
            val user = db.userDao().getPrimaryUser()
            val meta = db.appMetaDao().getAppMeta()
            val shopId = meta?.activeShopId?.ifBlank { null } ?: user?.shopId ?: ""

            val stockList = db.stockItemDao().getAllStockItems(shopId)
            val txnList = db.txnDao().getAllTxns(shopId)
            val partyList = db.partyDao().getAllParties(shopId)
            val paymentList = db.paymentDao().getAllPayments(shopId)

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val zipFile = File(exportDir, "buysell_export_$timeStamp.zip")

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                // 1. Full Backup JSON
                val gson = GsonBuilder().setPrettyPrinting().create()
                val fullBackup = mapOf(
                    "shopId" to shopId,
                    "exportedAt" to System.currentTimeMillis(),
                    "stockItems" to stockList,
                    "transactions" to txnList,
                    "parties" to partyList,
                    "payments" to paymentList
                )
                val jsonStr = gson.toJson(fullBackup)
                zos.putNextEntry(ZipEntry("buysell_backup.json"))
                zos.write(jsonStr.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // 2. Stock Items CSV
                val stockCsv = StringBuilder("ID,Brand,Model,IMEI,Category,Quantity,RemainingQty,Status,Condition,Notes\n")
                stockList.forEach { s ->
                    stockCsv.append("\"${s.id}\",\"${s.brand}\",\"${s.model}\",\"${s.identifier ?: ""}\",\"${s.categoryId}\",${s.quantity},${s.remainingQty},\"${s.status}\",\"${s.condition}\",\"${s.notes ?: ""}\"\n")
                }
                zos.putNextEntry(ZipEntry("stock_items.csv"))
                zos.write(stockCsv.toString().toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // 3. Transactions CSV
                val txnCsv = StringBuilder("TxnID,Type,TxnDate,PartyID,TotalAmount,Scope,Note\n")
                txnList.forEach { t ->
                    txnCsv.append("\"${t.id}\",\"${t.type}\",\"${t.txnDate}\",\"${t.partyId ?: ""}\",${t.totalAmount},\"${t.scope}\",\"${t.note ?: ""}\"\n")
                }
                zos.putNextEntry(ZipEntry("transactions.csv"))
                zos.write(txnCsv.toString().toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // 4. Parties CSV
                val partyCsv = StringBuilder("PartyID,Name,Phone,CNIC,Address,Notes\n")
                partyList.forEach { p ->
                    partyCsv.append("\"${p.id}\",\"${p.name}\",\"${p.phone ?: ""}\",\"${p.cnic ?: ""}\",\"${p.address ?: ""}\",\"${p.notes ?: ""}\"\n")
                }
                zos.putNextEntry(ZipEntry("parties.csv"))
                zos.write(partyCsv.toString().toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // 5. Payments CSV
                val payCsv = StringBuilder("PaymentID,TxnID,PartyID,Direction,Amount,Method,PayDate\n")
                paymentList.forEach { py ->
                    payCsv.append("\"${py.id}\",\"${py.txnId ?: ""}\",\"${py.partyId}\",\"${py.direction}\",${py.amount},\"${py.method}\",\"${py.payDate}\"\n")
                }
                zos.putNextEntry(ZipEntry("payments.csv"))
                zos.write(payCsv.toString().toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                zipFile
            )

            withContext(Dispatchers.Main) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, "${context.getString(com.mmushtaq04.buysell.R.string.app_name)} Backup Data ZIP ($timeStamp)")
                    putExtra(Intent.EXTRA_TEXT, context.getString(com.mmushtaq04.buysell.R.string.export_backup_data_title, context.getString(com.mmushtaq04.buysell.R.string.app_name)))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share or Save ZIP Backup"))
            }
        }.onFailure { e ->
            Log.e(TAG, "Export ZIP error: ${e.localizedMessage}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

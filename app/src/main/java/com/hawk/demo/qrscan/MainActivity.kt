package com.hawk.demo.qrscan

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.anthonycr.grant.PermissionsManager
import com.anthonycr.grant.PermissionsResultAction
import com.hawk.qrscan.activity.CardPreviewActivity
import com.hawk.qrscan.util.Utils
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val permissions:Array<String> = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA)
    private val CODE_SCAN = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener { showPreview() }
    }

    private fun showPreview() {
        PermissionsManager.getInstance().requestAllManifestPermissionsIfNecessary(this@MainActivity,
                object : PermissionsResultAction() {
                    override fun onGranted() {
                        val intent = Intent(this@MainActivity, CardPreviewActivity::class.java)
                        startActivityForResult(intent, CODE_SCAN)
                    }

                    override fun onDenied(permission: String) {

                    }
                })

    }

    //因为权限管理类无法监听系统，所以需要重写onRequestPermissionResult方法，更新权限管理类，并回调结果。这个是必须要有的。
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == CODE_SCAN) {
                val code = data.getStringExtra(Utils.Extra_Code)
                val bitmap = data.getParcelableExtra<Bitmap>(Utils.Extra_Bitmap)

                Log.i("test", "^^^^^^^@" + code)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                }
            }
        }
    }
}

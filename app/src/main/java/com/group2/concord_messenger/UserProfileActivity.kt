package com.group2.concord_messenger

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.group2.concord_messenger.ConcordDatabase.Companion.getCurrentUser
import com.group2.concord_messenger.dialogs.PickPhotoFragment
import com.group2.concord_messenger.model.UserProfile
import com.group2.concord_messenger.utils.checkPermissions
import com.group2.concord_messenger.utils.getBitmap
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream

class UserProfileActivity : AppCompatActivity(), PickPhotoFragment.PickPhotoListener {
    private lateinit var profileTitle: TextView
    private lateinit var usernameField: EditText
    private lateinit var emailField: EditText
    private lateinit var bioField: EditText
    private lateinit var editButton: ImageButton
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private lateinit var changeImageButton: Button

    private lateinit var userData: UserProfile

    private lateinit var profileImgView: ImageView
    private lateinit var profileImgName: String
    private lateinit var profileImgUri: Uri

    private lateinit var cameraResult: ActivityResultLauncher<Intent>
    private lateinit var galleryResult: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        profileTitle = findViewById(R.id.profile_title)
        usernameField = findViewById(R.id.username_field)
        emailField = findViewById(R.id.email_field)
        bioField = findViewById(R.id.bio_field)
        editButton = findViewById(R.id.edit_profile_button)
        saveButton = findViewById(R.id.save_button)
        cancelButton = findViewById(R.id.cancel_button)
        changeImageButton = findViewById(R.id.image_button)

        userData = intent.extras?.get("user") as UserProfile

        profileImgView = findViewById(R.id.profile_image)
        profileImgName = userData.uId + "_profile_photo.jpg"
        profileImgUri = FileProvider.getUriForFile(
             this,
            "com.group2.concord_messenger",
                    File(this.getExternalFilesDir(null), profileImgName))

        loadProfile()

        saveButton.setOnClickListener{
            saveProfile()
            finish()
        }

        cancelButton.setOnClickListener{
            changeImageButton.visibility = View.GONE
            saveButton.visibility = View.GONE
            cancelButton.visibility = View.GONE
            usernameField.isEnabled = false
            bioField.isEnabled = false
            finish()
        }

        changeImageButton.setOnClickListener {
            checkPermissions(this)
            val pick = PickPhotoFragment()
            pick.show(supportFragmentManager, "PickPhoto")
        }

        editButton.setOnClickListener {
            changeImageButton.visibility = View.VISIBLE
            saveButton.visibility = View.VISIBLE
            cancelButton.visibility = View.VISIBLE
            usernameField.isEnabled = true
            // TODO: should the email be updatable?
            bioField.isEnabled = true
        }

        cameraResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ){ result: ActivityResult ->
            println(result)
            if(result.resultCode == Activity.RESULT_OK){
                val bitmap = getBitmap(this, profileImgUri, profileImgUri.path!!)
                profileImgView.setImageBitmap(bitmap)
                profileImgView.setImageURI(profileImgUri) // this fixes image orientation issues
            }
        }

        galleryResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ){ result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                // https://developer.android.com/training/data-storage/shared/documents-files
                // Copy selected image into the profile image file
                val parcelFileDescriptor: ParcelFileDescriptor =
                    contentResolver.openFileDescriptor(result.data!!.data!!, "r")!!
                val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor
                val bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                parcelFileDescriptor.close()

                val outputStream = FileOutputStream(File(this.getExternalFilesDir(null),
                                                    profileImgName))
                profileImgView.setImageBitmap(bitmap)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.close()
            }
        }
    }

    private fun loadProfile() {
        getCurrentUser{
            val currUser = it!!
            if (currUser.uId == userData.uId) {
                userData = currUser     // use the latest fetch
                editButton.visibility = View.VISIBLE
            }
        }
        // fetch latest copy
        val fsDb = FirebaseFirestore.getInstance()
        fsDb.collection("users").document(userData.uId).get()
            .addOnSuccessListener {
                userData = it.toObject(UserProfile::class.java)!!
                println("${userData.uId}, ${userData.bio}, ${userData.profileImg}")
                profileTitle.text = userData.userName + "'s Profile"
                usernameField.setText(userData.userName)
                emailField.setText(userData.email)
                bioField.setText(userData.bio)

                // Load image from firebase
                // https://firebase.google.com/docs/storage/android/download-files#kotlin+ktx_2
                val storage = Firebase.storage
                // Create a reference to a file from a Google Cloud Storage URI
                val gsReference = storage.getReferenceFromUrl(userData.profileImg)
                gsReference.getFile(profileImgUri).addOnSuccessListener {
                    // Local temp file has been created
                    val bitmap = getBitmap(this, profileImgUri, profileImgUri.path!!)
                    profileImgView.setImageBitmap(bitmap)
                }
            }
    }

    private fun saveProfile() {
        val storage = Firebase.storage
        val imageRef = storage.reference.child("images/${profileImgUri.lastPathSegment}")
        imageRef.putFile(profileImgUri).addOnFailureListener {
            // Handle unsuccessful uploads
        }.addOnSuccessListener { taskSnapshot ->
            // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
            // ...
        }
        val fsDb = FirebaseFirestore.getInstance()
        // only the current user can be updated
        fsDb.collection("users").document(FirebaseAuth.getInstance().currentUser!!.uid)
            .update("userName", usernameField.text.toString(),
                "bio", bioField.text.toString(),
                "profileImg", imageRef.toString())
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
    }

    private fun takeProfilePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, profileImgUri)
        cameraResult.launch(intent)
    }

    private fun pickFromGallery() {
        // https://developer.android.com/training/camera/photobasics
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, profileImgUri)
        galleryResult.launch(intent)
    }

    override fun onComplete(dialog: DialogFragment) {
        val temp : PickPhotoFragment = dialog as PickPhotoFragment
        if (temp.pickedOption == 0) {
            takeProfilePhoto()
        } else {
            pickFromGallery()
        }
    }

}
package com.example.knightshade.internship


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.tamir7.contacts.Contacts
import com.github.tamir7.contacts.PhoneNumber
import com.vicpin.krealmextensions.delete
import com.vicpin.krealmextensions.queryAll
import com.vicpin.krealmextensions.save
import io.realm.Realm
import io.realm.RealmObject
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar_layout.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast

class MainActivity : AppCompatActivity() {

    private final val MY_PERMISSION_READ_CONTACT = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Contacts.initialize(this)
        Realm.init(this)

        setContentView(R.layout.activity_main)
        tv_title.text = "SELECT  CONTACTS"
        setSupportActionBar(toolbar)

        requestPermission()
        getContact()

        rv_select_contacts.setHasFixedSize(true)
        rv_select_contacts.layoutManager = LinearLayoutManager(this)
        rv_select_contacts.adapter = ContactAdapter(getContact(), this)

        fab_done.setOnClickListener {
            startActivity<Members>()
            finish()
        }
    }


    private fun getContact(): List<ContactModel> {
        val contact = Contacts.getQuery().find()
        val contacts = arrayListOf<ContactModel>()

        contact.forEach { c ->
            contacts.add(ContactModel(
                    c.displayName,
                    getPhoneNumber(c.phoneNumbers),
                    c.photoUri
            ))
        }

        return contacts

    }

    private fun getPhoneNumber(phoneNumber: List<PhoneNumber>):String {
        phoneNumber.forEach { p ->
            if (p.number.isNotEmpty()){
                return p.number
            }
        }
        return ""
    }

    private fun permissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        if(permissionGranted()){
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_CONTACTS),
                    MY_PERMISSION_READ_CONTACT)
        } else {
            Snackbar.make(select_contact_activity, "Permission already granted", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSION_READ_CONTACT -> {
                if( (grantResults.isNotEmpty()) && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                } else {
                    requestPermission()
                }
                return
            }
            else -> {
                // Ignore all other requests
            }
        }
    }
}

private class ContactAdapter( private val contacts: List<ContactModel>, private val context: Context): RecyclerView.Adapter<ContactAdapter.ViewHolder>(){

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val name = view.findViewById<TextView>(R.id.tv_name) as TextView
        val phone = view.findViewById<TextView>(R.id.tv_phone_number) as TextView
        val profile = view.findViewById<ImageView>(R.id.iv_profile) as ImageView
        val parentLayout = view.findViewById<CardView>(R.id.cv_contact_item) as CardView
        val tickIcon = view.findViewById<ImageView>(R.id.iv_selected) as ImageView
        var selected = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val item = LayoutInflater.from(parent.context)
                .inflate(R.layout.contact_item, parent, false)

        return ViewHolder(item)
    }

    override fun getItemCount(): Int = contacts.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        holder.name.text = contact.name
        holder.phone.text = contact.number

        if (contact.photoUri != null){
            Glide.with(context).load(contact.photoUri)
                    .apply(RequestOptions.circleCropTransform())
                    .into(holder.profile)
        }

        holder.parentLayout.setOnClickListener {

            if (!holder.selected) {
                holder.tickIcon.visibility = View.VISIBLE
                contact.save()
            } else {
                holder.tickIcon.visibility = View.INVISIBLE
                ContactModel().delete { equalTo("name", contact.name) }
            }
            holder.selected = !holder.selected

        }
    }
}

open class ContactModel(
        open var name: String = "",
        open var number: String = "",
        open var photoUri: String? = "") : RealmObject()


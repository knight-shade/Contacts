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
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.tamir7.contacts.Contacts
import com.github.tamir7.contacts.PhoneNumber
import com.miguelcatalan.materialsearchview.MaterialSearchView
import com.vicpin.krealmextensions.delete
import com.vicpin.krealmextensions.query
import com.vicpin.krealmextensions.queryAll
import com.vicpin.krealmextensions.save
import io.michaelrocks.libphonenumber.android.NumberParseException
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import io.michaelrocks.libphonenumber.android.Phonenumber
import io.realm.Realm
import io.realm.RealmObject
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.selects.select
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast

class MainActivity : AppCompatActivity() {

    private val MY_PERMISSION_READ_CONTACT = 1

    // Used by Material Search Bar
    lateinit var fullList: List<ContactModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Contacts.initialize(this)
        Realm.init(this)

        // Checking whether activity is started to add members explicitly
        // when database already container members .
        if (intent.getBooleanExtra("check", true)) {
            alreadyHasMembers()
        }

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        // Getting the permission
        requestPermission()

        fab_done.setOnClickListener {
            startActivity<Members>()
            finish()
        }

        // The search bar
        search_view.setOnQueryTextListener(object: MaterialSearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Since results are loaded instantly, query submission is not required.
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                val results = fullList.filter {
                    it.name.startsWith(newText, ignoreCase = true)
                }

                rv_select_contacts.adapter = ContactAdapter(results)
                return false
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options, menu)
        val item = menu.findItem(R.id.action_search)
        search_view.setMenuItem(item);
        return true
    }

    // Setups the activity
    private fun setUp() {
        fullList = getContact()
        rv_select_contacts.layoutManager = LinearLayoutManager(this)
        rv_select_contacts.adapter = ContactAdapter(fullList)
    }

    private fun alreadyHasMembers() {
        if (ContactModel().queryAll().isNotEmpty()){
            startActivity<Members>()
            finish()
        }
    }

    // Getting contacts from content provider
    private fun getContact(): List<ContactModel> {
        val contact = Contacts.getQuery().find()
        val contacts = arrayListOf<ContactModel>()

        contact.forEach { c ->
            if (isMobileNumber(getPhoneNumber(c.phoneNumbers))) {
                contacts.add(ContactModel(
                        c.displayName,
                        getPhoneNumber(c.phoneNumbers),
                        c.photoUri
                ))
            } else {
                Log.d(this.javaClass.simpleName, "Landline number ${getPhoneNumber(c.phoneNumbers)}")
            }
        }

        return contacts

    }

    // Return true when given number is a mobile number
    private fun isMobileNumber(phNumber: String): Boolean {
        val phoneNumberUtil: PhoneNumberUtil = PhoneNumberUtil.createInstance(this)
        var phoneNumber: Phonenumber.PhoneNumber? = null

        try {
            phoneNumber = phoneNumberUtil.parse(phNumber, "IN")
        } catch (e: NumberParseException) {
            Log.d(this.javaClass.simpleName, e.toString())
        }

        if (phoneNumber == null){
            return false
        } else {
            val isMobile = phoneNumberUtil.getNumberType(phoneNumber)
            return PhoneNumberUtil.PhoneNumberType.MOBILE == isMobile
        }

    }

    // PhoneNumber being list and some are randomly empty, this take care of it.
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
            setUp()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSION_READ_CONTACT -> {
                if( (grantResults.isNotEmpty()) && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    setUp()
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

private class ContactAdapter( private val contacts: List<ContactModel>): RecyclerView.Adapter<ContactAdapter.ViewHolder>(){

    lateinit var context: Context

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

        context = parent.context

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
        } else {
            Glide.with(context).load(R.drawable.profile)
                    .apply(RequestOptions.circleCropTransform())
                    .into(holder.profile)
        }

        if ( contact.photoUri == null){
            Log.d("PictureUri", "NULL")
        } else {
            Log.d("Picture: ", contact.photoUri)
        }

        holder.tickIcon.visibility = View.INVISIBLE

        holder.parentLayout.setOnClickListener {

            if (!holder.selected) {
                val members = ContactModel().query { equalTo("name", contact.name) }
                if( members.isNotEmpty()) {
                    context.toast("Already selected")
                } else {
                    holder.tickIcon.visibility = View.VISIBLE
                    contact.save()
                    holder.selected = !holder.selected
                }

            } else {
                holder.tickIcon.visibility = View.INVISIBLE
                ContactModel().delete { equalTo("name", contact.name) }
                holder.selected = !holder.selected
            }


        }
    }
}

open class ContactModel(
        open var name: String = "",
        open var number: String = "",
        open var photoUri: String? = "") : RealmObject()


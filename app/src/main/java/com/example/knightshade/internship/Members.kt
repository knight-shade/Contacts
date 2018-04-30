package com.example.knightshade.internship

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
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
import com.vicpin.krealmextensions.delete
import com.vicpin.krealmextensions.queryAll
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_members.*
import kotlinx.android.synthetic.main.toolbar_layout.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast

class Members : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Realm.init(this)

        setContentView(R.layout.activity_members)

        tv_title.text = "MEMBERS"
        setSupportActionBar(toolbar)

        rv_members.setHasFixedSize(true)
        rv_members.layoutManager = LinearLayoutManager(this)
        rv_members.adapter = MembersAdapter(ContactModel().queryAll().toMutableList())

        // Add more members
        fab_add.setOnClickListener {
            startActivity<MainActivity>( "check" to false)
            finish()
        }
    }
}


private class MembersAdapter( private val contacts: MutableList<ContactModel>): RecyclerView.Adapter<MembersAdapter.ViewHolder>(){

    lateinit var context: Context

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val name = view.findViewById<TextView>(R.id.tv_name) as TextView
        val phone = view.findViewById<TextView>(R.id.tv_phone_number) as TextView
        val profile = view.findViewById<ImageView>(R.id.iv_profile) as ImageView
        val parentLayout = view.findViewById<CardView>(R.id.cv_contact_item) as CardView
        var deleted = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val item = LayoutInflater.from(parent.context)
                .inflate(R.layout.member_item, parent, false)

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

        holder.parentLayout.setOnClickListener {

            if (!holder.deleted) {
                ContactModel().delete { equalTo("name", contact.name) }
                contacts.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, contacts.size)
            } else {
                context.toast("Already deleted")
            }
        }
    }
}
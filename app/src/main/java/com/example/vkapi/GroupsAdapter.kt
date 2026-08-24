package com.example.vkapi

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.vkapi.PubView

class GroupsAdapter(initialData: List<PubView>, var context: Context) :  RecyclerView.Adapter<GroupsAdapter.PubViewHolder>() {
    private val pubs = initialData.toMutableList()
    class PubViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val picture: ImageView = view.findViewById(R.id.picture)
        val name: TextView = view.findViewById(R.id.name)
        val place: ConstraintLayout = view.findViewById(R.id.Groupe)
        val params: TextView = view.findViewById(R.id.params)
        val unrPosts: TextView = view.findViewById(R.id.unreadedPosts)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PubViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.pub_layout, parent, false)
        return PubViewHolder(view)
    }

    override fun onBindViewHolder(holder: PubViewHolder, position: Int) {
        val pub = pubs[position]
        holder.picture.load(pub.pic)
        holder.name.text = pub.name
        holder.unrPosts.text = "${pub.unreadPosts}"

        holder.place.setOnClickListener {
            JsonHolder.jsonItem = pub.posts

            val intent = Intent(context, WallActivity::class.java)
            intent.putExtra("name", pub.name)
            intent.putExtra("pic", pub.pic)
            intent.putExtra("ref", pub.ref)
            intent.putExtra("unrp", pub.unreadPosts)
            context.startActivity(intent)
        }

        holder.params.setOnClickListener { view ->
            val db = DBHelper(view.context, null)
            showPopupMenu(view, position, db, pub.ref)
        }
    }

    override fun getItemCount(): Int = pubs.size

    fun updateData(new: List<PubView>) {
        pubs.clear()
        pubs.addAll(new)
        notifyDataSetChanged()
    }

    private fun showPopupMenu(view: View, position: Int, db: DBHelper, ref: String) {
        val popup = PopupMenu(view.context, view)
        popup.menuInflater.inflate(R.menu.dropdown_menu, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.optdel -> {
                    Log.d("refvalue", ref)
                    db.delete_pub(ref)
                    val newList = pubs.filter { it.ref != ref }
                    updateData(newList)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
}
package app.example.landmarkremarkapplication.ui.activity

import CircleTransform
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import app.example.landmarkremarkapplication.R
import app.example.landmarkremarkapplication.constrants.AppConstants

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import app.example.landmarkremarkapplication.databinding.ActivityMapsBinding
import app.example.landmarkremarkapplication.model.entity.LocationInfo
import app.example.landmarkremarkapplication.model.entity.NotesModel
import app.example.landmarkremarkapplication.model.entity.UserModel
import app.example.landmarkremarkapplication.ui.adapter.ListAdapter
import app.example.landmarkremarkapplication.ui.dialog.InputDialogNote
import app.example.landmarkremarkapplication.widget.VNCharacterUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.collections.ArrayList

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, ListAdapter.OnclickItem {

    private lateinit var mMap: GoogleMap
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMapsBinding
    private lateinit var database: DatabaseReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var uid: String
    private lateinit var adapter: ListAdapter
    private var lat: Double = 0.0
    private var long: Double = 0.0
    private var currentMarker: Marker? = null
    private var newMarker: Marker? = null
    private var user = UserModel()
    private var listNotes = ArrayList<NotesModel>()
    private var listResult = ArrayList<NotesModel>()
    private var isShowPined = false
    var ignoreTextChange = false
    val listMarkers = mutableListOf<Marker>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        auth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (auth.currentUser != null) {
            uid = auth.currentUser!!.uid
            showUser()
        }
        binding.llInformationNotes.root.visibility = View.GONE
        requestLocationPermissions()
        setAdapter()
        getNotes()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        binding.imvLocation.setImageResource(R.drawable.ic_union)
        binding.llMyLocation.setOnClickListener {
            moveCamera(lat, long)
        }
        binding.crAvatar.setOnClickListener {
            startActivity(Intent(this@MapsActivity, InformationActivity::class.java))
        }

        binding.llAddNote.setOnClickListener {
            if (currentMarker == null) {
                val inputDialogNote = InputDialogNote(this)
                inputDialogNote.setOnActionDone(object : InputDialogNote.OnActionDone {
                    override fun onActionDone(isConfirm: Boolean, note: String) {
                        if (isConfirm) {
                            if (note.isNotEmpty()) {
                                addMarker(lat, long, note)
                                upNotes(lat, long, note)
                                currentMarker!!.remove()
                                inputDialogNote.dismiss()
                            } else {
                                AppConstants.setToast(
                                    this@MapsActivity,
                                    getString(R.string.insufficient_data),
                                    getString(R.string.please_not_empty_notes),
                                    6
                                )
                            }
                        } else {
                            currentMarker!!.remove()
                        }

                    }
                })
                inputDialogNote.show()
            }
        }

        binding.edtSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query!!.isNotEmpty()) {
                    searchRecycleView(query)
                } else {
                    binding.llListSearch.visibility = View.GONE
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (ignoreTextChange) return true
                if (newText!!.isNotEmpty()) {
                    searchRecycleView(newText)
                } else {
                    binding.llListSearch.visibility = View.GONE
                }
                return true
            }

        })
        binding.edtSearch.setOnQueryTextFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                if (binding.edtSearch.query.isNotEmpty()) {
                    searchRecycleView(binding.edtSearch.query.toString())
                }
            }
        }

        binding.llOnOffMarker.setOnClickListener {
            if (isShowPined) {
                binding.imvMarker.setImageResource(R.drawable.ic_location_unpined)
                setMarker()
                isShowPined = false
            } else {
                binding.imvMarker.setImageResource(R.drawable.ic_location_pined)
                binding.llInformationNotes.root.visibility = View.GONE
                // Clear the markers from the map
                for (marker in listMarkers) {
                    marker.remove()
                }
                isShowPined = true
            }


        }


        mMap.setOnMapLongClickListener {
            newMarker = mMap.addMarker(MarkerOptions().position(it))

            val inputDialogNote = InputDialogNote(this)
            inputDialogNote.setOnActionDone(object : InputDialogNote.OnActionDone {
                override fun onActionDone(isConfirm: Boolean, note: String) {
                    if (isConfirm) {
                        if (note.isNotEmpty()) {
                            upNotes(it.latitude, it.longitude, note)
                            newMarker!!.remove()
                            inputDialogNote.dismiss()
                        } else {
                            AppConstants.setToast(
                                this@MapsActivity,
                                getString(R.string.insufficient_data),
                                getString(R.string.please_not_empty_notes),
                                6
                            )
                        }
                    } else {
                        newMarker!!.remove()
                        inputDialogNote.dismiss()
                    }

                }
            })
            inputDialogNote.show()
        }
        mMap.setOnMarkerClickListener { clickedMarker ->
            // Xử lý sự kiện khi người dùng nhấn vào ghim
            val info = clickedMarker.tag as? NotesModel
            info?.let {
                setDataNotes(it)
            }
            true
        }
        mMap.setOnMapClickListener {
            binding.llInformationNotes.root.visibility = View.GONE
        }

        getNotes()
        setStyleMap(mMap)
    }

    /**
     * Thiết lập các đánh dấu trên bản đồ từ danh sách ghi chú.
     * Trong quá trình này, ứng dụng sẽ tạo ra các marker tương ứng với mỗi ghi chú trong danh sách.
     * Các marker này sẽ được tạo dưới dạng ảnh vòng tròn với ảnh đại diện của người dùng và tiêu đề là ghi chú của họ.
     **/
    private fun setMarker() {
        // Kiểm tra nếu Activity vẫn còn tồn tại
        if (isDestroyed || isFinishing) {
            return
        }

        for (item in listNotes) {
            val location = LocationInfo(LatLng(item.lat!!, item.long!!), item)

            Glide.with(this@MapsActivity)
                .asBitmap()
                .load(location.info.photo)
                .transform(CircleTransform())
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        // Kiểm tra lại nếu Activity vẫn còn tồn tại trước khi thêm marker
                        if (isDestroyed || isFinishing) {
                            return
                        }

                        val marker = mMap.addMarker(
                            MarkerOptions().position(location.latLng)
                                .icon(BitmapDescriptorFactory.fromBitmap(resource))
                                .title(location.info.note)
                        )
                        if (marker != null) {
                            marker.tag = location.info
                            listMarkers.add(marker)
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // Implementation not needed
                    }
                })
        }
    }

    // Thiết lập kiểu dáng bản đồ
    private fun setStyleMap(mMap: GoogleMap) {
        var countStyle = 0
        binding.llChangeMap.setOnClickListener {
            when (countStyle) {
                0 -> {
                    mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
                    countStyle++
                }

                1 -> {
                    mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
                    countStyle = 0
                }
            }
        }
    }

    // Thiết lập Adapter cho RecyclerView hiển thị danh sách kết quả tìm kiếm
    private fun setAdapter() {
        adapter = ListAdapter(listResult)
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        adapter.clickItem = this
        binding.rcvListResult.layoutManager = LinearLayoutManager(this)
        binding.rcvListResult.adapter = adapter
        binding.rcvListResult.setMaxHeight(screenHeight / 2)
    }

    // Hiển thị thông tin chi tiết của một ghi chú khi được chọn trên bản đồ
    private fun setDataNotes(note: NotesModel) {
        binding.llInformationNotes.root.visibility = View.VISIBLE
        binding.llInformationNotes.tvUserName.text = note.userName
        binding.llInformationNotes.tvNote.text = note.note
        binding.llInformationNotes.tvAddress.text = note.address
    }

    // Tìm kiếm trong danh sách ghi chú và cập nhật RecyclerView với các kết quả phù hợp
    @SuppressLint("NotifyDataSetChanged")
    fun searchRecycleView(query: String) {
        if (query.isNotEmpty()) {
            val list = listNotes.filter { item ->
                VNCharacterUtils.removeAccent(item.userName?.lowercase(Locale.getDefault()))
                    .contains(
                        VNCharacterUtils.removeAccent(
                            query.lowercase(
                                Locale.getDefault()
                            )
                        )
                    ) || VNCharacterUtils.removeAccent(item.address?.lowercase(Locale.getDefault()))
                    .contains(
                        VNCharacterUtils.removeAccent(
                            query.lowercase(
                                Locale.getDefault()
                            )
                        )
                    )
                        || VNCharacterUtils.removeAccent(item.note?.lowercase(Locale.getDefault()))
                    .contains(
                        VNCharacterUtils.removeAccent(
                            query.lowercase(
                                Locale.getDefault()
                            )
                        )
                    )
            }
            listResult.clear()
            listResult.addAll(list)
            //
            if (listResult.isNotEmpty()) {
                adapter.notifyDataSetChanged()
                binding.rcvListResult.visibility = View.VISIBLE
                binding.tvEmpty.visibility = View.GONE
                binding.llListSearch.visibility = View.VISIBLE
            } else {
                binding.rcvListResult.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
                binding.llListSearch.visibility = View.VISIBLE
            }
        } else {
            //
            adapter.clearData()
            adapter.addData(listNotes)
            binding.rcvListResult.visibility = View.GONE
            binding.tvEmpty.visibility = View.VISIBLE
            binding.llListSearch.visibility = View.VISIBLE
        }

    }

    // Lấy thông tin người dùng từ Firebase và hiển thị ảnh đại diện của họ
    private fun showUser() {
        database = FirebaseDatabase.getInstance().getReference("Users")
        database.child(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                user = snapshot.getValue(UserModel::class.java)!!
                user.photo?.let { AppConstants.loadPhoto(binding.crAvatar, it) }
            }

            override fun onCancelled(error: DatabaseError) {
                AppConstants.setToast(
                    this@MapsActivity,
                    getString(R.string.fails),
                    error.message,
                    2
                )
            }
        })
    }

    // Lấy danh sách các ghi chú từ Firebase
    private fun getNotes() {
        database = FirebaseDatabase.getInstance().getReference("Notes")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (uidSnapshot in snapshot.children) {
                    for (idSnapshot in uidSnapshot.children) {
                        val notesItems = idSnapshot.getValue(NotesModel::class.java)
                        if (notesItems != null) {
                            if (!listNotes.contains(notesItems)) {
                                listNotes.add(notesItems)
                            }
                        }
                    }


                }
                setMarker()
            }

            override fun onCancelled(error: DatabaseError) {
                AppConstants.setToast(
                    this@MapsActivity,
                    getString(R.string.fails),
                    error.message,
                    2
                )
            }
        })

    }

    // Thêm một ghi chú mới vào cơ sở dữ liệu Firebase
    private fun upNotes(lat: Double, long: Double, note: String) {
        database = FirebaseDatabase.getInstance().getReference("Notes")
        lifecycleScope.launch {
            try {
                val uid = auth.currentUser!!.uid
                val id = database.push().key
                // Create the user model
                val address = getNameAddress(lat, long)
                val notePush = NotesModel(
                    id,
                    user.userName,
                    user.photo,
                    lat, long, note, address
                )

                //  Lưu ghi chú vào cơ sở dữ liệu
                database.child(uid).child(id.toString()).setValue(notePush).await()

                AppConstants.setToast(
                    this@MapsActivity,
                    getString(R.string.success),
                    getString(R.string.add_note_success),
                    1
                )
                if (!isShowPined) {
                    binding.imvMarker.setImageResource(R.drawable.ic_location_unpined)
                    setMarker()
                    isShowPined = true
                } else {
                    setMarker()
                }
            } catch (e: Exception) {
                AppConstants.setToast(
                    this@MapsActivity,
                    getString(R.string.fail_add_note),
                    e.message.toString(),
                    2
                )
            }
        }
    }

    // Di chuyển camera đến một vị trí cụ thể trên bản đồ
    private fun moveCamera(lat: Double, long: Double) {
        val latLng = LatLng(lat, long)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    // Thêm một đánh dấu mới vào bản đồ
    private fun addMarker(lat: Double, long: Double, title: String) {
        val location = LatLng(lat, long)
        currentMarker?.remove()
        currentMarker = mMap.addMarker(MarkerOptions().position(location).title(title))
        moveCamera(lat, long)
    }

    // Lấy địa chỉ từ tọa độ GPS
    private fun getNameAddress(lat: Double, long: Double): String {
        val geocoder = Geocoder(this@MapsActivity, Locale.getDefault())
        val addresses: List<Address>? = geocoder.getFromLocation(lat, long, 1)
        return addresses?.firstOrNull()?.getAddressLine(0) ?: getString(R.string.not_get_address)
    }

    private fun requestLocationPermissions() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Yêu cầu quyền truy cập vị trí từ người dùng
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                getLastKnownLocation()
            }

            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                getLastKnownLocation()
            }

            else -> {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
        }
    }

    // Lấy vị trí GPS hiện tại của thiết bị
    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation() {
        fusedLocationClient.getCurrentLocation(
            LocationRequest.PRIORITY_HIGH_ACCURACY,
            object : CancellationToken() {
                override fun onCanceledRequested(p0: OnTokenCanceledListener) =
                    CancellationTokenSource().token

                override fun isCancellationRequested() = false
            }).addOnSuccessListener { location ->
            if (location != null) {
                try {
                    mMap.isMyLocationEnabled = true
                    mMap.uiSettings.isMyLocationButtonEnabled = false
                    binding.imvLocation.setImageResource(R.drawable.ic_location)
                    lat = location.latitude
                    long = location.longitude
                    moveCamera(lat, long)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                AppConstants.setToast(
                    this,
                    getString(R.string.fails),
                    "Failed to get current location",
                    6
                )
            }
        }
    }

    // Xử lý sự kiện khi Activity bắt đầu và hiển thị người dùng hiện tại
    override fun onStart() {
        super.onStart()
        auth.currentUser?.let {
            showUser()
        }
    }

    // Xử lý sự kiện khi người dùng nhấp vào một mục trong RecyclerView
    override fun onClick(position: Int) {
        val item = listResult[position]
        moveCamera(item.lat!!, item.long!!)
        binding.llInformationNotes.root.visibility = View.VISIBLE
        binding.llListSearch.visibility = View.GONE
        ignoreTextChange = true
        binding.edtSearch.setQuery(item.note, false)
        ignoreTextChange = false
        setDataNotes(item)
    }
}


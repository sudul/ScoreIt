/*
 * Copyright 2020 Stéphane Baiget
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sbgapps.scoreit.app.ui.history.adapter

import android.annotation.SuppressLint
import androidx.annotation.CallSuper
import com.android.billingclient.api.SkuDetails
import com.sbgapps.scoreit.app.R
import com.sbgapps.scoreit.app.databinding.ListItemLapDonationBinding
import com.sbgapps.scoreit.app.model.DonationRow
import com.sbgapps.scoreit.core.widget.BaseViewHolder
import com.sbgapps.scoreit.data.repository.getBeerSku
import com.sbgapps.scoreit.data.repository.getCoffeeSku

class DonationAdapter(
    model: DonationRow,
    private val callback: (skuDetails: SkuDetails) -> Unit
) : BaseLapAdapter<DonationRow>(model, R.layout.list_item_lap_donation, {}) {

    @SuppressLint("SetTextI18n")
    @CallSuper
    override fun onBindViewHolder(viewHolder: BaseViewHolder) {
        val binding = ListItemLapDonationBinding.bind(viewHolder.itemView)
        model.skus.getCoffeeSku()?.let { sku ->
            binding.coffee.text = "${sku.description}\n${sku.price}"
            binding.coffee.setOnClickListener { callback(sku) }
        }
        model.skus.getBeerSku()?.let { sku ->
            binding.beer.text = "${sku.description}\n${sku.price}"
            binding.beer.setOnClickListener { callback(sku) }
        }
    }
}

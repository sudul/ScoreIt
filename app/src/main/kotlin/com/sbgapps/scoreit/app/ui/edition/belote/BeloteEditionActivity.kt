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

package com.sbgapps.scoreit.app.ui.edition.belote

import android.annotation.SuppressLint
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.sbgapps.scoreit.R
import com.sbgapps.scoreit.app.ui.edition.EditionActivity
import com.sbgapps.scoreit.app.ui.widget.AdaptableLinearLayoutAdapter
import com.sbgapps.scoreit.core.utils.string.build
import com.sbgapps.scoreit.data.model.BeloteBonusValue
import com.sbgapps.scoreit.data.model.PlayerPosition
import com.sbgapps.scoreit.databinding.ActivityEditionBeloteBinding
import com.sbgapps.scoreit.databinding.ListItemEditionBonusBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class BeloteEditionActivity : EditionActivity() {

    private val viewModel by viewModel<BeloteEditionViewModel>()
    private lateinit var binding: ActivityEditionBeloteBinding
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditionBeloteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar(binding.toolbar)

        viewModel.observeStates(this) { state ->
            when (state) {
                is BeloteEditionState.Content -> {
                    binding.buttonTeamOne.text = state.players[PlayerPosition.ONE.index].name
                    binding.buttonTeamTwo.text = state.players[PlayerPosition.TWO.index].name

                    binding.scorerGroup.removeOnButtonCheckedListener(scorerCheckedListener)
                    when (state.taker) {
                        PlayerPosition.ONE -> binding.scorerGroup.check(R.id.buttonTeamOne)
                        PlayerPosition.TWO -> binding.scorerGroup.check(R.id.buttonTeamTwo)
                        else -> error("Only two players for Belote")
                    }
                    binding.scorerGroup.addOnButtonCheckedListener(scorerCheckedListener)

                    binding.lapInfo.text = state.lapInfo.build(this)

                    bindButton(binding.pointsPlusTen, 10, state.stepPointsByTen.canAdd)
                    bindButton(binding.pointsMinusTen, -10, state.stepPointsByTen.canSubtract)
                    bindButton(binding.pointsPlusOne, 1, state.stepPointsByOne.canAdd)
                    bindButton(binding.pointsMinusOne, -1, state.stepPointsByOne.canSubtract)

                    Timber.d("Team points are ${state.results}")
                    val (teamOne, teamTwo) = state.results
                    binding.pointsTeamOne.text = teamOne
                    binding.pointsTeamTwo.text = teamTwo

                    binding.addBonus.isVisible = state.availableBonuses.isNotEmpty()
                    binding.addBonus.setOnClickListener {
                        BeloteEditionBonusFragment().show(supportFragmentManager, null)
                    }
                    val model = state.selectedBonuses.map { (player, bonus) ->
                        state.players[player.index].name to bonus
                    }
                    binding.bonusContainer.adapter = BonusAdapter(model)
                }

                is BeloteEditionState.Completed -> {
                    // Handle the completed state
                    finish()
                }
            }
        }

        // Initialize the OnBackPressedCallback
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Handle the back button event
                viewModel.cancelEdition()
            }
        }

        // Add the callback to the dispatcher
        this.onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        viewModel.loadContent()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.done -> {
            viewModel.completeEdition()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private val scorerCheckedListener = MaterialButtonToggleGroup.OnButtonCheckedListener { _, checkedId, isChecked ->
        if (isChecked) {
            viewModel.changeTaker(
                when (checkedId) {
                    R.id.buttonTeamOne -> PlayerPosition.ONE
                    R.id.buttonTeamTwo -> PlayerPosition.TWO
                    else -> error("Unknown player")
                }
            )
        }
    }

    private fun bindButton(button: MaterialButton, increment: Int, enabled: Boolean) {
        button.apply {
            isEnabled = enabled
            setOnClickListener {
                viewModel.incrementScore(increment)
            }
        }
    }

    inner class BonusAdapter(val model: List<Pair<String, BeloteBonusValue>>) : AdaptableLinearLayoutAdapter {

        @SuppressLint("SetTextI18n")
        override fun getView(position: Int, parent: ViewGroup): View {
            val view = ListItemEditionBonusBinding.inflate(layoutInflater)
            view.bonus.text = "${model[position].first} • ${getString(model[position].second.resId)}"
            view.remove.setOnClickListener {
                viewModel.removeBonus(position)
            }
            return view.root
        }

        override fun getCount(): Int = model.size
    }
}

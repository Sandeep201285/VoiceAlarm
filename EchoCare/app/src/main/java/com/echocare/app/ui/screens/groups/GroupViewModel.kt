package com.echocare.app.ui.screens.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echocare.app.data.repository.GroupRepository
import com.echocare.app.domain.model.Group
import com.echocare.app.domain.model.GroupMember
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupsUiState(
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository
) : ViewModel() {

    val uiState: StateFlow<GroupsUiState> = groupRepository.getAllGroups()
        .map { list -> GroupsUiState(groups = list, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GroupsUiState()
        )

    fun createGroup(name: String, guestMembers: List<Pair<String, String>>) {
        viewModelScope.launch {
            // Group ownerId is hardcoded "me" or derived from credentials
            val groupMembers = guestMembers.map { (mName, mPhone) ->
                GroupMember(
                    groupId = "",
                    userId = "",
                    name = mName,
                    phone = mPhone,
                    role = "MEMBER",
                    status = "PENDING"
                )
            }
            groupRepository.createGroup(name = name, ownerId = "Sandeep", members = groupMembers)
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            groupRepository.deleteGroup(groupId)
        }
    }

    fun getMembers(groupId: String): Flow<List<GroupMember>> =
        groupRepository.getMembersForGroup(groupId)

    fun updateStatus(groupId: String, userId: String, status: String) {
        viewModelScope.launch {
            groupRepository.updateMemberStatus(groupId, userId, status)
        }
    }
}

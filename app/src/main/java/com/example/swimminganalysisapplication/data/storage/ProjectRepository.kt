package com.example.swimminganalysisapplication.data.storage

import kotlinx.coroutines.flow.Flow

/**
 * Repository for Project data operations.
 */
class ProjectRepository(private val projectDao: ProjectDao) {

    fun getAllProjects(): Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun getProjectById(projectId: Int): ProjectEntity? = projectDao.getProjectById(projectId)

    suspend fun insertProject(project: ProjectEntity): Long = projectDao.insertProject(project)

    suspend fun updateProject(project: ProjectEntity) = projectDao.updateProject(project)

    suspend fun deleteProject(project: ProjectEntity) = projectDao.deleteProject(project)

    suspend fun deleteProjectById(projectId: Int) = projectDao.deleteProjectById(projectId)
}

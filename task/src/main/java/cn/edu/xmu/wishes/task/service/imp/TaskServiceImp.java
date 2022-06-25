package cn.edu.xmu.wishes.task.service.imp;

import cn.edu.xmu.wishes.core.util.Common;
import cn.edu.xmu.wishes.core.util.ReturnNo;
import cn.edu.xmu.wishes.core.util.ReturnObject;
import cn.edu.xmu.wishes.task.mapper.TaskMapper;
import cn.edu.xmu.wishes.task.model.po.Task;
import cn.edu.xmu.wishes.task.model.vo.TaskRetVo;
import cn.edu.xmu.wishes.task.model.vo.TaskVo;
import cn.edu.xmu.wishes.task.service.TaskService;
import cn.edu.xmu.wishes.task.service.TaskTypeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service("taskService")
public class TaskServiceImp extends ServiceImpl<TaskMapper, Task> implements TaskService {

    @Autowired
    private TaskTypeService taskTypeService;

    private final String taskCacheKey = "task#3600";

    private LambdaQueryWrapper<Task> getQueryWrapperByTask(Task exampleTask) {
        LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<>();
        Optional.ofNullable(exampleTask.getInitiatorId()).ifPresent(x -> queryWrapper.eq(Task::getInitiatorId, x));
        Optional.ofNullable(exampleTask.getReceiverId()).ifPresent(x -> queryWrapper.eq(Task::getReceiverId, x));
        Optional.ofNullable(exampleTask.getTypeId()).ifPresent(x -> queryWrapper.eq(Task::getTypeId, x));
        Optional.ofNullable(exampleTask.getState()).ifPresent(x -> queryWrapper.eq(Task::getState, x));

        return queryWrapper;
    }
    public ReturnObject listTaskByExampleAndPage(Task exampleTask, Integer page, Integer pageSize) {
        // get data
        Page<Task> taskPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<Task> queryWrapper = getQueryWrapperByTask(exampleTask);
        Page<Task> tasks = this.baseMapper.selectPage(taskPage, queryWrapper);

        // transform data to vo
        Page<TaskRetVo> taskRetVoPage = Common.transformPageVo(tasks, TaskRetVo.class);
        List<TaskRetVo> taskRetVos = taskRetVoPage.getRecords();
        TaskRetVo taskRetVo;
        for(int i=0;i<taskRetVos.size();i++) {
            taskRetVo = taskRetVos.get(i);
            taskRetVo.setType(taskTypeService.getTypeName(tasks.getRecords().get(i).getTypeId()));
        }
        return new ReturnObject(taskRetVoPage);
    }

    public ReturnObject listTaskByExample(Task exampleTask) {
        LambdaQueryWrapper<Task> queryWrapper = getQueryWrapperByTask(exampleTask);
        List<Task> taskList = this.list(queryWrapper);
        List<TaskRetVo> taskRetVoList = Common.transformListVo(taskList, TaskRetVo.class);

        TaskRetVo taskRetVo;
        for(int i=0;i<taskRetVoList.size();i++) {
            taskRetVo = taskRetVoList.get(i);
            taskRetVo.setType(taskTypeService.getTypeName(taskList.get(i).getTypeId()));
        }
        return new ReturnObject(taskRetVoList);
    }

    @Override
    public ReturnObject listTask(Long initiatorId, Long receiverId, Long typeId, Integer page, Integer pageSize) {
        Task exampleTask = Task.builder()
                .initiatorId(initiatorId)
                .receiverId(receiverId)
                .typeId(typeId)
                .state(Task.StateType.NOT_ACCEPTED)
                .build();

        return listTaskByExampleAndPage(exampleTask, page, pageSize);
    }

    @Override
    @Cacheable(cacheNames = taskCacheKey, key = "#id")
    public ReturnObject<TaskRetVo> getTaskById(Long id) {
        Task task = this.getById(id);
        if (task == null) {
            return new ReturnObject<>(ReturnNo.RESOURCE_ID_NOTEXIST);
        } else {
            TaskRetVo taskRetVo = Common.cloneVo(task, TaskRetVo.class);
            taskRetVo.setType(taskTypeService.getTypeName(task.getTypeId()));
            return new ReturnObject<>(taskRetVo);
        }
    }

    @Override
    public ReturnObject saveOrUpdateTask(Long userId, Long taskId, TaskVo taskVo) {
        Task task = new Task();
        BeanUtils.copyProperties(taskVo, task);
        task.setId(taskId);
        task.setInitiatorId(userId);
        if (this.saveOrUpdate(task)) {
            return new ReturnObject();
        } else {
            return ReturnObject.INTERNAL_SERVER_ERR_RET;
        }
    }

    @Override
    public ReturnObject acceptTask(Long taskId, Long userId) {
        LambdaUpdateWrapper<Task> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper
                .eq(Task::getId, taskId)
                .eq(Task::getState, Task.StateType.NOT_ACCEPTED)
                .set(Task::getState, Task.StateType.ACCEPTED)
                .set(Task::getReceiverId, userId);
        boolean isUpdated = this.update(updateWrapper);
        if (isUpdated) {
            return ReturnObject.OK_RET;
        } else {
            Task task = this.getById(taskId);
            if (task == null) {
                return ReturnObject.RESOURCE_ID_NOTEXIST_RET;
            }
            return new ReturnObject(ReturnNo.AUTH_NO_RIGHT, "任务已被接取");
        }
    }

    @Override
    public ReturnObject getUserAcceptedTask(Long userId, Long typeId) {
        Task exampleTask = Task.builder()
                .receiverId(userId)
                .typeId(typeId)
                .build();

        return listTaskByExample(exampleTask);
    }


    @Override
    public ReturnObject getUserPublishedTask(Long userId, Long typeId) {
        Task exampleTask = Task.builder()
                .initiatorId(userId)
                .typeId(typeId)
                .build();

        return listTaskByExample(exampleTask);
    }

    @Override
    public ReturnObject deleteUserTask(Long userId, Long taskId) {
        LambdaQueryWrapper<Task> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .eq(Task::getId, taskId)
                .eq(Task::getInitiatorId, userId);
        boolean isRemove = this.remove(lambdaQueryWrapper);
        if (isRemove) {
            return ReturnObject.OK_RET;
        } else {
            return new ReturnObject(ReturnNo.RESOURCE_ID_OUTSCOPE, "该任务不属于你或不存在");
        }
    }

    @Override
    public ReturnObject cancelAcceptTask(Long userId, Long taskId) {
        LambdaUpdateWrapper<Task> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper
                .eq(Task::getId, taskId)
                .eq(Task::getReceiverId, userId)
                .set(Task::getReceiverId, null)
                .set(Task::getState, Task.StateType.NOT_ACCEPTED);

        boolean isUpdate = this.update(lambdaUpdateWrapper);
        if (isUpdate) {
            return ReturnObject.OK_RET;
        } else {
            return new ReturnObject(ReturnNo.RESOURCE_ID_OUTSCOPE, "该任务不属于你或不存在");
        }
    }

    @Override
    public ReturnObject finishTask(Long userId, Long taskId) {
        LambdaUpdateWrapper<Task> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper
                .eq(Task::getId, taskId)
                .eq(Task::getReceiverId, userId)
                .set(Task::getReceiverId, null)
                .set(Task::getState, Task.StateType.FINISHED);

        boolean isUpdate = this.update(lambdaUpdateWrapper);
        if (isUpdate) {
            return ReturnObject.OK_RET;
        } else {
            return new ReturnObject(ReturnNo.RESOURCE_ID_OUTSCOPE, "该任务不属于你或不存在");
        }
    }
}

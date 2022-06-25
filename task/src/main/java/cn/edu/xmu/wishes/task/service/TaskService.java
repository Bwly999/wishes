package cn.edu.xmu.wishes.task.service;

import cn.edu.xmu.wishes.core.util.ReturnObject;
import cn.edu.xmu.wishes.task.model.po.Task;
import cn.edu.xmu.wishes.task.model.vo.TaskRetVo;
import cn.edu.xmu.wishes.task.model.vo.TaskVo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface TaskService extends IService<Task> {
    ReturnObject listTask(Long initiatorId, Long receiverId, Long typeId, Integer page, Integer pageSize);

    ReturnObject<TaskRetVo> getTaskById(Long id);

    ReturnObject saveOrUpdateTask(Long userId, Long taskId, TaskVo taskVo);

    ReturnObject acceptTask(Long taskId, Long userId);

    ReturnObject getUserAcceptedTask(Long userId, Long typeId);

    ReturnObject getUserPublishedTask(Long userId, Long typeId);

    ReturnObject deleteUserTask(Long userId, Long taskId);

    ReturnObject cancelAcceptTask(Long userId, Long taskId);

    ReturnObject finishTask(Long userId, Long taskId);
}

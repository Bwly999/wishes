package cn.edu.xmu.wishes.task.service.imp;

import cn.edu.xmu.wishes.core.util.Common;
import cn.edu.xmu.wishes.core.util.RedisUtil;
import cn.edu.xmu.wishes.core.util.ReturnNo;
import cn.edu.xmu.wishes.core.util.ReturnObject;
import cn.edu.xmu.wishes.core.util.storage.StorageUtil;
import cn.edu.xmu.wishes.task.mapper.TaskDraftMapper;
import cn.edu.xmu.wishes.task.model.po.TaskDraft;
import cn.edu.xmu.wishes.task.model.vo.TaskDraftRetVo;
import cn.edu.xmu.wishes.task.model.vo.TaskDraftVo;
import cn.edu.xmu.wishes.task.service.TaskDraftService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service("taskDraftService")
public class TaskDraftServiceImp extends ServiceImpl<TaskDraftMapper, TaskDraft> implements TaskDraftService {
    @Autowired
    private StorageUtil storageUtil;

    @Autowired
    private RedisUtil redisUtil;

    private final String taskDraftCacheKey = "task.draft#3600";

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = taskDraftCacheKey, key = "#result.data.id")
    public ReturnObject insertTaskDraft(TaskDraftVo taskDraftVo) {
        try {
            TaskDraft taskDraft = new TaskDraft();
            BeanUtils.copyProperties(taskDraftVo, taskDraft, TaskDraft.class);

            save(taskDraft);
            return new ReturnObject(taskDraft);
        }
        catch (Exception e) {
            log.error("createTaskDraft" + e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    @Cacheable(cacheNames = taskDraftCacheKey, key = "#id")
    public ReturnObject getTaskDraftById(Long id) {
        try {
            TaskDraft taskDraft = getById(id);
            if (taskDraft == null) {
                return new ReturnObject(ReturnNo.RESOURCE_ID_NOTEXIST);
            }
            return new ReturnObject(taskDraft);
        }
        catch (Exception e) {
            log.error("getTaskDraftById" + e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnObject uploadTaskImage(Long id, List<MultipartFile> files) {
        try {
            TaskDraft taskDraft = getById(id);
            if (taskDraft == null) {
                return new ReturnObject(ReturnNo.RESOURCE_ID_NOTEXIST);
            }

            String imageUrls = taskDraft.getImageUrl();
            List<String> fileUrlList;
            if (imageUrls != null) {
                String[] split = imageUrls.split(",");
                fileUrlList = new ArrayList<>(split.length + files.size());
                Collections.addAll(fileUrlList, split);
            }
            else {
                fileUrlList = new ArrayList<>(files.size());
            }

            // 存储图片并将url放入imaUrls中
            String url;
            for (MultipartFile file : files) {
                url = storageUtil.storeImg(file.getInputStream(), file.getOriginalFilename());
                if (url == null) {
                    return new ReturnObject(ReturnNo.INTERNAL_SERVER_ERR);
                }
                else {
                    fileUrlList.add(url);
                }
            }
            String newImageUrls = Common.concatString(",", fileUrlList).toString();
            taskDraft.setImageUrl(newImageUrls);
            updateById(taskDraft);
            return new ReturnObject();
        }
        catch (Exception e) {
            log.error("uploadTaskImage" + e.getMessage());
            throw (RuntimeException)e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = taskDraftCacheKey, key = "#id")
    public ReturnObject updateTaskDraft(Long id, TaskDraftVo taskDraftVo) {
        try {
            TaskDraft taskDraft = new TaskDraft();
            BeanUtils.copyProperties(taskDraftVo, taskDraft);
            taskDraft.setId(id);
            this.updateById(taskDraft);
            return new ReturnObject(Common.cloneVo(taskDraft, TaskDraftRetVo.class));
        }
        catch (Exception e) {
            log.error("updateTaskDraft" + e.getMessage());
            throw e;
        }
    }

}

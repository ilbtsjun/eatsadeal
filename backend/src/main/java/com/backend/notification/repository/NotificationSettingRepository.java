package com.backend.notification.repository;

import com.backend.notification.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

}

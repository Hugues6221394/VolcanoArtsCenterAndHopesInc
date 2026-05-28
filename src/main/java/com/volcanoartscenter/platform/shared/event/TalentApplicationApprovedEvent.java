package com.volcanoartscenter.platform.shared.event;

import com.volcanoartscenter.platform.shared.model.TalentApplication;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TalentApplicationApprovedEvent extends ApplicationEvent {

    private final TalentApplication application;

    public TalentApplicationApprovedEvent(Object source, TalentApplication application) {
        super(source);
        this.application = application;
    }
}

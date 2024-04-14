package com.walking.api.config;

import com.walking.api.data.DataConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({DataConfig.class})
public class ApiDataConfig {}

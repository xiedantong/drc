package com.ctrip.framework.drc.console.controller.v2;


import com.ctrip.framework.drc.console.service.v2.MetaCompareService;
import com.ctrip.framework.drc.console.service.v2.impl.MetaGeneratorV3;
import com.ctrip.framework.drc.console.service.v2.impl.MetaGeneratorV4;
import com.ctrip.framework.drc.console.service.v2.impl.MetaGeneratorV5;
import com.ctrip.framework.drc.console.utils.XmlUtils;
import com.ctrip.framework.drc.core.entity.Drc;
import com.ctrip.framework.drc.core.http.ApiResult;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/drc/v2/generator/")
public class DrcGeneratorController {
    private static final Logger logger = LoggerFactory.getLogger(DrcGeneratorController.class);

    @Autowired
    private MetaGeneratorV5 metaGeneratorV5;
    @Autowired
    private MetaGeneratorV4 metaGeneratorV4;
    @Autowired
    private MetaGeneratorV3 metaGeneratorV3;
    @Autowired
    private MetaCompareService metaCompareService;

    @GetMapping("doBenchMark")
    public ApiResult<GeneratorStatistics.Task> benchMarkTest(@RequestParam("compare") Boolean compare) {
        try {
            List<GeneratorStatistics.Task> list = Lists.newArrayList();
            StopWatch stopWatch = new StopWatch();

            // v3
            logger.info("start v3");
            stopWatch.start(metaGeneratorV3.getClass().getSimpleName());
            Drc drcV3 = metaGeneratorV3.getDrc();
            stopWatch.stop();
            list.add(new GeneratorStatistics.Task(stopWatch.getLastTaskName(), stopWatch.getLastTaskTimeMillis()));


            // v4
            logger.info("start v4");
            stopWatch.start(metaGeneratorV4.getClass().getSimpleName());
            Drc drcV4 = metaGeneratorV4.getDrc();
            stopWatch.stop();
            list.add(new GeneratorStatistics.Task(stopWatch.getLastTaskName(), stopWatch.getLastTaskTimeMillis()));


            // v5
            logger.info("start v5");
            stopWatch.start(metaGeneratorV5.getClass().getSimpleName());
            Drc drcV5 = metaGeneratorV5.getDrc();
            stopWatch.stop();
            list.add(new GeneratorStatistics.Task(stopWatch.getLastTaskName(), stopWatch.getLastTaskTimeMillis()));



            // compareResult
            String summaryInfo = stopWatch.prettyPrint();
            List<Boolean> compareResult = Lists.newArrayList();
            if (Boolean.TRUE.equals(compare)) {
                stopWatch.start("compare v3, v4");
                String res = metaCompareService.compareDrcMeta(drcV3, drcV4);
                stopWatch.stop();
                list.add(new GeneratorStatistics.Task(stopWatch.getLastTaskName(), stopWatch.getLastTaskTimeMillis()));

                if (!StringUtils.isEmpty(res)) {
                    summaryInfo += String.format("\n-----Compare (v3,v4)Result------\n\n%s\n\n--------------------\n", res);
                }
                compareResult.add(metaCompareService.isConsistent(res));

                stopWatch.start("compare v4, v5");
                String res2 = metaCompareService.compareDrcMeta(drcV4, drcV5);
                stopWatch.stop();
                list.add(new GeneratorStatistics.Task(stopWatch.getLastTaskName(), stopWatch.getLastTaskTimeMillis()));

                if (!StringUtils.isEmpty(res2)) {
                    summaryInfo += String.format("\n-----Compare (v4,v5)Result------\n\n%s\n\n--------------------\n", res2);
                }
                compareResult.add(metaCompareService.isConsistent(res2));
            }
            logger.info(summaryInfo);
            return ApiResult.getSuccessInstance(new GeneratorStatistics(list, compareResult, summaryInfo));
        } catch (Throwable e) {
            logger.error("benchMarkTest error", e);
            return ApiResult.getFailInstance(null, e.getMessage());
        }
    }


    @GetMapping("generateV3")
    public String generateV3() {
        try {
            // v3
            logger.info("start v3");
            Drc drcV3 = metaGeneratorV3.getDrc();
            logger.info("finish v3");
            return XmlUtils.formatXML(drcV3.toString());
        } catch (Throwable e) {
            logger.error("benchMarkTest error", e);
            return  e.getMessage();
        }
    }

    @GetMapping("generateV4")
    public String generatev4() {
        try {
            // v4
            logger.info("start v4");
            Drc drcv4 = metaGeneratorV4.getDrc();
            logger.info("finish v4");
            return XmlUtils.formatXML(drcv4.toString());
        } catch (Throwable e) {
            logger.error("benchMarkTest error", e);
            return  e.getMessage();
        }
    }

    @GetMapping("generateV5")
    public String generateV5() {
        try {
            // v5
            logger.info("start v5");
            Drc drcv5 = metaGeneratorV5.getDrc();
            logger.info("finish v5");
            return XmlUtils.formatXML(drcv5.toString());
        } catch (Throwable e) {
            logger.error("benchMarkTest error", e);
            return  e.getMessage();
        }
    }


    static class GeneratorStatistics {
        private final List<Task> task;
        private final List<Boolean> same;
        private final String summaryInfo;

        public GeneratorStatistics(List<Task> task, List<Boolean> same, String summaryInfo) {
            this.task = task;
            this.same = same;
            this.summaryInfo = summaryInfo;
        }

        public List<Task> getTask() {
            return task;
        }

        public String getSummaryInfo() {
            return summaryInfo;
        }

        public List<Boolean> getSame() {
            return same;
        }

        public static class Task {
            private final String name;
            private final Long seconds;


            public Task(String name, Long seconds) {
                this.name = name;
                this.seconds = seconds;
            }

            public String getName() {
                return name;
            }

            public Long getSeconds() {
                return seconds;
            }

        }

    }


}

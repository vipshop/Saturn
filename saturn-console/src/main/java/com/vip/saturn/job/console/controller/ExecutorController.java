package com.vip.saturn.job.console.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.vip.saturn.job.console.domain.JobBriefInfo;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.ExecutorService;
import com.vip.saturn.job.console.service.JobDimensionService;
import com.vip.saturn.job.console.utils.CronExpression;

import jxl.Cell;
import jxl.CellType;
import jxl.Sheet;
import jxl.Workbook;
import jxl.write.WriteException;

/**
 * @author xiaopeng.he
 */
@RestController
@RequestMapping("executor")
public class ExecutorController extends AbstractController {

	protected static Logger log = LoggerFactory.getLogger(ExecutorController.class);

	@Resource
	private ExecutorService executorService;
    @Resource
    private JobDimensionService jobDimensionService;

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

	@RequestMapping(value = "checkAndAddJobs", method = RequestMethod.POST)
	public RequestResult checkAndAddJobs(JobConfig jobConfig, HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		try {
			checkJobConfig(jobConfig);
			requestResult = executorService.addJobs(jobConfig);
		} catch (SaturnJobConsoleException e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.getMessage());
		} catch (Throwable t) {
			requestResult.setSuccess(false);
			requestResult.setMessage(t.toString());
			log.error("checkAndAddJobs exception:", t);
		}
		return requestResult;
	}

	private void checkJobConfig(JobConfig jobConfig) throws SaturnJobConsoleException {
		// 作业名必填
		if (jobConfig.getJobName() == null || jobConfig.getJobName().trim().isEmpty()) {
			throw new SaturnJobConsoleException("作业名必填");
		}
		// 作业类型必填
		if (jobConfig.getJobType() == null || jobConfig.getJobType().trim().isEmpty()) {
			throw new SaturnJobConsoleException("作业类型必填");
		}
		// 验证作业类型
		if (JobBriefInfo.JobType.getJobType(jobConfig.getJobType()).equals(JobBriefInfo.JobType.UNKOWN_JOB)) {
			throw new SaturnJobConsoleException("作业类型未知");
		}
		// 如果是JAVA/MSG作业
		if (jobConfig.getJobType().equals(JobBriefInfo.JobType.JAVA_JOB.name())
				|| jobConfig.getJobType().equals(JobBriefInfo.JobType.MSG_JOB.name())) {
			// 作业实现类必填
			if (jobConfig.getJobClass() == null || jobConfig.getJobClass().trim().isEmpty()) {
				throw new SaturnJobConsoleException("对于JAVA/MSG作业，作业实现类必填");
			}
		}
		// 如果是JAVA/SHELL作业
		if (jobConfig.getJobType().equals(JobBriefInfo.JobType.JAVA_JOB.name())
				|| jobConfig.getJobType().equals(JobBriefInfo.JobType.SHELL_JOB.name())) {
			// cron表达式必填
			if (jobConfig.getCron() == null || jobConfig.getCron().trim().isEmpty()) {
				throw new SaturnJobConsoleException("对于JAVA/SHELL作业，cron表达式必填");
			}
			// cron表达式语法验证
			try {
				CronExpression.validateExpression(jobConfig.getCron());
			} catch (ParseException e) {
				throw new SaturnJobConsoleException("cron表达式语法有误，" + e.toString());
			}
		} else {
        	jobConfig.setCron("");;// 其他类型的不需要持久化保存cron表达式
        }
		if (jobConfig.getLocalMode() != null && jobConfig.getLocalMode()) {
			if (jobConfig.getShardingItemParameters() == null) {
				throw new SaturnJobConsoleException("对于本地模式作业，分片参数必填。");
			} else {
				String[] split = jobConfig.getShardingItemParameters().split(",");
				boolean includeXing = false;
				for (String tmp : split) {
					String[] split2 = tmp.split("=");
					if ("*".equalsIgnoreCase(split2[0].trim())) {
						includeXing = true;
						break;
					}
				}
				if (!includeXing) {
					throw new SaturnJobConsoleException("对于本地模式作业，分片参数必须包含如*=xx。");
				}
			}
		} else {
			// 分片参数不能小于分片总数
			if (jobConfig.getShardingTotalCount() == null || jobConfig.getShardingTotalCount() < 1) {
				throw new SaturnJobConsoleException("分片数不能为空，并且不能小于1");
			}
			if (jobConfig.getShardingTotalCount() > 0) {
				if (jobConfig.getShardingItemParameters() == null
						|| jobConfig.getShardingItemParameters().trim().isEmpty()
						|| jobConfig.getShardingItemParameters().split(",").length < jobConfig
								.getShardingTotalCount()) {
					throw new SaturnJobConsoleException("分片参数不能小于分片总数");
				}
			}
		}
	}

	@RequestMapping(value = "batchAddJobs", method = RequestMethod.POST)
	public RequestResult batchAddJobs(MultipartHttpServletRequest request) {
		RequestResult result = new RequestResult();
		int successCount = 0;
		int failCount = 0;
		String failMessage = "";
		try {
			Iterator<String> fileNames = request.getFileNames();
			MultipartFile file = null;
			while (fileNames.hasNext()) {
				if (file != null) {
					result.setSuccess(false);
					result.setMessage("仅支持单文件导入");
					return result;
				}
				file = request.getFile(fileNames.next());
			}
			if (file == null) {
				result.setSuccess(false);
				result.setMessage("请选择导入的文件");
				return result;
			}
			String originalFilename = file.getOriginalFilename();
			if(originalFilename == null || !originalFilename.endsWith(".xls")) {
				result.setSuccess(false);
				result.setMessage("仅支持.xls文件导入");
				return result;
			}
			Workbook workbook = Workbook.getWorkbook(file.getInputStream());
			Sheet[] sheets = workbook.getSheets();
			List<JobConfig> jobConfigList = new ArrayList<>();
			// 第一行为配置项提示，从第二行开始为作业配置信息
			// 先获取数据并检测内容格式的正确性
			for (int i = 0; i < sheets.length; i++) {
				Sheet sheet = sheets[i];
				int rows = sheet.getRows();
				for (int row = 1; row < rows; row++) {
					Cell[] rowCells = sheet.getRow(row);
					// 如果这一行的表格全为空，则跳过这一行。
					if(!isBlankRow(rowCells)) {
						jobConfigList.add(convertJobConfig(i + 1, row + 1, rowCells));
					}
				}
			}
			// 再进行添加
			for (JobConfig jobConfig : jobConfigList) {
				RequestResult addJobResult = executorService.addJobs(jobConfig);
				if (addJobResult.isSuccess()) {
					successCount++;
				} else {
					failCount++;
					failMessage += " [" + addJobResult.getMessage() + "]";
				}
			}
		} catch (SaturnJobConsoleException e) {
			result.setSuccess(false);
			result.setMessage("导入失败，" + e.getMessage());
			return result;
		} catch (Exception e) {
			result.setSuccess(false);
			result.setMessage("导入失败，错误信息：" + e.toString());
			return result;
		}
		result.setSuccess(true);
		if (failCount > 0) {
			result.setMessage("共导入" + successCount + "个作业，忽略" + failCount + "个。错误信息：" + failMessage);
		} else {
			result.setMessage("共导入" + successCount + "个作业，忽略0个");
		}
		return result;
	}

	private boolean isBlankRow(Cell[] rowCells) {
		for(int i=0; i<rowCells.length; i++) {
			if(!CellType.EMPTY.equals(rowCells[i].getType())) {
				return false;
			}
		}
		return true;
	}

	private String createExceptionMessage(int sheetNumber, int rowNumber, int columnNumber, String message) {
		return "内容格式有误，错误发生在表格页:" + sheetNumber + "，行号:" + rowNumber + "，列号:" + columnNumber + "，错误信息：" + message;
	}

	private JobConfig convertJobConfig(int sheetNumber, int rowNumber, Cell[] rowCells) throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();

		String jobName = getContents(rowCells, 0);
		if (jobName == null || jobName.trim().isEmpty()) {
			throw new SaturnJobConsoleException(createExceptionMessage(sheetNumber, rowNumber, 1, "作业名必填。"));
		}
		jobConfig.setJobName(jobName);

		String jobType = getContents(rowCells, 1);
		if (jobType == null || jobType.trim().isEmpty()) {
			throw new SaturnJobConsoleException(createExceptionMessage(sheetNumber, rowNumber, 2, "作业类型必填。"));
		}
		if (JobBriefInfo.JobType.getJobType(jobType).equals(JobBriefInfo.JobType.UNKOWN_JOB)) {
			throw new SaturnJobConsoleException(createExceptionMessage(sheetNumber, rowNumber, 2, "作业类型未知。"));
		}
		if (JobBriefInfo.JobType.getJobType(jobType).equals(JobBriefInfo.JobType.VSHELL) && jobDimensionService.isNewSaturn("1.1.2") != 2) {
			throw new SaturnJobConsoleException(createExceptionMessage(sheetNumber, rowNumber, 2, "Shell消息作业不能导入到包含1.1.2以下版本Executor所在的域。"));
		}
		jobConfig.setJobType(jobType);

		String jobClass = getContents(rowCells, 2);
		if (jobType.equals(JobBriefInfo.JobType.JAVA_JOB.name()) || jobType.equals(JobBriefInfo.JobType.MSG_JOB.name())) {
			if (jobClass == null || jobClass.trim().isEmpty()) {
				throw new SaturnJobConsoleException(createExceptionMessage(sheetNumber, rowNumber, 3, "对于JAVA/MSG作业，作业实现类必填。"));
			}
		}
		jobConfig.setJobClass(jobClass);

		String cron = getContents(rowCells, 3);
		if (jobType.equals(JobBriefInfo.JobType.JAVA_JOB.name()) || jobType.equals(JobBriefInfo.JobType.SHELL_JOB.name())) {
			if (cron == null || cron.trim().isEmpty()) {
				throw new SaturnJobConsoleException(createExceptionMessage(sheetNumber, rowNumber, 4, "对于JAVA/SHELL作业，cron表达式必填。"));
			}
			try {
				CronExpression.validateExpression(cron);
			} catch (ParseException e) {
				throw new SaturnJobConsoleException(createExceptionMessage(sheetNumber, rowNumber, 4, "cron表达式语法有误，" + e.toString()));
			}
		} else {
			cron = "";// 其他类型的不需要持久化保存cron表达式
		}

		jobConfig.setCron(cron);

		jobConfig.setDescription(getContents(rowCells, 4));

		jobConfig.setLocalMode(Boolean.valueOf(getContents(rowCells, 5)));

		int shardingTotalCount = 1;
		if(jobConfig.getLocalMode()) {
			jobConfig.setShardingTotalCount(shardingTotalCount);
		} else {
			String tmp = getContents(rowCells, 6);
			if (tmp != null) {
				try {
					shardingTotalCount = Integer.parseInt(tmp);
				} catch (NumberFormatException e) {
					throw new SaturnJobConsoleException(createExceptionMessage(sheetNumber, rowNumber, 7, "分片数有误，" + e.toString()));
				}
			} else {
				throw new SaturnJobConsoleException(createExceptionMessage(sheetNumber, rowNumber, 7, "分片数必填"));
			}
			if(shardingTotalCount < 1) {
				throw new SaturnJobConsoleException(createExceptionMessage(sheetNumber, rowNumber, 7, "分片数不能小于1"));
			}
			jobConfig.setShardingTotalCount(shardingTotalCount);
		}

		int timeoutSeconds = 0;
		try {
			String tmp = getContents(rowCells, 7);
			if (tmp != null && !tmp.trim().isEmpty()) {
				timeoutSeconds = Integer.parseInt(tmp.trim());
			}
		} catch (NumberFormatException e) {
			throw new SaturnJobConsoleException(createExceptionMessage(sheetNumber, rowNumber, 8, "超时时间有误，" + e.toString()));
		}
		jobConfig.setTimeoutSeconds(timeoutSeconds);

		jobConfig.setJobParameter(getContents(rowCells, 8));

		String shardingItemParameters = getContents(rowCells, 9);
		if(jobConfig.getLocalMode()) {
			if(shardingItemParameters == null) {
				throw new SaturnJobConsoleException(createExceptionMessage(sheetNumber, rowNumber, 10, "对于本地模式作业，分片参数必填。"));
			} else {
				String[] split = shardingItemParameters.split(",");
				boolean includeXing = false;
				for(String tmp : split) {
					String[] split2 = tmp.split("=");
					if("*".equalsIgnoreCase(split2[0].trim())) {
						includeXing = true;
						break;
					}
				}
				if(!includeXing) {
					throw new SaturnJobConsoleException(createExceptionMessage(sheetNumber, rowNumber, 10, "对于本地模式作业，分片参数必须包含如*=xx。"));
				}
			}
		} else if (shardingTotalCount > 0) {
			if (shardingItemParameters == null ||  shardingItemParameters.trim().isEmpty() || shardingItemParameters.split(",").length < shardingTotalCount) {
				throw new SaturnJobConsoleException(createExceptionMessage(sheetNumber, rowNumber, 10, "分片参数不能小于分片总数。"));
			}
		}
		jobConfig.setShardingItemParameters(shardingItemParameters);

		jobConfig.setQueueName(getContents(rowCells, 10));
		jobConfig.setChannelName(getContents(rowCells, 11));
		jobConfig.setPreferList(getContents(rowCells, 12));
		jobConfig.setUseDispreferList(!Boolean.valueOf(getContents(rowCells, 13)));

		int processCountIntervalSeconds = 300;
		try {
			String tmp = getContents(rowCells, 14);
			if (tmp != null && !tmp.trim().isEmpty()) {
				processCountIntervalSeconds = Integer.parseInt(tmp.trim());
			}
		} catch (NumberFormatException e) {
			throw new SaturnJobConsoleException(createExceptionMessage(sheetNumber, rowNumber, 15, "统计处理数据量的间隔秒数有误，" + e.toString()));
		}
		jobConfig.setProcessCountIntervalSeconds(processCountIntervalSeconds);

		int loadLevel = 1;
		try {
			String tmp = getContents(rowCells, 15);
			if (tmp != null && !tmp.trim().isEmpty()) {
				loadLevel = Integer.parseInt(tmp.trim());
			}
		} catch (NumberFormatException e) {
			throw new SaturnJobConsoleException(createExceptionMessage(sheetNumber, rowNumber, 15, "负荷有误，" + e.toString()));
		}
		jobConfig.setLoadLevel(loadLevel);

		jobConfig.setShowNormalLog(Boolean.valueOf(getContents(rowCells, 16)));

		jobConfig.setPausePeriodDate(getContents(rowCells, 17));

		jobConfig.setPausePeriodTime(getContents(rowCells, 18));

		jobConfig.setUseSerial(Boolean.valueOf(getContents(rowCells, 19)));

		return jobConfig;
	}

	private String getContents(Cell[] rowCell, int column) {
		if (rowCell.length > column) {
			return rowCell[column].getContents();
		}
		return null;
	}

	@RequestMapping(value = "exportJob")
	public void exportJob(HttpServletRequest request, HttpServletResponse response) throws IOException, WriteException {
		File exportJobFile = null;
		try {
			exportJobFile = executorService.getExportJobFile();

			String currentTime = sdf.format(new Date());
			String fileName = getNamespace() + "_allJobs_" + currentTime + ".xls";

			response.setContentType("application/octet-stream");
			response.setHeader("Content-disposition", "attachment; filename=" + new String(fileName.getBytes("UTF-8"), "ISO8859-1"));

			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(exportJobFile));
			BufferedOutputStream bos = new BufferedOutputStream(response.getOutputStream());
			byte[] buff = new byte[2048];
			int bytesRead;
			while (-1 != (bytesRead = bis.read(buff, 0, buff.length))) {
				bos.write(buff, 0, bytesRead);
			}
			bis.close();
			bos.close();
		} catch (SaturnJobConsoleException e) {
			printErrorToResponse("导出全域作业出错：" + e.toString(), response);
			return;
		} finally {
			if(exportJobFile != null) {
				exportJobFile.delete();
			}
		}
	}

	private void printErrorToResponse(String errorMsg, HttpServletResponse response) throws IOException {
		response.setContentType("text/html; charset=utf-8");
		StringBuilder msg = new StringBuilder()
				.append("<script language='javascript'>")
				.append("alert(\"")
				.append(errorMsg.replaceAll("\"", "\\\""))
				.append("\");")
				.append("history.back();")
				.append("</script>");
		response.getOutputStream().print(new String(msg.toString().getBytes("UTF-8"), "ISO8859-1"));
	}

}

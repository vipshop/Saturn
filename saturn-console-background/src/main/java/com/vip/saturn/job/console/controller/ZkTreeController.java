package com.vip.saturn.job.console.controller;

import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.ZkTree;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.ZkTreeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

/**
 * @author hebelala
 */
@Controller
@RequestMapping("/zkTree")
public class ZkTreeController extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZkTreeController.class);

    @Resource
    private ZkTreeService zkTreeService;

    @RequestMapping(value = "downLoadZkTreeByNamespace")
    public void getZkTreeByNamespaceOfSession(HttpServletResponse response, String namespace) throws IOException {
        File file = null;
        try {
            checkMissingParameter("namespace", namespace);
            ZkTree zkTree = zkTreeService.getZkTreeByNamespace(namespace);
            file = zkTreeService.convertZkTreeToFile(zkTree);

            String currentTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String fileName = namespace + "_zk_tree_" + currentTime + ".json";

            response.setContentType("application/octet-stream");
            response.setHeader("Content-disposition",
                    "attachment; filename=" + new String(fileName.getBytes("UTF-8"), "ISO8859-1"));

            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            BufferedOutputStream bos = new BufferedOutputStream(response.getOutputStream());
            byte[] buff = new byte[2048];
            int bytesRead;
            while (-1 != (bytesRead = bis.read(buff, 0, buff.length))) {
                bos.write(buff, 0, bytesRead);
            }
            bis.close();
            bos.close();
        } catch (SaturnJobConsoleException e) {
            printErrorToJsAlert("下载zk树出错：" + e.getMessage(), response);
        } catch (Throwable t) {
            printErrorToJsAlert("下载zk树出错：" + t.toString(), response);
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    @RequestMapping(value = "convertFileToZkTree", method = RequestMethod.POST)
    public RequestResult convertFileToZkTree(MultipartHttpServletRequest request) {
        RequestResult requestResult = new RequestResult();
        try {
            Iterator<String> fileNames = request.getFileNames();
            MultipartFile file = null;
            while (fileNames.hasNext()) {
                if (file != null) {
                    requestResult.setSuccess(false);
                    requestResult.setMessage("仅支持单文件导入");
                    return requestResult;
                }
                file = request.getFile(fileNames.next());
            }
            if (file == null) {
                requestResult.setSuccess(false);
                requestResult.setMessage("请选择导入的文件");
                return requestResult;
            }
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.endsWith(".json")) {
                requestResult.setSuccess(false);
                requestResult.setMessage("仅支持.json文件导入");
                return requestResult;
            }
            ZkTree zkTree = zkTreeService.convertInputStreamToZkTree(file.getInputStream());
            requestResult.setObj(zkTree);
            requestResult.setSuccess(true);
        } catch (SaturnJobConsoleException e) {
            requestResult.setSuccess(false);
            requestResult.setMessage(e.getMessage());
        } catch (Throwable t) {
            requestResult.setSuccess(false);
            requestResult.setMessage(t.toString());
        }
        return requestResult;
    }

}

package com.a.eye.skywalking.storage.index;

import com.a.eye.skywalking.storage.index.exception.DataFileIndexSaveFailedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.a.eye.skywalking.storage.config.Config.DataFileIndex.DATA_FILE_INDEX_FILE_NAME;
import static com.a.eye.skywalking.storage.config.Config.DataFileIndex.STORAGE_BASE_PATH;

public class DataFileIndexUpdator {

    private static Logger logger = LogManager.getLogger(DataFileIndexUpdator.class);
    private IndexL1Cache l1Cache;
    private IndexL2Cache l2Cache;

    public DataFileIndexUpdator(IndexL1Cache l1Cache, IndexL2Cache l2Cache) {
        this.l1Cache = l1Cache;
        this.l2Cache = l2Cache;
    }

    public void addRecord(long timestamp) {
        logger.info("Updating index. timestamp:{}", timestamp);
        try {
            updateFile(timestamp);
            updateCache(timestamp);
        } catch (Exception e) {
            logger.error("Failed to add index record", e);
        }
    }

    private void updateCache(long timestamp) {
        l1Cache.update(timestamp);
        l2Cache.update(timestamp);
    }


    private void updateFile(long timestamp) throws DataFileIndexSaveFailedException {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(new File(STORAGE_BASE_PATH, DATA_FILE_INDEX_FILE_NAME)));
            writer.write(String.valueOf(timestamp));
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            throw new DataFileIndexSaveFailedException("Failed to save index[" + timestamp + "]", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    logger.error("Failed to close index file", e);
                }
            }
        }
    }


    void init() {
        List<Long> indexData = new ArrayList<>();
        BufferedReader indexFileReader = null;
        try {
            indexFileReader =
                    new BufferedReader(new FileReader(new File(STORAGE_BASE_PATH, DATA_FILE_INDEX_FILE_NAME)));
            String indexDataStr = null;
            while ((indexDataStr = indexFileReader.readLine()) != null) {
                indexData.add(Long.parseLong(indexDataStr));
            }
        } catch (IOException e) {
            logger.error("Failed to read index data.", e);
        } finally {
            if (indexFileReader != null) {
                try {
                    indexFileReader.close();
                } catch (IOException e) {
                    logger.error("Failed to close index file", e);
                }
            }
        }

        Collections.reverse(indexData);
        l1Cache.initData(indexData);
        l2Cache.initData(indexData);
    }
}

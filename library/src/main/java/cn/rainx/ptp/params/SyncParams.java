package cn.rainx.ptp.params;

/**
 * Created by rainx on 2017/5/27.
 */

public interface SyncParams {
    int SYNC_TRIGGER_MODE_EVENT = 0;
    int SYNC_TRIGGER_MODE_POLL_LIST = 1;

    int SYNC_MODE_SYNC_ALL          = 0;
    int SYNC_MODE_SYNC_NEW_ADDED    = 1;

    int SYNC_RECORD_MODE_REMEMBER   = 0; // will save
    int SYNC_RECORD_MODE_FORGET     = 1;
}

package com.pandcaspian.indicator;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for CommonDefine constants and utilities.
 */
public class CommonDefineTest {

    @Test
    public void testProtocolRevision() {
        assertEquals(2, CommonDefine.ESP_ProtocolRevision_);
    }

    @Test
    public void testMessageIconConstants() {
        assertEquals(1, CommonDefine.ID_QuestionIcon_);
        assertEquals(2, CommonDefine.ID_MemoryIcon_);
        assertEquals(3, CommonDefine.ID_ErrorIcon_);
        assertEquals(4, CommonDefine.ID_WiFiIcon_);
        assertEquals(5, CommonDefine.ID_CautionIcon_);
        assertEquals(6, CommonDefine.ID_DeleteIcon_);
        assertEquals(7, CommonDefine.ID_InformationIcon_);
        assertEquals(8, CommonDefine.ID_WaitIcon_);
    }

    @Test
    public void testNetStatusConstants() {
        assertEquals(0, CommonDefine.NetStatus_UnDefined_);
        assertEquals(1, CommonDefine.NetStatus_OK_);
        assertEquals(2, CommonDefine.NetStatus_Disconnected_);
        assertEquals(3, CommonDefine.NetStatus_Connecting_);
        assertEquals(4, CommonDefine.NetStatus_NotFound_);
        assertEquals(5, CommonDefine.NetStatus_WrongPassword_);
        assertEquals(6, CommonDefine.NetStatus_ConnectFail_);
        assertEquals(7, CommonDefine.NetStatus_Idle_);
    }

    @Test
    public void testResultCodes() {
        assertEquals((byte) 0x0, CommonDefine.result_Success);
        assertEquals((byte) 0x1, CommonDefine.result_Fail);
        assertEquals((byte) 0x2, CommonDefine.result_Reject);
        assertEquals((byte) 0x3, CommonDefine.result_Processing);
        assertEquals((byte) 0x4, CommonDefine.result_Not_Found);
        assertEquals((byte) 0x5, CommonDefine.result_Unauthorised);
        assertEquals((byte) 0x6, CommonDefine.result_NotAcceptable);
        assertEquals((byte) 0x7, CommonDefine.result_Cancelled);
        assertEquals((byte) 0x8, CommonDefine.result_Creation_Error);
    }

    @Test
    public void testPrefixConstants() {
        assertEquals((byte) '<', CommonDefine.ESP_Prefix_GetNum);
        assertEquals((byte) '>', CommonDefine.ESP_Prefix_SetNum);
        assertEquals((byte) '{', CommonDefine.ESP_Prefix_GetStr);
        assertEquals((byte) '}', CommonDefine.ESP_Prefix_SetStr);
        assertEquals((byte) '[', CommonDefine.ESP_Prefix_Request);
        assertEquals((byte) ']', CommonDefine.ESP_Prefix_Order);
    }

    @Test
    public void testAccessLevelConstants() {
        assertEquals(0, CommonDefine.UnknownLevel_);
        assertEquals(20, CommonDefine.OperatorLevel_);
        assertEquals(21, CommonDefine.AdministerLevel_);
        assertEquals(22, CommonDefine.RemoteUserLevel_);
    }

    @Test
    public void testIndexConstants() {
        assertEquals(0, CommonDefine.BootingIndex_);
        assertEquals(10, CommonDefine.MenuIndex_);
        assertEquals(11, CommonDefine.FormIndex_);
        assertEquals(12, CommonDefine.ListIndex_);
        assertEquals(13, CommonDefine.PageIndex_);
    }

    @Test
    public void testBulkPacketSize() {
        assertEquals(512, CommonDefine.FBPS_);
    }
}

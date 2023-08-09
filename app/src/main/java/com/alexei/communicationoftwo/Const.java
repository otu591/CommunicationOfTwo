package com.alexei.communicationoftwo;

import java.text.SimpleDateFormat;

public class Const {
    public static final SimpleDateFormat formatDate = (SimpleDateFormat) SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM);
    public static final SimpleDateFormat formatTime = (SimpleDateFormat) SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM);
    public static final SimpleDateFormat formatTimeDate = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.MEDIUM);

    public static final String PREF_PATH_IMG_ACCOUNT = "path_img_account";
    public static final String PREF_NAME_ACCOUNT = "name_account";
    public static final String CURRENT_PAGE = "current_page";
    public static final String PART_NAME_AVATAR ="contact_" ;

    public static final int PORT_CONNECT = 6767;

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "chat_db";
    public static final String PATH_STORAGE = "storage";
    public static final String PATH_AVATAR = "avatar";
    public static final String NAME_SYSTEM ="system" ;
    public static final String CONNECTION_ADD = "+";



    public static final byte NO_CONNECT_CONTACT_STATUS = 2;
    public static final byte CONNECT_CONTACT_STATUS = 3;
    public static final byte GET_AVATAR_TO_CONTACT_REQUEST = 26;
    public static final byte BLOCKED_IP_CONTACT_STATUS = 27;
    public static final byte CONNECTING_CONTACT_STATUS = 14;
    public static final byte NOT_RUNNING_CONTACT_STATUS =0 ;

    public static final byte TYPE_CONNECT_STREAM = 4;
    public static final byte TYPE_COMMUNICATION_NODE = 5;
    public static final byte ADD_CONTACT = 6;
    public static final byte EDIT_CONTACT = 7;
    public static final byte CHECK_SETTINGS_CODE = 8;

    public static final byte STORAGE_REQUEST_PERMISSION = 9;
    public static final byte VOICE_REQUEST_CODE = 10;
    public static final byte RESULT_OK = 11;
    public static final byte SELECT_DOT_REQUEST_CODE = 12;
    public static final byte GET_AVATAR_TO_ACCOUNT_REQUEST = 13;
    public static final byte ATTACH_FILES_REQUEST = 15;
    public static final byte AUDIO_RECORD_REQUEST_CODE = 17;
    public static final byte RESOLVABLE_RESULT_OK = -1;
    public static final byte NOT_ID_PACKET = -1;

    public static final byte RESOLVABLE_RESULT_NO = 0;
    public static final byte MESSAGE_TYPE_DIRECTION_OUT = 18;
    public static final byte MESSAGE_TYPE_DIRECTION_IN = 19;

    public static final byte INCOMING_PHONE_CALL = 22;
    public static final byte OUTGOING_PHONE_CALL = 23;
    public static final byte VOICE_PHONE = 24;
    public static final byte MISSING_PHONE = 25;

    public static final byte INTENT_RECORDING_VOICE =28 ;
    public static final byte NO_ACCEPTED_MESSAGE_STATUS = 0;
    public static final byte ACCEPT_MESSAGE_STATUS = 29;
    public static final byte LOCATION_REQUEST_PERMISSION = 30;
    public static final byte PAGE_CHAT = 0;
    public static final byte PAGE_CONNECTIONS = 1;
    public static final byte ACTION_VIDEO_REQUEST = 31;
    public static final byte CAMERA_CODE_PERMISSION = 32;
    public static final byte OUT_MEMORY_MESSAGE_STATUS = 33;
    public static final byte FOR_SHOW_CHAT = 34;
    public static final byte FOR_DELETE_CHAT = 35;
    public static final byte ARRIVED_MESSAGE = 36;
    public static final byte ARRIVED_CALL_PHONE = 37;
    public static final byte DROP_IP_CONTACT_STATUS = 38;
    public static final int REMOVE = 39 ;
    public static final int ADD = 40 ;
    public static final int SEND_ACTION_TO_MAPS = 41;

    public static final int INTERVAL_SLEEP_FOR_RESTART = 3000;
    public static final int INTERVAL_FOR_PING = 5000;
    public static final int PAUSE_RESTART_CONNECT = 2000;

    public static final int TIMEOUT_SOCKET_CONNECT = 20000;







    //    -----packet
    public static final byte ID_MESSAGE = 2;
    public static final byte ID_AUTHENTICATION = 3;
    public static final byte ID_MESSAGE_AND_ATTACHED_FILE = 4;

    public static final byte ID_REQUEST_PING = 5;
    public static final byte ID_RESPONSE_PING = 6;
    public static final byte ID_RESPONSE_MESSAGE_ACCEPT = 7;
    public static final byte ID_OUTGOING_VOICE_COMMUNICATION_REQUEST = 8;
    public static final byte ID_VOICE_COMMUNICATION_REQUEST = 9;
    public static final byte ID_MISSING_VOICE_COMMUNICATION_REQUEST = 11;
    public static final byte ID_CANCEL_CALL_VOICE_COMMUNICATION_REQUEST = 12;
    public static final byte ID_BLOCKED_IP = 13;
    public static final byte ID_MESSAGE_SYS = 14;
    public static final byte ID_RESPONSE_MESSAGE_OUT_MEMORY = 15;
    public static final byte ID_MESSAGE_RESENDING = 16;
    public static final byte ID_SUCCESS_AUTHENTICATION = 17;
    public static final byte ID_REGISTRATION_KEY =18 ;
    public static final byte ID_DROP_CONNECTION = 19;
    public static final byte ID_ACCEPT_RESEND_PACKET = 20;

    public static final byte ID_DROP_VOICE_COMMUNICATION_REQUEST =21 ;
}

package com.alexei.communicationoftwo.database.repository;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.alexei.communicationoftwo.database.model.HistoryMessagePacket;
import com.alexei.communicationoftwo.model.UserChat;

import java.util.List;

@Dao
public interface HistoryMessageDAO {
    @Insert
    long add(HistoryMessagePacket hMP);

    @Query("UPDATE history_messages SET statusAccept =:status WHERE created =:created AND keyUnique=:keyObj  AND idContact=:idContact ")
    int updateAcceptStatus(long created, String keyObj, long idContact, int status);

    @Query("DELETE FROM history_messages WHERE created=:created AND keyUnique=:keyUnique AND idContact=:idContact")
    int delete(long created, String keyUnique, long idContact);

    @Query("SELECT * FROM history_messages WHERE idContact=:idContact ")
    List<HistoryMessagePacket> getHMPackets(long idContact);

    @Query("DELETE FROM history_messages")
    void clearTable();

    @Query("SELECT COUNT(*) FROM history_messages")
    long getSizeTable();

    @Query("SELECT recipientIp,nameContact,idContact  FROM history_messages  GROUP BY idContact ORDER BY id DESC")
    List<UserChat> getAllChatUsers();

    @Query("DELETE FROM history_messages WHERE idContact=:idContact")
    int deleteAll(long idContact);

    @Query("SELECT * FROM history_messages WHERE idContact=:idContact AND statusAccept=:status AND directionTypeMessage =:typeDirection ")
    List<HistoryMessagePacket> getHMPacketNoAccepted(long idContact, int status,int typeDirection);

    @Query("SELECT id FROM history_messages WHERE created=:create AND keyUnique=:keyObj ")
    long getId(long create, String keyObj);

}

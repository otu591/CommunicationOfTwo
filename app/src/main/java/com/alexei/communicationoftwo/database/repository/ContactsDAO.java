package com.alexei.communicationoftwo.database.repository;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.alexei.communicationoftwo.database.model.DataContact;
import com.alexei.communicationoftwo.database.model.HistoryMessagePacket;
import com.alexei.communicationoftwo.model.UserChat;

import java.util.List;

@Dao
public interface ContactsDAO {


    @Insert
    long add(DataContact contact);

    @Delete
    int delete(DataContact contact);

    @Query("SELECT * FROM contacts ")
    List<DataContact> getContacts();

    @Query("SELECT COUNT(*) FROM contacts")
    long getSize();

    @Update
    int update(DataContact contact);

    @Query("SELECT * FROM contacts WHERE id=:id ")
    DataContact getContact(long id);

    @Query("SELECT * FROM contacts WHERE id=:id ")
    int getIdContact(int id);

}

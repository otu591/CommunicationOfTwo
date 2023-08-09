package com.alexei.communicationoftwo.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.alexei.communicationoftwo.Const;
import com.alexei.communicationoftwo.database.model.DataContact;
import com.alexei.communicationoftwo.database.repository.ContactsDAO;
import com.alexei.communicationoftwo.database.repository.HistoryMessageDAO;
import com.alexei.communicationoftwo.database.model.HistoryMessagePacket;

@Database(entities = { HistoryMessagePacket.class, DataContact.class},
        version = Const.DATABASE_VERSION)

public abstract class AppDB extends RoomDatabase {

    public abstract HistoryMessageDAO getMessageDAO();
    public abstract ContactsDAO getContactsDAO();

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ssamant.utilities;

import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Sunil
 */
public class CheckBoxWrapperTableModel extends AbstractTableModel {
     private ArrayList<Boolean> checkBoxes = new ArrayList<>();

    private DefaultTableModel model;
    private String columnName;

    public CheckBoxWrapperTableModel(DefaultTableModel model, String columnName)
    {
        this.model = model;
        this.columnName = columnName;

        for (int i = 0; i < model.getRowCount(); i++)
            checkBoxes.add( Boolean.FALSE );
    }

    @Override
    public String getColumnName(int column)
    {
        return (column > 0) ? model.getColumnName(column - 1) : columnName;
    }

    @Override
    public int getRowCount()
    {
        return model.getRowCount();
    }

    @Override
    public int getColumnCount()
    {
        return model.getColumnCount() + 1;
    }

    @Override
    public Object getValueAt(int row, int column)
    {
        if (column > 0)
            return model.getValueAt(row, column - 1);
        else
        {
            Object value = checkBoxes.get(row);
            return (value == null) ? Boolean.FALSE : value;
        }
    }

    @Override
    public boolean isCellEditable(int row, int column)
    {
        if (column > 0)
            return model.isCellEditable(row, column - 1);
        else
            return true;
    }

    @Override
    public void setValueAt(Object value, int row, int column)
    {
        if (column > 0)
            model.setValueAt(value, row, column - 1);
        else
        {
            checkBoxes.set(row, (Boolean)value);
        }

        fireTableCellUpdated(row, column);
    }
    
     public void setRowCount(int rowCount)
     {
        model.setRowCount(0);
     }

    @Override
    public Class getColumnClass(int column)
    {
        return (column > 0) ? model.getColumnClass(column - 1) : Boolean.class;
    }
    


    public void removeRow(int row)
    {
        checkBoxes.remove(row);
        fireTableRowsDeleted(row, row);
        model.removeRow(row);
    }
}
